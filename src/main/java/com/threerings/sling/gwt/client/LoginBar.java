//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;

/**
 * A toolbar for logging in.
 */
public class LoginBar extends SmartTable
{
    /**
     * Creates a new login toolbar. If the username is non-null, the username text box is preset
     * with that value.
     */
    public LoginBar (SlingApp<?> app, String username)
    {
        _app = app;
        setStyleName("uLoginBar");

        if (username != null) {
            _username.setText(username);
        }

        ClickHandler go = new ClickHandler() {
            public void onClick (ClickEvent event)
            {
                login();
            }
        };

        EnterClickAdapter.bind(_password, go);

        cell(0, 0).text(_msgs.username());
        cell(0, 1).widget(_username);
        cell(0, 2).text(_msgs.passwordLabel());
        cell(0, 3).widget(_password);
        cell(0, 4).widget(new Button(_msgs.login(), go));
    }

    protected void login ()
    {
        _app.login(_username.getText(), _password.getText());
    }

    protected SlingApp<?> _app;

    protected TextBox _username = new TextBox();
    protected PasswordTextBox _password = new PasswordTextBox();

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
