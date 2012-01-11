//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.ItemListBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.gwt.util.InputClickCallback;
import com.threerings.sling.gwt.client.SlingNav.Events;
import com.threerings.sling.gwt.ui.NamedRowSmartTable;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.gwt.util.ServerTime;
import com.threerings.sling.web.client.SlingService.AssignEventResult;
import com.threerings.sling.web.client.SlingService.PostMessageResult;
import com.threerings.sling.web.client.SlingService.TimeUnit;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.Message;
import com.threerings.sling.web.data.UniversalTime;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * A panel for viewing and editing a sling event.
 */
public class EventPanel extends FlowPanel
{
    /**
     * Creates an event panel for the given event.
     */
    public EventPanel(SlingContext ctx, Event event)
    {
        _ctx = ctx;
        _event = event;

        setStyleName("uEventPanel");

        // It is not okay to ignore petitions, just remove the item.
        if (_event.type == Event.Type.PETITION) {
            _status.removeItem(Event.Status.IGNORED_CLOSED);
        }

        _statusBar.setStyleName("StatusBar");
        _statusBar.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _statusBar.add(_status);

        add(new ParaPanel(_msgs.eventPanelTitle(_event.source.name.toString(),
            _event.subject), "Title"));

        // create the event information
        int row = 0;
        _fields.cell(row, 0).text(_msgs.eventId()).styles("Label");
        _fields.cell(row, 1).text(String.valueOf(_event.eventId));
        row++;

        _fields.cell(row, 0).text(_msgs.eventTypeLabel()).styles("Label");
        _fields.cell(row, 1).text(SlingUtils.translate(_event.type));
        row++;

        _fields.cell(row, 0).text(_msgs.languageLabel()).styles("Label");
        // hard-wired languages
        final ItemListBox<String> language = ItemListBox.<String>builder()
            .add(null, "None").add("en").add("fr").add("es").add("de").build();
        language.setSelectedItem(_event.language);
        _fields.cell(row, 1).widget(language);
        row++;

        _fields.cell(row, 0).text(_msgs.filedBy()).styles("Label");
        _fields.cell(row, 1).widget(Widgets.newFlowPanel("LinkBar",
            SlingUtils.linkToAccount(_ctx, _event.source.name),
            SlingUtils.linkToSupportHistory(_ctx, _event.source.name)));
        row++;

        _fields.cell(row, 1).text(_event.source.machineIdent != null ?
            _event.source.machineIdent : _msgs.noMachineIdent());
        row++;

        _fields.cell(row, 1).text(_event.source.ipAddress != null ?
            _event.source.ipAddress : _msgs.noIpAddress());
        row++;

        if (_event.target != null) {
            _fields.cell(row, 0).text(_msgs.filedAgainst()).styles("Label");
            _fields.cell(row, 1).widget(Widgets.newFlowPanel("LinkBar",
                SlingUtils.linkToAccount(_ctx, event.target.name),
                SlingUtils.linkToSupportHistory(_ctx, event.target.name)));
            row++;

            _fields.cell(row, 1).text(_event.target.machineIdent != null ?
                _event.target.machineIdent : _msgs.noMachineIdent());
            row++;

            _fields.cell(row, 1).text(_event.target.ipAddress != null ?
                _event.target.ipAddress : _msgs.noIpAddress());
            row++;
        }

        if (_event.link != null) {
            _fields.cell(row, 0).text(_msgs.link()).styles("Label");
            _fields.cell(row, 1).widget(new Anchor(_event.link, _event.link, "_blank"));
            row++;
        }

        _fields.cell(row, 0).text(_msgs.statusLabel()).styles("Label");
        _fields.cell(row, 1).widget(_statusBar);
        row++;

        _fields.cell(row, 0).text(_msgs.waitingForPlayerLabel()).styles("Label");
        _fields.cell(row, 1).widget(_waitingForPlayer);
        _waitingForPlayer.setValue(_event.waitingForPlayer);
        row++;

        _fields.cell(row, 0).text(_msgs.filed()).styles("Label");
        _fields.cell(row, 1).text(ServerTime.from(_event.entered).format());
        row++;

        _fields.cell(row, 0).text(_msgs.lastUpdated()).styles("Label");
        _fields.cell(row, 1).text(ServerTime.from(_event.lastUpdated).format());
        _fields.nameRow("lastUpdated", row++); // name this row for later poking

        _fields.cell(row, 0).text(_msgs.firstResponseLabel()).styles("Label");
        _fields.cell(row, 1).text(formatFirstResponse());
        _fields.nameRow("firstResponse", row++); // name this row for later poking

        add(_fields);

        // add the chat history if we have it
        if (event.chatHistory != null && event.chatHistory.length() > 0) {
            add(new ParaPanel(_msgs.chatHistoryLabel(), "SubHead"));

            if (event.chatHistory.startsWith("HTML")) {
                add(Widgets.newHTML("<pre>" + event.chatHistory.substring(4) + "</pre>",
                    "ChatHistoryHTML"));
            } else {
                add(Widgets.newLabel(event.chatHistory, "ChatHistory"));
            }
        }

        // messages
        add(Widgets.newLabel(_msgs.messages(), "MessagesHeader"));

        _postNote.addStyleName("PostMessageDisclosure");
        _postNote.setHeader(Widgets.newFlowPanel(
            Widgets.newInlineLabel(_msgs.postNoteHeader() + " "),
            SlingUtils.makeLink(ctx, _msgs.standaloneLink(), Events.postNote(event.eventId))));
        _postNote.setContent(new PostMessagePanel(ctx, event, false, this));
        add(_postNote);

        if (event.type == Event.Type.PETITION) {
            _postReply.addStyleName("PostMessageDisclosure");
            _postReply.setHeader(Widgets.newFlowPanel(
                Widgets.newInlineLabel(_msgs.postReplyHeader() + " "),
                SlingUtils.makeLink(ctx, _msgs.standaloneLink(), Events.postReply(event.eventId))));
            _postReply.setContent(new PostMessagePanel(ctx, event, true, this));
            add(_postReply);
        }

        add(_messages);
        _messages.add(Widgets.newInlineLabel(_msgs.loading()));

        final AsyncCallback<Message> statusCallback = new AsyncCallback<Message>() {
            @Override public void onFailure (Throwable caught) {
                Popups.error(_msgs.anErrorOccurred(translateServerError(caught)));
                _status.setEnabled(true);
            }

            @Override public void onSuccess (Message result) {
                // side effects of the server's status change
                switch (_event.status) {
                case IN_PROGRESS:
                    _event.owner = _ctx.ainfo.name;
                    break;
                case OPEN:
                case ESCALATED_LEAD:
                case ESCALATED_ADMIN:
                    _event.owner = null;
                    break;
                }

                statusChanged();
                addMessage(result);
            }
        };

        _status.addChangeHandler(new ChangeHandler() {
            @Override public void onChange (ChangeEvent event) {
                if (!_status.isEnabled()) {
                    // how did they do that? just bail
                    return;
                }

                _status.setEnabled(false);
                _event.status = _status.getSelectedItem();
                _ctx.undersvc.updateEvent(_event.eventId, _event.status, statusCallback);
            }
        });

        final AsyncCallback<Message> waitFlagCallback =
                new AsyncCallback<Message>() {
            @Override public void onFailure (Throwable caught) {
                Popups.error(_msgs.anErrorOccurred(translateServerError(caught)));
                _waitingForPlayer.setEnabled(true);
            }

            @Override public void onSuccess (Message result) {
                _event.waitingForPlayer = _waitingForPlayer.getValue();
                _waitingForPlayer.setEnabled(true);
                addMessage(result);
            }
        };

        _waitingForPlayer.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override public void onValueChange (ValueChangeEvent<Boolean> event) {
                _waitingForPlayer.setEnabled(false);
                _ctx.undersvc.setWaitingForPlayer(_event.eventId, event.getValue(),
                        waitFlagCallback);
            }
        });

        language.addChangeHandler(new ChangeHandler() {
            AsyncCallback<Message> callback = new AsyncCallback<Message>() {
                @Override public void onFailure (Throwable caught) {
                    Popups.error(_msgs.anErrorOccurred(translateServerError(caught)));
                    language.setEnabled(true);
                }

                @Override public void onSuccess (Message result) {
                    _event.language = language.getSelectedItem();
                    language.setEnabled(true);
                    addMessage(result);
                }
            };

            @Override public void onChange (ChangeEvent event) {
                _ctx.undersvc.setLanguage(
                    _event.eventId, language.getSelectedItem(), callback);
            }
        });

        // load up our messages
        _ctx.undersvc.loadMessages(event.eventId, new AsyncCallback<Message[]>() {
            public void onSuccess (Message[] messages) {
                _messages.clear();
                for (int ii = messages.length - 1; ii >= 0; ii--) {
                    addMessage(messages[ii]);
                }
            }
            public void onFailure (Throwable cause) {
                _messages.clear();
                _messages.add(Widgets.newInlineLabel(translateServerError(cause)));
            }
        });

        // initialize status-dependent controls
        statusChanged();
    }

    public void onNewMessage (PostMessageResult result)
    {
        _postNote.setOpen(false);
        _postReply.setOpen(false);
        _waitingForPlayer.setValue(result.waitingForPlayer);
        addMessage(result.message);
        // TODO: could also update first response cell here, but kind of a pain to match server
        // logic. user can just refresh
    }

    protected String formatFirstResponse ()
    {
        if (_event.firstResponse == null) {
            return _msgs.noResponse();
        }
        long time = _event.firstResponse;
        int days = (int)(time / TimeUnit.DAY.millis);
        time -= days * TimeUnit.DAY.millis;
        int hours = (int)(time / TimeUnit.HOUR.millis);
        time -= hours * TimeUnit.HOUR.millis;
        float minutes = (float)time / 1000 / 60;
        return _msgs.fmtFirstResponse(String.valueOf(days), String.valueOf(hours),
            FIRST_RESPONSE_MINUTES.format(minutes));
    }

    /** Adds a message to the TOP of the message panel. */
    protected void addMessage (Message message)
    {
        Widget body = Widgets.newHTML(SlingUtils.linkify(message.body), "Body");
        Widget author = SlingUtils.linkToAccount(_ctx, _ctx.toHandle(message.author));
        Widget header = Widgets.newFlowPanel("Header",
            Widgets.setStyleNames(author, "Author"),
            Widgets.newInlineLabel(" - "),
            Widgets.newInlineLabel(ServerTime.from(message.authored).format(), "Date"),
            Widgets.newInlineLabel(":"));
        _messages.insert(Widgets.setStyleNames(Widgets.newFlowPanel(header, body), "Message",
            _event.type == Event.Type.PETITION && message.access == Message.Access.NORMAL ?
                "Public" : "Private"), 0);
    }

    // TODO: remove this and other side effect emulation and just reload the event

    protected void eventTouched ()
    {
        _event.lastUpdated = UniversalTime.now();
        _fields.cell("lastUpdated", 1).text(ServerTime.from(_event.lastUpdated).format());
    }

    protected void statusChanged ()
    {
        // show or hide the action buttons as appropriate
        _status.setEnabled(_event.owner != null && _event.owner.equals(_ctx.ainfo.name)
            && _event.status.isOpen());

        while (_statusBar.getWidgetCount() > 1) {
            _statusBar.remove(1);
        }

        if (_event.owner != null) {
            Widget link = SlingUtils.linkToAccount(_ctx, _ctx.toHandle(_event.owner));
            _statusBar.add(link);
            link.addStyleName(_event.owner.equals(_ctx.ainfo.name) ? "You" : "NotYou");
        }

        if (_ctx.ainfo.name.equals(_event.owner)) {
            Button assignBtn = new Button(_msgs.assign());
            _statusBar.add(assignBtn);
            new InputClickCallback<AssignEventResult, TextBox>(assignBtn,
                    Widgets.newTextBox(_ctx.supportPrefix, 230, 50)) {
                @Override public boolean callService (String message) {
                    _ctx.undersvc.assignEvent(_event.eventId,
                        Event.Status.IN_PROGRESS, message, this);
                    return true;

                }

                @Override public boolean gotResult (AssignEventResult result) {
                    _event.status = Event.Status.IN_PROGRESS;
                    if (result != null) {
                        _event.owner = result.newOwner;
                    }
                    eventTouched();
                    statusChanged();
                    addMessage(result.logMessage);
                    return true;
                }

                @Override protected String formatError (Throwable cause) {
                    return translateServerError(cause);
                }
            }.setPromptText(_msgs.assignEventTo())
             .setConfirmChoices(_msgs.assign(), _msgs.cancel());
        }

        if (_event.owner == null || !_event.owner.equals(_ctx.ainfo.name)) {
            Button claimBtn = new Button(_msgs.claim());
            _statusBar.add(claimBtn);
            new ClickCallback<Message>(claimBtn) {
                @Override protected boolean callService () {
                    _event.status = Event.Status.IN_PROGRESS;
                    _ctx.undersvc.updateEvent(_event.eventId, _event.status, this);
                    return true;
                }

                @Override protected boolean gotResult (Message result) {
                    _event.owner = _ctx.ainfo.name; // server side effect
                    eventTouched();
                    statusChanged();
                    addMessage(result);
                    return true;
                }

                @Override protected String formatError (Throwable cause) {
                    return translateServerError(cause);
                }
            };
        }

        _status.setSelectedItem(_event.status);
    }

    protected SlingContext _ctx;
    protected Event _event;
    protected NamedRowSmartTable _fields = new NamedRowSmartTable("Fields", 0, 0);
    protected FlowPanel _messages = Widgets.newFlowPanel("Messages");
    protected HorizontalPanel _statusBar = new HorizontalPanel();
    protected ItemListBox<Event.Status> _status = SlingUtils.makeStatusListBox(Event.Status.OPEN);
    protected CheckBox _waitingForPlayer = new CheckBox();
    protected DisclosurePanel _postNote = new DisclosurePanel();
    protected DisclosurePanel _postReply = new DisclosurePanel();
    protected static final NumberFormat FIRST_RESPONSE_MINUTES = NumberFormat.getFormat("#.####");
    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
