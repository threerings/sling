//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.ItemListBox;
import com.threerings.gwt.ui.Widgets;
import com.threerings.sling.gwt.client.SlingNav.Accounts;
import com.threerings.sling.gwt.client.SlingNav.Events;
import com.threerings.sling.gwt.client.SlingNav.Reports;
import com.threerings.sling.gwt.util.PageAddress;

/**
 * The standard toolbar for admin users.
 */
public class AdminBar extends VerticalPanel
{
    /**
     * Creates a new admin toolbar for the given app.
     */
    public AdminBar (SlingApp<?> app)
    {
        _app = app;
        setStyleName("uAdminBar");
        setVerticalAlignment(ALIGN_MIDDLE);
        setHorizontalAlignment(ALIGN_CENTER);

        final ItemListBox<Accounts.SearchBy> accountSearchType =
            ItemListBox.<Accounts.SearchBy>builder()
            .add(Accounts.SearchBy.ACCOUNTNAME, _msgs.accountName())
            .add(Accounts.SearchBy.USERNAME, _msgs.name())
            .add(Accounts.SearchBy.EMAIL, _msgs.emailItem())
            .add(Accounts.SearchBy.GAMENAME, _msgs.gameName())
            .build();

        final TextBox accountQuery = Widgets.newTextBox("", 512, 24);
        final TextBox eventIdQuery = Widgets.newTextBox("", 7, 5);

        Label logout = Widgets.newLabel(_msgs.logout(), "actionLabel");

        HorizontalPanel row = Widgets.newRow("AdminRow");
        add(row);

        row.add(Widgets.newFlowPanel("Accounts",
            Widgets.newLabel(_msgs.searchLabel()),
            accountSearchType, accountQuery));

        row.add(Widgets.newFlowPanel("Events",
            Widgets.newLabel("\u2022", "SpacerLabel"),
            Widgets.newLabel(_msgs.eventsLabel(), "BarLabel"),
            SlingUtils.makeLink(_app.getContext(), _msgs.open(), Events.quickSearchOpen()),
            SlingUtils.makeLink(_app.getContext(), _msgs.mine(), Events.quickSearchMy()),
            SlingUtils.makeLink(_app.getContext(), _msgs.all(), Events.quickSearchAll()),
            eventIdQuery,
            SlingUtils.makeLink(_app.getContext(), _msgs.advanced(), Events.search())));

        row.add(Widgets.newFlowPanel("LoginStatus",
            Widgets.newLabel("\u2022", "SpacerLabel"),
            Widgets.newLabel(_msgs.loggedInAs(), "BarLabel"),
            Widgets.newLabel(_app.getContext().ainfo.name.accountName),
            logout));

        if (_app.getContext().isAdmin()) {
            row.add(Widgets.newFlowPanel("CreateAccount",
                Widgets.newLabel("\u2022", "SpacerLabel"),
                SlingUtils.makeLink(
                    _app.getContext(), _msgs.createAccount(), Accounts.createAccount())));
        }

        row = Widgets.newRow("AdminRow");
        add(row);
        row.add(Widgets.newFlowPanel("AgentActivity",
            Widgets.newLabel("\u2022", "SpacerLabel"),
            SlingUtils.makeLink(_app.getContext(), _msgs.agentActivity(), Reports.agentActivity())));

        ClickHandler accountSearch = new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                nav(Accounts.find(accountSearchType.getSelectedItem(), accountQuery.getText()));
            }
        };

        EnterClickAdapter.bind(accountQuery, accountSearch);

        ClickHandler eventSearch = new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                nav(Events.loadEvent(Integer.parseInt(eventIdQuery.getText())));
            }
        };

        EnterClickAdapter.bind(eventIdQuery, eventSearch);

        logout.addClickHandler(new ClickHandler() {
            @Override
            public void onClick (ClickEvent event)
            {
                _app.logout();
            }
        });
    }

    protected void nav (PageAddress addr)
    {
        _app.getContext().navigate(addr);
    }

    protected SlingApp<?> _app;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
