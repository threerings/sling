//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.sling.gwt.ui.NamedRowSmartTable;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.MachineIdentity;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * Panel for displaying a number of machine identities (related to a given account). From here,
 * accounts that have used the identities can be viewed and machine identities banned or tainted.
 */
public class RelatedAccountsPanel extends NamedRowSmartTable
{
    /**
     * Creates a new related accounts panel.
     */
    public RelatedAccountsPanel (SlingContext ctx, int accountId, List<MachineIdentity> idents)
    {
        _ctx = ctx;
        _idents = idents;

        setStyleName("uRelatedAccounts");

        int row = 0;
        cell(row, 0).styles("Title").colSpan(2).text(
            _msgs.accountsRelatedToAccountId(String.valueOf(accountId)));
        row++;

        cell(row, 0).text("").colSpan(2).styles("Status");
        setStatus(_msgs.loadedRelatedAccounts());
        row++;

        final CheckBox toggleGameNames = new CheckBox(_msgs.showGameNames());
        final CheckBox toggleInactive = new CheckBox(_msgs.showInactiveAccts());
        cell(row, 0).widget(Widgets.newFlowPanel(toggleGameNames, toggleInactive)).colSpan(2);
        row++;

        cell(row, 0).text(_msgs.relAccIdentHdr()).styles("Header");
        cell(row, 1).text(_msgs.relAccAccountHdr()).styles("Header");
        nameRow("idents", row++);

        for (MachineIdentity ident : _idents) {
            cell(row, 0).widget(createIdentLabel(ident, row, 0));
            updateIdentStyle(ident, row, 0);
            cell(row, 1).widget(Widgets.newInlineLabel(""));
            row++;
        }

        ValueChangeHandler<Boolean> refresh = new ValueChangeHandler<Boolean>() {
            @Override public void onValueChange (ValueChangeEvent<Boolean> event) {
                refresh();
            }
        };

        _showInactiveAccounts = toggleInactive;
        _showGameNames = toggleGameNames;
        toggleGameNames.addValueChangeHandler(refresh);
        toggleInactive.addValueChangeHandler(refresh);
        refresh();
    }

    protected void updateIdentStyle (MachineIdentity ident, int row, int col)
    {
        if (ident.banned) {
            cell(row, 0).styles("Ident", "BannedIdent");
        } else if (ident.tainted) {
            cell(row, 0).styles("Ident", "TaintedIdent");
        } else {
            cell(row, 0).styles("Ident");
        }
    }

    protected void refresh ()
    {
        int row = getNamedRow("idents") + 1;
        for (MachineIdentity ident : _idents) {
            FlowPanel container = Widgets.newFlowPanel("Accounts");
            addNames(container, ident);
            cell(row, 1).widget(container);
            row++;
        }
    }

    protected void addNames (FlowPanel container, MachineIdentity ident)
    {
        boolean showGameNames = _showGameNames.getValue();
        boolean showInactiveAccounts = _showInactiveAccounts.getValue();
        for (MachineIdentity.AccountInfo info : ident.accounts) {
            AccountName name = info.name;
            if (!showInactiveAccounts && name.gameNames.isEmpty()) {
                continue;
            }
            FlowPanel nameWidget;
            if (showGameNames) {
                // account names in bold, followed by game names
                Widget account = Widgets.setStyleNames(SlingUtils.linkToAccount(
                    _ctx, name.accountName, name.accountName), "Name");
                if (name.gameNames.size() == 0) {
                    nameWidget = Widgets.newFlowPanel(account);
                } else {
                    String gameNames = "";
                    for (String gameName : name.gameNames) {
                        if (gameNames.length() > 0) {
                            gameNames += ", ";
                        }
                        gameNames += gameName;
                    }
                    gameNames = " (" + gameNames + ")";
                    nameWidget = Widgets.newFlowPanel(account, Widgets.newInlineLabel(gameNames));
                }
            } else {
                // just account names
                nameWidget = Widgets.newFlowPanel(
                    SlingUtils.linkToAccount(_ctx, name.accountName, name.accountName));
            }
            if (info.paid) {
                nameWidget.insert(Widgets.newInlineLabel("$", "Paid"), 0);
            }
            container.add(Widgets.setStyleNames(nameWidget,
                info.banned ? BANNED_ACCOUNT_STYLES : ACCOUNT_STYLES));
            container.getElement().appendChild(SPACE.cloneNode(false));
        }
    }

