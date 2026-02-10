//
// Sling - Copyright 2012 Three Rings Design, Inc.

package client;

import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.sling.gwt.client.FAQSection;
import com.threerings.sling.gwt.client.PetitionSection;
import com.threerings.sling.gwt.client.SlingApp;
import com.threerings.sling.gwt.client.SlingContext;
import com.threerings.sling.gwt.client.SlingEntryPoint;

/**
 * The main entry point for the Sling test user client.
 */
public class user extends SlingEntryPoint
{
    @Override // from SlingEntryPoint
    public void onModuleLoad ()
    {
        super.onModuleLoad();

        RootPanel.get("appcontent").add(_ctx.app.getMainPanel());
        manageNavigation();
    }

    @Override
    protected SlingApp<SlingContext> createApp ()
    {
        @SuppressWarnings("unchecked")
        SlingApp<SlingContext> app = (SlingApp<SlingContext>)super.createApp();
        app.registerSection(new FAQSection<SlingContext>());
        app.registerSection(new PetitionSection<SlingContext>());
        return app;
    }

    @Override // from SlingEntryPoint
    protected String getServletPath ()
    {
        return "/";
    }
}
