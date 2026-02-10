//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.PageAddress;
import com.threerings.sling.web.client.SlingService;
import com.threerings.sling.web.client.SlingServiceAsync;

/**
 * The common entry point for sling clients.
 */
public abstract class SlingEntryPoint
    implements EntryPoint
{
    @Override // from interface EntryPoint
    public void onModuleLoad ()
    {
        // create our web context
        _ctx = createContext();
        _ctx.app = createApp();
        _ctx.svc = (SlingServiceAsync)initService(GWT.create(SlingService.class));
    }

    /**
     * Sets the service entry point of a newly created asynchronous service to our
     * {@link #getServiceEntryPoint()}.
     */
    protected Object initService (Object svc)
    {
        ((ServiceDefTarget)svc).setServiceEntryPoint(getServiceEntryPoint());
        return svc;
    }

    /**
     * Causes the entry point to navigate to all tokens by listening for history token changes.
     * Also issues an initial navigate to the current token. Most apps will want to do this.
     * Only those that perform their own top-level history management must avoid it.
     */
    protected void manageNavigation ()
    {
        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override public void onValueChange (ValueChangeEvent<String> event) {
                _ctx.navigate(PageAddress.fromToken(event.getValue()));
            }
        });
        _ctx.navigate(PageAddress.fromToken(History.getToken()));
    }

    /**
     * Creates the web context. Subclasses may want to override this to provide more service
     * members etc.
     */
    protected SlingContext createContext ()
    {
        return new SlingContext();
    }

    /**
     * Creates the sling application. By default, uses a vanilla {@code SlingApp} instance with the
     * most obvious toolbars.
     */
    protected SlingApp<?> createApp ()
    {
        return new SlingApp<SlingContext>(_ctx) {
            @Override protected Widget createToolbar (AuthLevel authLevel, String username) {
                switch (authLevel) {
                case NONE:
                    return new LoginBar(this, username);
                case USER:
                    return new UserBar(this);
                case ADMIN:
                    return new AdminBar(this);
                }
                return null;
            }
        };
    }

    protected String getServiceEntryPoint ()
    {
        String prefix = GWT.isScript() ? getServletPath() : GWT.getModuleBaseURL();
        return prefix + "data";
    }

    /**
     * Returns the path at which the SlingServlet is mounted. For example:
     * <code>/sling/</code>.
     */
    protected abstract String getServletPath ();

    protected SlingContext _ctx;
}