    protected void setStatus(String status)
    {
        cell(1, 0).text(status);
    }

    protected Widget createIdentLabel(final MachineIdentity ident, final int row, final int col)
    {
        final ToggleMenuItem taint = new ToggleMenuItem(
            _msgs.taintMachineIdent(), _msgs.untaintMachineIdent()) {
            @Override protected boolean getState () {
                return ident.tainted;
            }

            @Override protected void setState (boolean state) {
                ident.tainted = state;
                updateIdentStyle(ident, row, col);
            }

            @Override protected void callService (boolean value) {
                _ctx.undersvc.updateIdentTaint(ident.machIdent, value, this);
            }
        };

        final ToggleMenuItem ban = new ToggleMenuItem(
            _msgs.banMachineIdent(), _msgs.unbanMachineIdent()) {
            @Override protected boolean getState () {
                return ident.banned;
            }

            @Override protected void setState (boolean state) {
                ident.banned = state;
                updateIdentStyle(ident, row, col);
            }

            @Override protected void callService (boolean value) {
                _ctx.undersvc.updateIdentBanned(ident.machIdent, value, this);
            }
        };

        MenuBar commands = new MenuBar(true); // vertical
        commands.addItem(ban);
        commands.addItem(taint);
        commands.addItem(_msgs.viewIdent(), new Command() {
            @Override public void execute () {
                setStatus(ident.machIdent);
            }
        });

        final MenuItem arrowItem = new MenuItem(_msgs.menuTitleClosed(), commands);
        MenuBar top = new MenuBar();
        top.addItem(arrowItem);

        commands.addAttachHandler(new AttachEvent.Handler() {
            @Override public void onAttachOrDetach (AttachEvent event) {
                arrowItem.setText(event.isAttached() ?
                    _msgs.menuTitleOpened() : _msgs.menuTitleClosed());
            }
        });

        String id = ident.machIdent;
        id = id.length() > 4 ? id.substring(0, 4) : id;
        return Widgets.newRow("Ident", new Label(id), top);
    }

    protected abstract class ToggleMenuItem extends MenuItem implements Command, AsyncCallback<Void>
    {
        public ToggleMenuItem (String set, String unset)
        {
            super("", (Command)null);
            _set = set;
            _unset = unset;
            setText(getState() ? unset : set);
            setCommand(this);
        }

        @Override
        public void execute ()
        {
            _sentState = !getState();
            callService(_sentState);
        }

        @Override
        public void onFailure (Throwable caught)
        {
            setStatus(_msgs.anErrorOccurred(translateServerError(caught)));
        }

        @Override
        public void onSuccess (Void result)
        {
            setStatus(_msgs.successful(_sentState ? _set : _unset));
            setState(_sentState);
            setText(getState() ? _unset : _set);
        }

        protected abstract void callService(boolean value);
        protected abstract boolean getState();
        protected abstract void setState(boolean state);

        protected boolean _sentState;
        protected String _set;
        protected String _unset;
    }

    protected SlingContext _ctx;
    protected List<MachineIdentity> _idents;
    protected HasValue<Boolean> _showGameNames;
    protected HasValue<Boolean> _showInactiveAccounts;

    protected static final String[] ACCOUNT_STYLES = {"Account"};
    protected static final String[] BANNED_ACCOUNT_STYLES = {"Account", "Banned"};
    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
    protected static final Node SPACE = Widgets.newLabel(" ").getElement().getChild(0);
}
