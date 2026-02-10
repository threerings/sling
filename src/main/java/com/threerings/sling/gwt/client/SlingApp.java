//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable.CellMutator;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.AuthorizationException;
import com.threerings.sling.gwt.util.PageAddress;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;

/**
 * A sling application. The sling application is a container with a toolbar on top and the content
 * below. It preforms a number of related functions:<ul>
 * <li>Determines which toolbar should be shown based on the context's current authorization
 * level.</li>
 * <li>Contains a registry of {@link Section} implementations.</li>
 * <li>Handles navigation requests by resolving the section and then the page within the
 * section.</li>
 * <li>Handles logging in and logging out, updating the toolbar and content when appropriate</li>.
 * <li>Handles authorization failures when resolving section pages.</li>.
 * <li>Handles navigation failures when resolving section pages.</li>.
 * </ul>
 * @param <Ctx> the type of context used by this application
 */
public abstract class SlingApp<Ctx extends SlingContext>
{
    /**
     * Creates a new sling app that will use the given context. The context is used for
     * authentication and for navigation.
     */
    public SlingApp (Ctx context)
    {
        _ctx = context;
        _mainPanel.setStyleName("uMain");
        bar().styles("ToolBar").alignCenter().widget(empty());
        content().styles("Content").widget(empty());
    }

    /**
     * Attempts to navigate to the given address. The call may be asynchronous. It may also be
     * ignored if a login or logout is currently in progress. Once the page has successfully been
     * displayed (or an authorization error handled), the
     * {@link SlingContext.Frame#navigatedTo(PageAddress)} is called.
     */
    public void openPage (PageAddress address)
    {
        if (_async) {
            return;
        }

        // first try and resolve the page
        _current = new ResolvedPage(address);

        if (_current.authError) {
            // if there was an error creating the view, try and get a higher auth level
            initSession();
        } else {
            // otherwise, carry on
            resetWidgets();
        }

        _ctx.frame.navigatedTo(address);
    }

    /**
     * Registers a new section, replacing any existing section with the same {@link SectionId}.
     * Future requests to open page addresses with a {@link PageAddress#sectionId} matching the
     * given section's {@link Section#getId()} will use the given section to generate the view.
     */
    public void registerSection (Section<Ctx> section)
    {
        _sections.put(section.getId(), section);
    }

    /**
     * Gets the main panel of the application. The entry point should add the main panel to
     * one of its root elements.
     * @see com.google.gwt.user.client.ui.RootPanel
     */
    public Widget getMainPanel ()
    {
        return _mainPanel;
    }

    /**
     * Gets the context for the app and all of its pages and sections.
     */
    public Ctx getContext ()
    {
        return _ctx;
    }

    /**
     * Attempts to login to the server provided by the app's context. On success, a new toolbar
     * will be shown and if any page is pending for which an {@code USER} or higher auth level was
     * required but not met, that page will be shown again. On failure the toolbar is refreshed
     * and displayed alongside an appropriate error message. During login, all requests to login
     * again, logout or display a page are ignored.
     */
    public void login (final String username, String password)
    {
        if (_async) {
            return;
        }

        bar().widget(Widgets.newLabel(_msgs.loggingIn()));
        content().widget(empty());

        _ctx.login(username, password, wrap(new AsyncCallback<Void>() {
            @Override public void onSuccess (Void result) {
                // login successful, show the default toolbar and resolved page
                resetWidgets();
            }

            @Override public void onFailure (Throwable caught) {
                // login failed, stick an error message next to the default toolbar and try again
                bar().widget(Widgets.newRow(Widgets.newLabel(_msgs.loginFailed(), "uError"),
                    createToolbar(_ctx.getCurrentAuthLevel(), username)));
            }
        }));
    }

    /**
     * Attempts to log out. Upon success, refreshes the toolbar using the new auth level. Upon
     * failure (should be very rare), displays an error message in place of the toolbar. During
     * the logout, any request to logout again, login or display a page will be ignored.
     */
    public void logout ()
    {
        if (_async) {
            return;
        }

        bar().widget(Widgets.newLabel(_msgs.loggingOut()));

        // TODO: is this wise? I think it must be attempting to keep the current page from hanging
        //     around looking buggy during the logout process. But how can we be sure the right
        //     page to go to is ""? Perhaps the application needs to pass in a token to go to on
        //     logout
        openPage(PageAddress.fromToken("")); // Hmm

        _ctx.logout(wrap(new AsyncCallback<Void>() {
            @Override public void onSuccess (Void result) {
                // logout normal, show the default toolbar and content
                resetWidgets();
            }

            @Override public void onFailure (Throwable caught) {
                if (caught == null) {
                    // we're about to get redirected, just show a message in case the user sees it
                    bar().widget(Widgets.newLabel(_msgs.redirecting()));
                } else {
                    // Could not log out!? oh well
                    bar().widget(Widgets.newLabel(_msgs.logoutFailed(), "uError"));
                }
            }
        }));
    }

