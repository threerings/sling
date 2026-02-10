//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.ItemListBox;
import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.InputException;
import com.threerings.gwt.util.PagedRequest;
import com.threerings.gwt.util.PagedResult;
import com.threerings.gwt.util.PagedServiceDataModel;
import com.threerings.sling.gwt.client.SlingNav.Events;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.gwt.ui.TimeRangeWidget;
import com.threerings.sling.gwt.ui.TimeSpanWidget;
import com.threerings.sling.gwt.util.TimeRanges;
import com.threerings.sling.web.client.SlingService.TimeUnit;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventFilter;
import com.threerings.sling.web.data.EventSearch;
import com.threerings.sling.web.data.TimeRange;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * User interface for displaying, editing and executing an {@link EventSearch}.
 */
public class AdvancedEventSearchPanel extends FlowPanel
{
    /**
     * Displays the given event search and a button to run it. If requested, runs the search
     * initially. Once the search is run, calls {@link SlingContext.Frame#navigatedTo()} to reflect
     * a token value that will recreate the search.
     */
    public AdvancedEventSearchPanel (SlingContext ctx, final EventSearch search, boolean run)
    {
        _ctx = ctx;
        setStyleName("uAdvancedEventSearchPanel");

        initDefaultFilters();

        add(new ParaPanel(_msgs.advancedEventSearch(), "Title"));

        final SmartTable inputs = new SmartTable("Inputs", 0, 5);
        int row = 0;

        for (int ii = 0; ii < 3; ++ii) {
            EventFilter filter = search.filters.size() > ii ? search.filters.get(ii) : null;
            _filters.add(new FilterRow(inputs, filter, row));
            row++;
        }

        Button searchBtn = new Button(_msgs.search());
        inputs.cell(row, 1).widget(searchBtn);
        row++;

        add(inputs);

        ClickHandler searchClick = new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                search.filters.clear();
                for (FilterRow row : _filters) {
                    if (!row.isEnabled()) {
                        continue;
                    }
                    EventFilter filter = row.toFilter();
                    if (filter == null) {
                        return;
                    }
                    search.filters.add(filter);
                }
                if (search.filters.isEmpty()) {
                    Popups.errorBelow(_msgs.errNoFilters(), inputs);
                    return;
                }
                Console.log("Searching: " + search);
                if (_events == null) {
                    add(_events = new EventsTable(_ctx));
                }
                _events.setModel(toModel(search), 0);
                _ctx.frame.navigatedTo(Events.search(search));
            }
        };
        searchBtn.addClickHandler(searchClick);
        if (run) {
            searchClick.onClick(null);
        }
    }

    protected void initDefaultFilters ()
    {
        for (EventFilter.Type type : new EventFilter.Type[] {
            EventFilter.Type.CHAT_HISTORY_MATCHES,
            EventFilter.Type.GAME_NAME_IS,
            EventFilter.Type.ACCOUNT_NAME_IS,
            EventFilter.Type.NOTE_MATCHES,
            EventFilter.Type.OWNER_IS,
            EventFilter.Type.SUBJECT_MATCHES,
            EventFilter.Type.HAS_NOTE}) {
            _defaultFilters.put(type, new EventFilter(type, ""));
        }
        _defaultFilters.put(EventFilter.Type.CREATED_BETWEEN,
            EventFilter.createdIn(TimeRanges.recentDays(30)));
        _defaultFilters.put(EventFilter.Type.UPDATED_BETWEEN,
            EventFilter.updatedIn(TimeRanges.recentDays(30)));
        _defaultFilters.put(EventFilter.Type.FIRST_RESPONSE_IS_MORE_THAN,
            EventFilter.firstResponseIsMoreThan(TimeUnit.DAY.millis));
        _defaultFilters.put(EventFilter.Type.OWNER_ID_IS, EventFilter.ownerIdIs(0));
        _defaultFilters.put(EventFilter.Type.TYPE_IS, EventFilter.typeIs(Event.Type.PETITION));
        _defaultFilters.put(EventFilter.Type.STATUS_IS, EventFilter.statusIs(Event.Status.OPEN));
        _defaultFilters.put(EventFilter.Type.IP_ADDRESS_IS, EventFilter.ipAddressIs("0.0.0.0"));
        _defaultFilters.put(EventFilter.Type.MACHINE_IDENT_IS, EventFilter.machineIdentIs(""));
        _defaultFilters.put(EventFilter.Type.WAITING_FOR_PLAYER, EventFilter.waitingForPlayer(true));
        _defaultFilters.put(EventFilter.Type.LANGUAGE_IS, EventFilter.languageIs("fr"));
    }

    protected class FilterRow
    {
        public FilterRow (SmartTable inputs, EventFilter filter, int row)
        {
            _filter = filter;
            inputs.cell(row, 0).widget(_enabled);
            inputs.cell(row, 1).widget(_types);
            inputs.cell(row, 2).widget(_params);

            _types.addChangeHandler(new ChangeHandler() {
                @Override public void onChange (ChangeEvent event) {
                    onTypeChange();
                }
            });

            _enabled.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                @Override public void onValueChange (ValueChangeEvent<Boolean> event) {
                    onEnabledChange();
                }
            });

            _enabled.setValue(_filter != null);
            if (_filter == null) {
                _filter = _defaultFilters.get(EventFilter.Type.OWNER_IS);
            }
            _types.setSelectedItem(_filter.type);
            onEnabledChange();
        }

        public boolean isEnabled ()
        {
            return _enabled.getValue();
        }

        public EventFilter toFilter ()
        {
            EventFilter filter = _params.parse(_types.getSelectedItem(), true);
            if (filter != null) {
                _params.update(filter);
            }
            return filter;
        }

        protected void onEnabledChange ()
        {
            _types.setEnabled(_enabled.getValue());
            _params.setVisible(_enabled.getValue());
            if (_enabled.getValue()) {
                onTypeChange();
            }
        }

        protected void onTypeChange ()
        {
            if (_filter == null) {
                throw new IllegalStateException();
            }

            EventFilter.Type newType = _types.getSelectedItem();
            if (_filter.type != newType) {
                try {
                    EventFilter filter = _params.parse(_filter.type, false);
                    if (filter != null) {
                        _defaultFilters.put(filter.type, filter);
                    }
                } catch (InputException ex) {
                    // swallow it
                }
                _filter = _defaultFilters.get(newType);
            }

            _params.update(_filter);
        }

        protected EventFilter _filter;
        protected final CheckBox _enabled = new CheckBox();
        protected final ItemListBox<EventFilter.Type> _types =
            ItemListBox.<EventFilter.Type>builder()
                .add(EventFilter.Type.TYPE_IS, _msgs.eventType())
                .add(EventFilter.Type.STATUS_IS, _msgs.status())
                .add(EventFilter.Type.IP_ADDRESS_IS, _msgs.ipAddress())
                .add(EventFilter.Type.MACHINE_IDENT_IS, _msgs.machineIdent())
                .add(EventFilter.Type.HAS_NOTE, _msgs.hasNote())
                .add(EventFilter.Type.CREATED_BETWEEN, _msgs.creationDate())
                .add(EventFilter.Type.UPDATED_BETWEEN, _msgs.updatedDate())
                .add(EventFilter.Type.OWNER_IS, _msgs.owner())
                .add(EventFilter.Type.OWNER_ID_IS, _msgs.ownerId())
                .add(EventFilter.Type.GAME_NAME_IS, _msgs.gameName())
                .add(EventFilter.Type.ACCOUNT_NAME_IS, _msgs.accountName())
                .add(EventFilter.Type.SUBJECT_MATCHES, _msgs.subject())
                .add(EventFilter.Type.CHAT_HISTORY_MATCHES, _msgs.chatHistory())
                .add(EventFilter.Type.NOTE_MATCHES, _msgs.notes())
                .add(EventFilter.Type.FIRST_RESPONSE_IS_MORE_THAN, _msgs.firstResponse())
                .add(EventFilter.Type.WAITING_FOR_PLAYER, _msgs.waitingForPlayer())
                .add(EventFilter.Type.LANGUAGE_IS, _msgs.ticketLanguage())
                .build();
        protected FilterParams _params = new FilterParams();
    }

    protected static class FilterParams extends HorizontalPanel
    {
        public void update (EventFilter filter)
        {
            clear();
            switch (filter.type) {
            case CREATED_BETWEEN:
            case UPDATED_BETWEEN:
                add(new TimeRangeWidget(filter.getTimeRange(), true));
                break;
            case TYPE_IS:
                ItemListBox<Event.Type> types = ItemListBox.<Event.Type>builder()
                    .add(Event.Type.COMPLAINT, _msgs.eventTypeComplaint())
                    .add(Event.Type.NOTE, _msgs.eventTypeNote())
                    .add(Event.Type.PETITION, _msgs.eventTypePetition())
                    .add(Event.Type.SUPPORT_ACTION, _msgs.eventTypeSupportAction())
                    .build();
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(types);
                types.setSelectedItem(filter.getEventType());
                break;
            case STATUS_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(SlingUtils.makeStatusListBox(filter.getEventStatus()));
                break;
            case IP_ADDRESS_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(Widgets.newTextBox(filter.getIpAddress(), 15, 15));
                break;
            case MACHINE_IDENT_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(Widgets.newTextBox(filter.getMachineIdent(), 230, 50));
                break;
            case CHAT_HISTORY_MATCHES:
            case NOTE_MATCHES:
            case SUBJECT_MATCHES:
                add(Widgets.newLabel(_msgs.matchesTerms()));
                add(Widgets.newTextBox(filter.getSearchTerms(), 230, 50));
                break;
            case HAS_NOTE:
                break;
            case OWNER_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(Widgets.newTextBox(filter.getOwner(), 230, 50));
                break;
            case GAME_NAME_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(Widgets.newTextBox(filter.getGameName(), 230, 50));
                break;
            case ACCOUNT_NAME_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(Widgets.newTextBox(filter.getAccountName(), 128, 50));
                break;
            case OWNER_ID_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(NumberTextBox.newIntBox(24, 6));
                break;
            case FIRST_RESPONSE_IS_MORE_THAN:
                add(Widgets.newLabel(_msgs.isMoreThan()));
                add(new TimeSpanWidget(filter.getMillis()));
                break;
            case WAITING_FOR_PLAYER:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(ItemListBox.<Boolean>builder()
                    .add(false, _msgs.falseValue())
                    .add(true, _msgs.trueValue())
                    .select(filter.getBoolean()).build());
                break;
            case LANGUAGE_IS:
                add(Widgets.newLabel(_msgs.isEqualTo()));
                add(Widgets.newTextBox(filter.getLanguage(), 2, 2));
                break;
            }
        }

        public EventFilter parse (EventFilter.Type type, boolean popupError)
        {
            switch (type) {
            case CREATED_BETWEEN:
                return EventFilter.createdIn(getTimeRange(0, popupError));
            case UPDATED_BETWEEN:
                return EventFilter.updatedIn(getTimeRange(0, popupError));
            case TYPE_IS:
                return EventFilter.typeIs(getItem(Event.Type.class, 1));
            case STATUS_IS:
                return EventFilter.statusIs(getItem(Event.Status.class, 1));
            case IP_ADDRESS_IS:
                return EventFilter.ipAddressIs(getNonEmptyText(1, popupError));
            case MACHINE_IDENT_IS:
                return EventFilter.machineIdentIs(getNonEmptyText(1, popupError));
            case CHAT_HISTORY_MATCHES:
                return EventFilter.chatHistoryMatches(getNonEmptyText(1, popupError));
            case NOTE_MATCHES:
                return EventFilter.noteMatches(getNonEmptyText(1, popupError));
            case SUBJECT_MATCHES:
                return EventFilter.subjectMatches(getNonEmptyText(1, popupError));
            case GAME_NAME_IS:
                return EventFilter.gameNameIs(getNonEmptyText(1, popupError));
            case ACCOUNT_NAME_IS:
                return EventFilter.accountNameIs(getNonEmptyText(1, popupError));
            case HAS_NOTE:
                return EventFilter.hasNote();
            case OWNER_IS:
                return EventFilter.ownerIs(getNonEmptyText(1, popupError));
            case OWNER_ID_IS:
                return EventFilter.ownerIdIs(getNumber(1).intValue());
            case FIRST_RESPONSE_IS_MORE_THAN:
                return EventFilter.firstResponseIsMoreThan(getTimespan(1, popupError));
            case WAITING_FOR_PLAYER:
                return EventFilter.waitingForPlayer(getItem(Boolean.class, 1));
            case LANGUAGE_IS:
                return EventFilter.languageIs(getText(1, popupError));
            default:
                throw new IllegalArgumentException("Unknown event filter type: " + type);
            }
        }

        @Override public void add (Widget w)
        {
            super.add(w);
            setCellVerticalAlignment(w, ALIGN_MIDDLE);
        }

        protected TimeRange getTimeRange (int childIdx, boolean popupError)
        {
            return ((TimeRangeWidget)getChildren().get(childIdx)).require(popupError);
        }

        protected long getTimespan (int childIdx, boolean popupError)
        {
            return ((TimeSpanWidget)getChildren().get(childIdx)).require(popupError);
        }

        protected <T> T getItem (Class<T> tclass, int childIdx)
        {
            @SuppressWarnings("unchecked")
            T item = ((ItemListBox<T>)getChildren().get(childIdx)).getSelectedItem();
            return item;
        }

        protected String getNonEmptyText (int childIdx, boolean popupError)
        {
            String text = getText(childIdx, popupError);
            if (popupError && text.trim().length() == 0) {
                Popups.errorBelow(_msgs.errEnterQuery(), getChildren().get(childIdx));
                throw new InputException();
            }
            return text;
        }

        protected String getText (int childIdx, boolean popupError)
        {
            return ((HasText)getChildren().get(childIdx)).getText();
        }

        protected Number getNumber (int childIdx)
        {
            return ((NumberTextBox)getChildren().get(childIdx)).getNumber();
        }
    }

    protected PagedServiceDataModel<Event, PagedResult<Event>> toModel (final EventSearch search)
    {
        return new PagedServiceDataModel<Event, PagedResult<Event>>() {
            @Override // from ServiceBackedDataModel
                 protected void callFetchService (
                     PagedRequest request, AsyncCallback<PagedResult<Event>> callback)
            {
                _ctx.svc.searchEvents(search, request, callback);
            }

            @Override // from ServiceBackedDataModel
            protected void reportFailure (Throwable caught)
            {
                Popups.error(translateServerError(caught));
            }
        };
    }

    protected SlingContext _ctx;
    protected List<FilterRow> _filters = Lists.newArrayList();
    protected EventsTable _events;
    protected Map<EventFilter.Type, EventFilter> _defaultFilters = Maps.newHashMap();

    protected static ClientMessages _msgs = GWT.create(ClientMessages.class);
}
