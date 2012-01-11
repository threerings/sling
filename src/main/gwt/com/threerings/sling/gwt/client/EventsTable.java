//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.sling.gwt.client.SlingNav.Events;
import com.threerings.sling.gwt.util.ServerTime;
import com.threerings.sling.web.data.Event;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * A table of events supporting paging.
 */
public class EventsTable extends PagedTable<Event>
{
    /**
     * Creates a new event table. The caller is expected to call
     * {@link #setModel(com.threerings.gwt.util.DataModel, int)} after construction.
     */
    public EventsTable (SlingContext ctx)
    {
        super(50);
        _ctx = ctx;
    }

    @Override // from PagedWidget
    protected String getEmptyMessage ()
    {
        return _msgs.noEventsFound();
    }

    @Override // from PagedTable
    protected List<Widget> createHeader ()
    {
        return Lists.<Widget>newArrayList(
            new HTML(_msgs.idHeader()),
            new HTML(_msgs.eventTypeHeader()),
            new HTML(_msgs.languageHeader()),
            new HTML(_msgs.source() + "<br/>" + _msgs.target()),
            new HTML(_msgs.idents()),
            new HTML(_msgs.ipAddresses()),
            new HTML(_msgs.subject()),
            new HTML(_msgs.lastUpdatedHeader()),
            new HTML(_msgs.statusHeader()),
            new HTML(""));
    }

    @Override // from PagedTable
    protected List<Widget> createRow (Event event)
    {
        FlowPanel srctarg = new FlowPanel();
        FlowPanel srctargId = new FlowPanel();
        FlowPanel srctargIp = new FlowPanel();
        addAccountWidgets(event.source, srctarg, srctargId, srctargIp);
        addAccountWidgets(event.target, srctarg, srctargId, srctargIp);

        HTML lastUpdated = new HTML(ServerTime.from(event.lastUpdated).format());
        HTML status = new HTML(SlingUtils.translate(event.status));

        Widget subject = SlingUtils.makeLink(_ctx, event.subject, Events.loadEvent(event.eventId));

        Widget check;
        if (event.status == Event.Status.OPEN) {
            CheckBox cb = new CheckBox();
            cb.setName(String.valueOf(event.eventId));
            _checkboxen.add(cb);
            check = cb;
        } else {
            check = new HTML("");
        }

        return Lists.newArrayList(
            new HTML(String.valueOf(event.eventId)),
            new HTML(SlingUtils.translate(event.type)),
            new HTML(event.language != null ? event.language : "-"),
            srctarg,
            srctargId,
            srctargIp,
            subject,
            lastUpdated,
            status,
            check);
    }

    @Override // from PagedTable
    protected void didAddRow (final SmartTable table, final int row, Event event)
    {
        // this is kinda messy, maybe PagedTable could better support this pattern... some day
        table.getRowFormatter().setStyleName(row, event.getStatusStyle());
    }

    @Override // from PagedTable
    protected SmartTable createContents (int start, int count, List<Event> list)
    {
        _checkboxen.clear();
        SmartTable table = super.createContents(start, count, list);
        table.addStyleName("uEventsTable");
        table.setCellSpacing(0);
        table.setCellPadding(0);
        return table;
    }

    @Override // from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new HTML(_msgs.loading());
    }

    @Override // from PagedWidget
    protected void addCustomControls (FlexTable controls)
    {
        controls.setWidget(0, 0, new Button(_msgs.ignoreSelected(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                ignoreSelected();
            }
        }));
        super.addCustomControls(controls);
    }

    protected void addAccountWidgets (Event.Participant party,
        HasWidgets names, HasWidgets ids, HasWidgets ips)
    {
        if (party == null) {
            names.add(new HTML("&Oslash"));
            ids.add(new Label(""));
            ips.add(new Label(""));
        } else {
            names.add(SlingUtils.linkToAccount(_ctx, party.name));
            ips.add(new Label(party.ipAddress != null ? party.ipAddress : "-"));
            ids.add(new Label(party.machineIdent != null ? party.machineIdent : "-"));
        }
    }

    protected void ignoreSelected ()
    {
        List<Integer> checkedIds = Lists.newArrayList();
        for (CheckBox cb : _checkboxen) {
            if (cb.getValue()) {
                checkedIds.add(Integer.parseInt(cb.getName()));
            }
        }
        int[] eventIds = new int[checkedIds.size()];
        for (int ii = 0, size = checkedIds.size(); ii < size; ++ii) {
            eventIds[ii] = checkedIds.get(ii);
        }
        _ctx.undersvc.updateEvents(eventIds, Event.Status.IGNORED_CLOSED,
            new AsyncCallback<Void>() {
                public void onSuccess (Void result)
                {
                    // TODO: why go to open events here?
                    _ctx.navigate(Events.quickSearchOpen());
                }

                public void onFailure (Throwable cause)
                {
                    Popups.error(translateServerError(cause));
                }
            });
    }

    protected SlingContext _ctx;
    protected List<CheckBox> _checkboxen = Lists.newArrayList();
    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
