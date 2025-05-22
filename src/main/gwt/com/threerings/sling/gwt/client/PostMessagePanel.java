//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;

import com.threerings.gwt.ui.InputUtil;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.web.client.SlingService.PostMessageResult;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.Message;

/**
 * Self-contained interface for support to post a message to an event. May optionally be embedded
 * in an {@code EventPanel}, in which case the ui is streamlined for posting private support
 * messages and event summary info is not shown.
 */
public class PostMessagePanel extends FlowPanel
{
    public PostMessagePanel (final SlingContext ctx, final Event event,
        final boolean playerVisible, final EventPanel parent)
    {
        setStyleName("uPostMessage");

        // only show the summary in standalone mode
        if (parent == null) {
            String type = SlingUtils.translate(event.type);
            String evId = String.valueOf(event.eventId);
            add(new ParaPanel(playerVisible ? _msgs.postReplyTitle(type, evId) :
                _msgs.postNoteTitle(type, evId), "Title"));

            String who = event.source.name.toString();
            if (event.target != null) {
                who = _msgs.postMessageSubtitleAgainst(who, event.target.name.toString());
            } else {
                who = _msgs.postMessageSubtitle(who);
            }
            add(Widgets.newFlowPanel("SubTitle",
                new ParaPanel(SlingUtils.linkify(event.subject), "Subject"),
                new ParaPanel(who, "Who")));
        }

        final Button post = new Button(playerVisible ? _msgs.postReply() : _msgs.postNote());
        final CheckBox updateWaitFlag = new CheckBox(_msgs.setWaitForPlayer());
        updateWaitFlag.setTitle(_msgs.setWaitForPlayerTip());
        FlowPanel postPanel = Widgets.newFlowPanel("Button", post);
        if (playerVisible && !event.waitingForPlayer) {
            postPanel.add(updateWaitFlag);
        }
        add(postPanel);

        if (playerVisible) {
            add(Widgets.newFlowPanel("Warning", Widgets.newHTML(
                _msgs.showMessagesToPlayerWarning())));
        }

        final TextArea msg = Widgets.newTextArea("", 100, 20);
        add(Widgets.newFlowPanel("Text", msg));

        new ClickCallback<PostMessageResult>(post) {
            @Override protected boolean callService () {
                ctx.svc.postMessage(event.eventId,
                    playerVisible ? Message.Access.NORMAL : Message.Access.SUPPORT,
                    InputUtil.requireNonEmpty(msg, _msgs.errNoMessage()),
                    playerVisible ? updateWaitFlag.getValue() : false, this);
                return true;
            }

            @Override protected boolean gotResult (PostMessageResult result) {
                if (result.sendError != null) {
                    Popups.info(_msgs.messagePostedButNotSent(
                        SlingUtils.translateServerMessage(result.sendError)));
                } else {
                    Popups.info(_msgs.messagePosted());
                }
                if (parent != null) {
                    parent.onNewMessage(result);
                }
                return true;
            }

            @Override protected String formatError (Throwable cause) {
                return SlingUtils.translateServerError(cause);
            }
        };
    }

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
