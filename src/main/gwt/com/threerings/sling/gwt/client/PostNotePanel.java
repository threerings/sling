//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * A panel for posting a support note on an account.
 */
public class PostNotePanel extends SmartTable
{
    /**
     * Creates a new panel to post a support note on the account with the given name.
     */
    public PostNotePanel (SlingContext ctx, String accountName)
    {
        super("uPostNotePanel", 0, 5);

        _ctx = ctx;
        _accountName = accountName;

        final TextBox subject = Widgets.newTextBox("", 255, 32);
        final TextArea text = Widgets.newTextArea("", 50, 8);
        final Button postBtn = new Button(_msgs.postNoteBtn());

        cell(0, 0).text(_msgs.postNoteToAccount(accountName)).colSpan(2).styles("Title");
        cell(1, 0).text(_msgs.subjectLabel()).styles("Label");
        cell(1, 1).widget(subject);
        cell(2, 0).text(_msgs.text()).styles("Label", "Text");
        cell(2, 1).widget(text);
        cell(3, 1).widget(postBtn);

        ChangeHandler onChange = new ChangeHandler() {
            @Override
            public void onChange (ChangeEvent event)
            {
                postBtn.setEnabled(subject.getText().length() > 0 && text.getText().length() > 0);
            }
        };
        subject.addChangeHandler(onChange);
        text.addChangeHandler(onChange);
        postBtn.setEnabled(false);

        new ClickCallback<Void>(postBtn) {
            @Override
            protected boolean callService ()
            {
                _ctx.svc.addNote(_accountName, subject.getText(),
                    text.getText(), this);
                return true;
            }

            @Override
            protected boolean gotResult (Void result)
            {
                cell(3, 1).widget(Widgets.newRow("ResultRow",
                    Widgets.newLabel(_msgs.notePosted()),
                    SlingUtils.linkToAccount(
                        _ctx, _msgs.viewAccount() + _accountName, _accountName)));
                return false;
            }

            @Override
            protected String formatError (Throwable cause)
            {
                return translateServerError(cause);
            }
        };
    }

    protected SlingContext _ctx;
    protected String _accountName;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
