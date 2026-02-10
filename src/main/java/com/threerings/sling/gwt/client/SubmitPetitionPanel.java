//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.sling.gwt.client.SlingNav.Requests;
import com.threerings.sling.gwt.ui.NamedRowSmartTable;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.UserPetition;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * Panel for submitting a new petition.
 */
public class SubmitPetitionPanel extends NamedRowSmartTable
{
    /**
     * Creates a new panel with a form that will submit a new petition to the server.
     */
    public SubmitPetitionPanel (SlingContext ctx, String recipientName, boolean useEmail)
    {
        super("uSubmitPetition", 0, 0);
        _ctx = ctx;

        if (recipientName == null) {
            recipientName = _msgs.petitionDefaultRecipient();
        }

        int row = 0;
        cell(row, 1).styles("Instruction").text(useEmail ?
            _msgs.petitionInstructionsEmail(recipientName) :
            _msgs.petitionInstructionsInGame(recipientName));
        row++;

        final TextBox email = Widgets.newTextBox(_ctx.ainfo.email, 0, Math.max(
            _ctx.ainfo.email.length(), 30));
        Button updateEmailBtn = null;
        if (useEmail) {
            updateEmailBtn = new Button(_msgs.update());
            cell(row, 0).text(_msgs.accountEmail()).styles("Label");
            cell(row, 1).widget(Widgets.newRow(email, updateEmailBtn));
            row++;
        }

        final ListBox gameNames = new ListBox();
        gameNames.addItem(_msgs.selectCharacter());
        for (String gameName : _ctx.ainfo.name.gameNames) {
            gameNames.addItem(gameName);
        }
        cell(row, 0).text(_msgs.characterName()).styles("Label");
        cell(row, 1).widget(gameNames);
        row++;

        final TextBox subject = Widgets.newTextBox("", 200, 60);
        cell(row, 0).text(_msgs.subjectLabel()).styles("Label");
        cell(row, 1).widget(subject);
        row++;

        final TextArea message = Widgets.newTextArea("", 60, 4);
        cell(row, 0).text(_msgs.messageLabel()).styles("Label", "MessageLabel");
        cell(row, 1).widget(message);
        row++;

        cell(row, 1).styles("Instruction").text(_msgs.petitionInstructions2());
        row++;

        Button submitBtn = new Button(_msgs.submitPetition());
        cell(row, 1).widget(submitBtn);
        nameRow("submit", row++);

        if (updateEmailBtn != null) {
            new ClickCallback<Void>(updateEmailBtn, email) {
                @Override protected boolean callService () {
                    _ctx.svc.updateEmail(email.getText(), this);
                    return true;
                }

                @Override protected boolean gotResult (Void result) {
                    return true;
                }

                @Override protected String formatError (Throwable cause) {
                    return translateServerError(cause);
                }
            };
        }

        new ClickCallback<Integer>(submitBtn) {
            @Override protected boolean callService () {
                final UserPetition petition = new UserPetition();
                petition.status = Event.Status.OPEN;
                petition.subject = subject.getText().trim();
                if (petition.subject.length() == 0) {
                    Popups.error(_msgs.enterPetitionSubject());
                    return false;
                }

                final String msg = message.getText().trim();
                if (msg.length() == 0) {
                    Popups.error(_msgs.enterPetitionMessage());
                    return false;
                }

                int gameNameIdx = gameNames.getSelectedIndex();
                boolean hasGameNames = !_ctx.ainfo.name.gameNames.isEmpty();
                if (gameNameIdx == 0 && hasGameNames) {
                    Popups.error(_msgs.errMustSelectCharacter());
                    return false;
                }
                String gameName = hasGameNames ? gameNames.getItemText(gameNameIdx) : null;
                _ctx.svc.registerPetition(petition, gameName, msg, this);
                return true;
            }

            @Override protected boolean gotResult (Integer result) {
                cell("submit", 1).widget(
                    Widgets.newRow(Widgets.newLabel(_msgs.petitionSubmitted()), SlingUtils.makeLink(
                        _ctx, _msgs.returnToPetitions(), Requests.viewMine())));
                return false;
            }

            @Override protected String formatError (Throwable cause) {
                return translateServerError(cause);
            }
        };
    }

    protected SlingContext _ctx;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
