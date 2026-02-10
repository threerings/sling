//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;

/**
 * Very basic toolbar for a logged-in non-admin user. Most sling library client will want to use
 * their own sexier toolbars.
 */
public class UserBar extends SmartTable
{
    public UserBar (SlingApp<?> app)
    {
        _app = app;
        setStyleName("uUserBar");

        Button logout = new Button(_msgs.logout());
        logout.addClickHandler(new ClickHandler() {
            @Override
            public void onClick (ClickEvent event)
            {
                _app.logout();
            }
        });

        cell(0, 2).styles("TopText").text(_msgs.loggedInAs());
        cell(1, 2).styles("LoginStatus").widget(Widgets.newRow(
            Widgets.newLabel(_app.getContext().ainfo.name.accountName),
            logout));
    }

    protected SlingApp<?> _app;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
