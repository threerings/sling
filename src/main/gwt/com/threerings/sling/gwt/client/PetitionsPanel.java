//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.gwt.util.InputClickCallback;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.gwt.util.ServerTime;
import com.threerings.sling.web.client.SlingService.PostMessageResult;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.Message;
import com.threerings.sling.web.data.UserPetition;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * A panel for viewing user petitions.
 */
public class PetitionsPanel extends FlowPanel
{
    /**
     * Creates a new petitions panel. The petitions are queried for the current user internally.
     */
    public PetitionsPanel (SlingContext ctx)
    {
        setStyleName("uPetitionsPanel");
        _ctx = ctx;

        Label refresh = Widgets.newInlineLabel(_msgs.petRefresh(), "actionLabel", "RefreshBtn");
        add(Widgets.newFlowPanel("Title",
            Widgets.newInlineLabel(_msgs.petYourRecentPetitions()),
            refresh));

        add(_petitions);
        _petitions.add(new ParaPanel(_msgs.loading()));

        new ClickCallback<List<UserPetition>>(refresh) {
            {
                // load up our petitions
                takeAction(true);
            }

            @Override
            protected boolean callService ()
            {
                _ctx.svc.loadPetitions(this);
                return true;
            }

            @Override
            protected boolean gotResult (List<UserPetition> result)
            {
                setPetitions(result);
                return true;
            }

            @Override
            protected String formatError (Throwable cause)
            {
                return translateServerError(cause);
            }
        };
    }

    protected void setPetitions (List<UserPetition> petitions)
    {
        _petitions.clear();
        if (petitions.isEmpty()) {
            _petitions.add(new ParaPanel(_msgs.petNoOpenPetitionsWereFound()));
        }
        for (UserPetition petition : petitions) {
            _petitions.add(new PetitionPanel(petition));
        }
    }

    protected static Event.Status sanitizeStatus (Event.Status status)
    {
        // avoid annoying the user by telling them we've ignored their idiotic support request
        if (status == Event.Status.IGNORED_CLOSED) {
            return Event.Status.RESOLVED_CLOSED;
        } else {
            return status;
        }
    }

    protected String formatAuthor (AccountName name)
    {
        return name.toString();
    }

    protected class PetitionPanel extends FlowPanel
    {
        public PetitionPanel (final UserPetition petition)
        {
            setStyleName("Petition");

            Button postReplyBtn = new Button(_msgs.petPostNewReply());
            FlowPanel header = Widgets.newFlowPanel("Header",
                Widgets.newLabel(petition.subject, "Subject"),
                Widgets.newFlowPanel(
                    Widgets.newInlineLabel(_msgs.petStatusLabel(), "Label"),
                    Widgets.newInlineLabel(" "),
                    Widgets.newInlineLabel(SlingUtils.translate(petition.status), "Status",
                        petition.status.isOpen() ? "StatusOpen" : "StatusClosed"),
                    petition.status.isOpen() ? postReplyBtn : Widgets.newInlineLabel("")));

            add(header);

            if (petition.messages.size() > 1) {
                add(new ParaPanel(_msgs.petReplies(String.valueOf(petition.messages.size())),
                    "RepliesHeader"));
            }

            add(_replies);

            // add our messages
            for (Message message : petition.messages) {
                _replies.add(makeReply(message));
            }

            new InputClickCallback<PostMessageResult, TextArea>(postReplyBtn,
                Widgets.newTextArea("", 80, 4)) {
                @Override
                protected boolean callService (String input)
                {
                    _ctx.svc.postMessage(petition.eventId,
                        Message.Access.NORMAL, input, true, this);
                    return true;
                }

                @Override
                protected boolean gotResult (PostMessageResult result)
                {
                    Widget reply = makeReply(result.message);
                    reply.addStyleName("New");
                    _replies.insert(reply, 0);
                    return true;
                }

                @Override
                protected String formatError (Throwable cause)
                {
                    return translateServerError(cause);
                }
            }.setBindEnterKey(false)
             .setPromptText(_msgs.petReplyToPetition(petition.subject))
             .setConfirmChoices(_msgs.petPost(), _msgs.cancel());
        }

        protected Widget makeReply (Message reply)
        {
            Widget author = Widgets.newFlowPanel(
                Widgets.newInlineLabel(formatAuthor(reply.author), "Author"),
                Widgets.newInlineLabel(" - "),
                Widgets.newInlineLabel(ServerTime.from(reply.authored).formatReadably(), "Date"),
                Widgets.newInlineLabel(": "));
            Widget w = Widgets.newFlowPanel("Reply",
                author, Widgets.newHTML(SlingUtils.linkify(reply.body), "Body"));
            if (!reply.author.equals(_ctx.ainfo.name)) {
                w.addStyleName("Other");
            }
            return w;
        }

        protected FlowPanel _replies = Widgets.newFlowPanel("Replies");
    }

    protected SlingContext _ctx;
    protected FlowPanel _petitions = Widgets.newFlowPanel();

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
    protected static final ServerMessages _smsgs = GWT.create(ServerMessages.class);
}
