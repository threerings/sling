//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.PagedRequest;
import com.threerings.gwt.util.PagedResult;
import com.threerings.gwt.util.PagedServiceDataModel;
import com.threerings.sling.gwt.client.SlingNav.Events;
import com.threerings.sling.gwt.ui.LoadingPanel;
import com.threerings.sling.gwt.util.Arguments;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.AuthorizationException;
import com.threerings.sling.gwt.util.Nav;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;
import com.threerings.sling.web.client.SlingService;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventFilter;
import com.threerings.sling.web.data.EventSearch;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * Implementation of the #events section.
 */
public class EventsSection<Ctx extends SlingContext>
    implements Section<Ctx>
{
    @Override // from Section
    public SectionId getId ()
    {
        return Events.ID;
    }

    @Override // from Section
    public Widget createView (final SlingContext ctx, Arguments args, Widget previous)
        throws AuthorizationException
    {
        AuthLevel.ADMIN.require(ctx);

        Iterator<String> shifter = args.iterator();
        final Events.Mode mode = Nav.asEnum(Events.Mode.class, shifter.next());
        switch (mode) {
        case QUICKSEARCH:
            final SlingService.Events by = Nav.asEnum(
                SlingService.Events.class, shifter.next());
            final String query = by == SlingService.Events.ACCOUNT ? shifter.next() : null;
            EventsTable events = new EventsTable(ctx);
            events.setModel(new PagedServiceDataModel<Event, PagedResult<Event>>() {
                @Override // from ServiceBackedDataModel
                protected void callFetchService (
                    PagedRequest request, AsyncCallback<PagedResult<Event>> callback)
                {
                    ctx.undersvc.loadEvents(by, query, request, callback);
                }

                @Override // from ServiceBackedDataModel
                protected void reportFailure (Throwable caught)
                {
                    Popups.error(translateServerError(caught));
                }
            }, 0);

            // for the "open" search, append a time stamp just so that returning to the original
            // token will cause a refresh
            if (by == SlingService.Events.OPEN && !shifter.hasNext()) {
                args.values.add(String.valueOf(System.currentTimeMillis()));
            }
            return events;
        case VIEW:
        case POST_NOTE:
        case POST_REPLY:
            final int eventId = Integer.parseInt(shifter.next());
            return new LoadingPanel<Event>() {
                @Override protected String callService () {
                    ctx.undersvc.loadEvent(eventId, this);
                    return _msgs.loadingEvent(String.valueOf(eventId));
                }

                @Override protected Widget finish (Event result) {
                    if (result == null) {
                        return Widgets.newLabel(_msgs.noEventFoundWithId(String.valueOf(eventId)));
                    } else if (mode == Events.Mode.VIEW) {
                        return new EventPanel(ctx, result);
                    } else {
                        boolean reply = mode == Events.Mode.POST_REPLY;
                        return new PostMessagePanel(ctx, result, reply, null);
                    }
                }

                @Override protected String formatError (Throwable caught) {
                    return translateServerError(caught);
                }
            }.start();

        case SEARCH:
            EventSearch search = new EventSearch();

            if (shifter.hasNext()) {
                // parsing search_sort_filter1type_filter1query[_filter2type_filter2query[...]]
                search.sort = Nav.asEnum(EventSearch.Sort.class, shifter.next());
                while (shifter.hasNext()) {
                    EventFilter.Type type = Nav.asEnum(EventFilter.Type.class, shifter.next());
                    search.filters.add(new EventFilter(type, shifter.next()));
                }
            }

            return new AdvancedEventSearchPanel(ctx, search, search.filters.size() > 0);
        }
        return null;
    }

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