    /**
     * Creates a toolbar appropriate for a user at the given auth level. Typically subclasses
     * should provide their own toolbars for {@code NONE} and {@code USER| levels, but create a
     * sling {@link AdminBar} for {@code ADMIN} level, though this is not required. Toolbars are
     * intended to be laid out horizontally across the top.
     */
    protected abstract Widget createToolbar (AuthLevel authLevel, String username);

    protected void initSession ()
    {
        // clear out our content while we're checking the session
        bar().widget(Widgets.newLabel(_msgs.loading()));
        content().widget(empty());

        _ctx.validateSession(wrap(new AsyncCallback<Void>() {
            @Override public void onFailure (Throwable caught) {
                if (caught != null) {
                    // session failed to validate, show the tool bar and the "access denied"
                    // content
                    resetWidgets();
                } else {
                    // we are being redirected by the context, so don't really need to show
                    // anything... but just in case, just say "redirecting"
                    bar().widget(Widgets.newLabel(_msgs.redirecting()));
                }
            }

            @Override public void onSuccess (Void result) {
                // yay, the session is good. show the default toolbar and resolved content
                resetWidgets();
            }
        }));
    }

    protected void resetWidgets ()
    {
        bar().widget(createToolbar(_ctx.getCurrentAuthLevel(), null));
        content().widget(_current.getContent());
    }

    protected CellMutator bar ()
    {
        return _mainPanel.cell(0, 0);
    }

    protected CellMutator content ()
    {
        return _mainPanel.cell(1, 0);
    }

    protected static Widget empty ()
    {
        return Widgets.newLabel("");
    }

    protected <T> AsyncCallback<T> wrap (final AsyncCallback<T> target)
    {
        _async = true;
        return new AsyncCallback<T>() {
            @Override
            public void onFailure (Throwable caught)
            {
                _async = false;
                target.onFailure(caught);
            }

            @Override
            public void onSuccess (T result)
            {
                _async = false;
                target.onSuccess(result);
            }
        };
    }

    protected class ResolvedPage
    {
        public PageAddress address;
        public boolean linkError;
        public boolean authError;

        public ResolvedPage (PageAddress address)
        {
            this.address = address;
            getContent();
        }

        public Widget getContent ()
        {
            if (_content == null || _authLevel != _ctx.getCurrentAuthLevel()) {
                linkError = authError = false;
                _authLevel = _ctx.getCurrentAuthLevel();
                _content = resolveContent();
                //SlingUtils.log(this.toString());
            }
            return _content;
        }

        @Override public String toString ()
        {
            return "ResolvedPage [address=" + address + ", auth=" + _authLevel +
                ", content=" + _content + (linkError ? ", with linkError " : "") +
                (authError ? ", with authError " : "") + "]";
        }

        protected Widget resolveContent ()
        {
            Section<Ctx> section = _sections.get(address.sectionId);
            if (section == null) {
                Console.log("Section not found", "id", address.sectionId);
                linkError = true;
                return Widgets.newLabel(_msgs.brokenLink());
            }

            try {
                Widget view = section.createView(_ctx, address.arguments, _content);

                if (view == null) {
                    Console.log("Page view not created", "address", address,
                        "sectionId", address.sectionId);
                    linkError = true;
                    return Widgets.newLabel(_msgs.pageNotFound());
                }
                return view;

            } catch (AuthorizationException ex) {

                authError = true;

                if (ex.requiredLevel == AuthLevel.ADMIN) {
                    return Widgets.newLabel(_msgs.youDontHavePermission());
                }

                return Widgets.newLabel(_msgs.youMustBeLoggedIn());

            } catch (Exception ex) {

                Console.log("Page view creation failed", "address", address,
                    "sectionId", address.sectionId, ex);
                linkError = true;
                return Widgets.newLabel(_msgs.brokenLinkOrSomething());
            }
        }

        protected AuthLevel _authLevel;
        protected Widget _content;
    }

    protected Map<SectionId, Section<Ctx>> _sections = Maps.newHashMap();
    protected SmartTable _mainPanel = new SmartTable();
    protected ResolvedPage _current;
    protected Ctx _ctx;
    protected boolean _async;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
