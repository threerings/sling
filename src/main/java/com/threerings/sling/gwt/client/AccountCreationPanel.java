package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;

import com.threerings.sling.gwt.client.SlingUtils;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.web.data.Account;

public class AccountCreationPanel extends FlowPanel
{
    public AccountCreationPanel (SlingContext ctx)
    {
        setStyleName("uAccountCreationPanel");

        _ctx = ctx;

        final TextBox name = Widgets.newTextBox("", 20, 20);
        final TextBox password = Widgets.newTextBox("", 35, 35);
        final TextBox email = Widgets.newTextBox("", 50, 50);

        final Button create = new Button(_msgs.accountCreateButton());
        new ClickCallback<Account>(create) {
            @Override
            public boolean callService ()
            {
                if (password.getText().length() < 4) {
                    Popups.errorBelow(_msgs.passwordTooShort(), create);
                    return false;
                }

                _ctx.svc.createSupportAccount(name.getText(),
                    _ctx.frame.md5hex(password.getText()), email.getText(), this);
                return true;
            }

            @Override protected String formatError (Throwable cause) {
                return SlingUtils.translateServerError(cause);
            }

            @Override
            public boolean gotResult (Account result)
            {
                _ctx.navigate(SlingNav.Accounts.findAccount(result.name.accountName));
                return true;
            }
        };

        SmartTable createPanel = new SmartTable();
        createPanel.cell(0, 0)
            .next()
            .text(_msgs.accountCreateUsername()).nextCol().widget(name).next()
            .text(_msgs.accountCreatePassword()).nextCol().widget(password).next()
            .text(_msgs.accountCreatedEmail()).nextCol().widget(email).next()
            .widget(create);

        add(new ParaPanel(_msgs.accountCreateTitle(), "Title"));
        add(createPanel);
    }

    protected SlingContext _ctx;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
