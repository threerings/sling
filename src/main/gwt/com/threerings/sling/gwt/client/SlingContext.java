//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.Console;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.BaseContext;
import com.threerings.sling.gwt.util.PageAddress;
import com.threerings.sling.gwt.util.ServerTime;
import com.threerings.sling.web.client.SlingService;
import com.threerings.sling.web.client.SlingServiceAsync;
import com.threerings.sling.web.data.AccountName;

/**
 * Contains a reference to the various bits that we're likely to need in the sling client UI.
 */
public class SlingContext
    implements BaseContext
{
    /**
     * Top-level functions for the web application, in case someone wants to stick us inside a
     * frame. I'm looking at you, Whirled!
     */
    public interface Frame
    {
        /**
         * Returns the hash of the given value.
         * TODO: is this really needed. We had the md5.js file, what is that for?
         */
        public String md5hex (String text);

        /**
         * Creates a token for a page. Sling will call this method any time a link to an app page
         * is needed. This allows the frame implementation to wrap the sling app under a separate
         * token prefix if other token schemes are in use. For example, if sling is attempting to
         * link to "#events", whirled would return "#support-events" from here.
         */
        public String toToken (PageAddress page);

        /**
         * Called to register a history token when the page is loaded. This allows the frame
         * implementation to wrap the sling app under a separate token prefix if other token
         * schemes are in use. For example, if sling navigates to "#events" whirled would want to
         * set the history token to "#support-events".
         */
        public void navigatedTo (PageAddress newPage);
    }

    /** Our authentication info. */
    public SlingService.AuthInfo ainfo;

    /** Used to make requests of the server. */
    public SlingServiceAsync svc;

    /** May be overwritten by application if sling is to be embedded. */
    public Frame frame = makeDefaultFrame();

    /** Our application. */
    public SlingApp<?> app;

    /** Account names starting with the supportPrefix are reserved for support staff. Sling
     * currently only uses this to provide the first part of the account name when assigning
     * events */
    public String supportPrefix;

    /**
     * Returns true if the user is currently authenticated as an admin, false otherwise.
     */
    public boolean isAdmin ()
    {
        return ainfo != null && ainfo.isAdmin;
    }

    @Override // from BaseContext
    public AuthLevel getCurrentAuthLevel ()
    {
        return isAdmin() ? AuthLevel.ADMIN : ainfo != null ? AuthLevel.USER : AuthLevel.NONE;
    }

    /**
     * Attempts to validate the stored session cookie with the server.<p>There are three possible
     * outcomes:</p><ol>
     * <li>The server found the sent cookie. In this case the {@link #ainfo} member is set and the
     * callback's onSuccess invoked.</li>
     * <li>The server could not find the cookie and wants the user to login. In this case the
     * callback's onFailure method will be invoked with the exception sent by the server.</li>
     * <li>The server could not find the cookie and wants the user to redirect to a different
     * page. In this case the redirect is handled and the callback's onFailure method is invoked
     * with null.</li></ol>
     */
    public void validateSession (final AsyncCallback<Void> callback)
    {
        // validate that the session token has not expired
        svc.validateSession(new AsyncCallback<SlingService.AuthInfo>() {
            public void onSuccess (SlingService.AuthInfo result) {

                // back door for development to login to standalone app (it doesn't matter if
                // users figure this out, the redirect is just a convenience)
                if (result.name == null && History.getToken().equals("@@@forcelogin@@@")) {
                    callback.onFailure(new Throwable("e.back_door_open"));
                    return;
                }

                if (result.getRedirectUrl() != null) {
                    // the server has redirected us. send a special failure
                    callback.onFailure(null);
                    Window.Location.assign(result.getRedirectUrl());
                    return;
                }
                ainfo = result;
                ServerTime.setOffset(ainfo.serverInfo.timeZoneOffset);
                callback.onSuccess(null);
            }

            public void onFailure (Throwable cause) {
                ainfo = null;
                callback.onFailure(cause);
            }
        });
    }

    /**
     * Authenticates with the server. If successful, sets the {@link #ainfo} member, sets the
     * session cookie and invokes the callback. Otherwise, just invokes the callback.
     */
    public void login (String username, String password, final AsyncCallback<Void> callback)
    {
        password = frame.md5hex(password);
        svc.login(username, password, new AsyncCallback<SlingService.AuthInfo>() {
            public void onSuccess (SlingService.AuthInfo result) {
                ainfo = result;
                ServerTime.setOffset(ainfo.serverInfo.timeZoneOffset);
                callback.onSuccess(null);
            }

            public void onFailure (Throwable cause) {
                callback.onFailure(cause);
            }
        });
    }

    /**
     * Attempts to log out of the server. <p>There are three possible outcomes:</p><ol>
     * <li>The logout was successful and the server wants the user to return to the standalone
     * sling landing page (login). In this case, the callback's onSuccess method is called.</li>
     * <li>The logout could not proceed. In this case the callback's onFailure method is called with
     * the exception returned from the server.</li>
     * <li>The logout was successful and the server wants the user to be redirected to a different
     * page. In this case, the redirect is automatically issued and the callback's onFailure method
     * is invoked with null.</li>
     * </ol>
     */
    public void logout (final AsyncCallback<Void> callback)
    {
        ainfo = null;
        svc.logout(new AsyncCallback<String>() {
            @Override public void onFailure (Throwable caught) {
                callback.onFailure(caught);
            }

            @Override public void onSuccess (String result) {
                if (result != null) {
                    Window.Location.assign(result);
                    callback.onFailure(null);
                } else {
                    callback.onSuccess(null);
                }
            }
        });
    }

    /**
     * Navigates to the given sling page. This simply delegates to the {@link #app}'s
     * {@link SlingApp#openPage(PageAddress)} method.
     */
    public void navigate (PageAddress address)
    {
        Console.log("Navigating to " + address);
        app.openPage(address);
    }

    /**
     * This is used in admin mode to convert the names of ticket owners and message authors into
     * display names.
     */
    public AccountName toHandle (AccountName name)
    {
        return name;
    }

    /**
     * Creates the frame implementation that works when sling is in the top-level frame.
     */
    protected Frame makeDefaultFrame ()
    {
        return new Frame() {
            @Override public String md5hex (String text) {
                return nmd5hex(text);
            }

            @Override public void navigatedTo (PageAddress address) {
                History.newItem(address.toToken(), false);
            }

            @Override public String toToken (PageAddress page) {
                return page.toToken();
            }
        };
    }

    /**
     * MD5 hashes the supplied text and returns the hex encoded hash value. This only works when
     * called from the top-level frame for some reason.
     */
    protected native static String nmd5hex (String text) /*-{
                    return $wnd.hex_md5(text);
                }-*/;
}
