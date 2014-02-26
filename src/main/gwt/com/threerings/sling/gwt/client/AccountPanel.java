//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Map;

import com.google.common.collect.Maps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.LimitedTextArea;
import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.InputClickCallback;
import com.threerings.sling.gwt.client.SlingNav.Accounts;
import com.threerings.sling.gwt.ui.NamedRowSmartTable;
import com.threerings.sling.gwt.util.ServerTime;
import com.threerings.sling.web.client.SlingService.TimeUnit;
import com.threerings.sling.web.data.Account;

/**
 * Displays the properties of an account in a tabular format.
 */
public class AccountPanel extends NamedRowSmartTable
{
    /**
     * Creates and populates a new account panel for the given account.
     */
    public AccountPanel (SlingContext ctx, Account account)
    {
        super("uAccountPanel", 0, 5);
        _ctx = ctx;
        _account = account;
        init();
        _updater.update();
    }

    protected void init ()
    {
        final Label status = new Label(_msgs.loadedAccount());

        int row = 0;
        cell(row, 0).text("").colSpan(4).styles("Title");
        row++;

        cell(row, 0).colSpan(4).widget(Widgets.newRow("LinkBar",
            SlingUtils.makeLink(_ctx, _msgs.relatedAccounts(),
                Accounts.findRelated(_account.accountId)),
            SlingUtils.linkToSupportHistory(_ctx, _account.name),
            SlingUtils.makeLink(_ctx, _msgs.postNoteLink(),
                Accounts.postNote(_account.name.accountName))));
        row++;

        final Button banBtn = new Button(_msgs.banAccount());
        final Button unbanBtn = new Button(_msgs.unbanAccount());
        final CheckBox clearIdents = new CheckBox(_msgs.clearIdents());
        cell(row, 0).text(_msgs.accountId()).styles("Label");
        cell(row, 1).text("" + _account.accountId);
        cell(row, 2).widget(unbanBtn); // this cell will swap when we update later
        cell(row, 3).widget(clearIdents); // will be hidden or unhidden in update
        nameRow("account", row++);

        cell(row, 0).text(_msgs.affiliate()).styles("Label");
        cell(row, 1).text(_account.affiliate);
        row++;

        cell(row, 0).text(_msgs.registered()).styles("Label");
        cell(row, 1).widget(new Label(ServerTime.from(_account.created).formatDatePart()));
        if (_ctx.ainfo.getBillingUrl() != null) {
            String url = _ctx.ainfo.getBillingUrl() + _account.name.accountName;
            cell(row, 2).widget(new Anchor(url, _msgs.billingInfo(), "_blank"));
        }
        row++;

        cell(row, 0).text(_msgs.firstPlayed()).styles("Label");
        if (_account.firstSession != null) {
            cell(row, 1).widget(new Label(ServerTime.from(_account.firstSession).format()));
            if (_ctx.ainfo.getGameUrl() != null) {
                String url = _ctx.ainfo.getGameUrl() + _account.name.accountName;
                cell(row, 2).widget(new Anchor(url, _msgs.gameInfo(), "_blank"));
            }
            row++;
            cell(row, 0).text(_msgs.lastPlayed()).styles("Label");
            cell(row, 1).widget(new Label(ServerTime.from(_account.lastSession).format()));
        } else {
            cell(row, 1).widget(new Label(_msgs.neverLoggedOn()));
        }
        row++;

        if (_account.altName != null) {
            cell(row, 0).text(_msgs.altName()).styles("Label");
            cell(row, 1).text(_account.altName);
            row++;
        }

        final TextBox email = new TextBox();
        email.setText(_account.email);
        Button update = new Button(_msgs.update());
        cell(row, 0).text(_msgs.accountEmail()).styles("Label");
        cell(row, 1).widget(email);
        cell(row, 2).widget(update);
        row++;

        final TextBox password = new TextBox();
        Button change = new Button(_msgs.change());
        cell(row, 0).text(_msgs.passwordLabel()).styles("Label");
        cell(row, 1).widget(password);
        cell(row, 2).widget(change);
        row++;

        final NumberTextBox days = new NumberTextBox(false, 2, 2);
        days.setNumber(2);
        final Button tempBanBtn = new Button(_msgs.tempBan());
        cell(row, 0).text(_msgs.days()).styles("Label");
        cell(row, 1).widget(days);
        cell(row, 2).widget(tempBanBtn);
        nameRow("tempBan", row++);

        final Button warnBtn = new Button(_msgs.warn());
        final Button clearWarnBtn = new Button(_msgs.clearWarning());
        cell(row, 0).text(_msgs.warning()).styles("Label");
        cell(row, 2).widget(warnBtn);
        cell(row, 3).widget(clearWarnBtn);
        nameRow("warn", row++);

        cell(row, 0).text(_msgs.billingStatus()).styles("Label");
        String billingStatus = "";
        switch (_account.billingSatus) {
        case TRIAL:
            billingStatus = _msgs.billingTrial();
            break;
        case SUBSCRIBER:
            billingStatus = _msgs.billingSubscriber();
            break;
        case EX_SUBSCRIBER:
            billingStatus = _msgs.billingExSubscriber();
            break;
        case FAILURE:
            billingStatus = _msgs.billingFailure();
            break;
        case BANNED:
            billingStatus = _msgs.billingBanned();
            break;
        }
        cell(row, 1).text(billingStatus);
        row++;

        final Button editFlagsBtn = new Button(_msgs.editFlags());
        cell(row, 0).text(_msgs.flags()).styles("Label");
        cell(row, 1).text(getFlagsString()).styles("FlagsCell");
        cell(row, 2).widget(editFlagsBtn);
        nameRow("flags", row++);
        row++;

        row = addMoreControls(row);

        cell(row, 0).widget(status).colSpan(2).styles("Status");
        row++;

        _updater = new UserStatusUpdater() {
            @Override
            public void update ()
            {
                String title = _account.name.toString();
                clearWarnBtn.setVisible(false);
                if (_account.isSet(Account.Flag.BANNED)) {
                    title = _msgs.isBanned(title);
                    tempBanBtn.setEnabled(false);
                    warnBtn.setText(_msgs.updateWarning());
                } else if (_account.tempBan != null) {
                    title = _msgs.isTempBanned(title);
                    tempBanBtn.setEnabled(true);
                    tempBanBtn.setText(_msgs.updateTempBan());
                    warnBtn.setText(_msgs.updateWarning());
                } else if (_account.warning != null) {
                    title = _msgs.isWarned(title);
                    tempBanBtn.setEnabled(true);
                    tempBanBtn.setText(_msgs.tempBan());
                    warnBtn.setText(_msgs.updateWarning());
                    clearWarnBtn.setVisible(true);
                } else {
                    tempBanBtn.setEnabled(true);
                    tempBanBtn.setText(_msgs.tempBan());
                    warnBtn.setText(_msgs.warn());
                }

                cell(0, 0).text(title);

                if (_account.warning == null) {
                    cell("warn", 1).text(_msgs.noWarning());
                } else {
                    cell("warn", 1).text(_account.warning);
                }

                int row = getNamedRow("flags");
                if (row != -1) {
                    cell(row, 1).text(getFlagsString());
                }

                if (!_account.isSet(Account.Flag.BANNED) && _account.tempBan != null) {
                    if (getNamedRow("tempBanDetail") == -1) {
                        row = insertRow(getNamedRow("tempBan"));
                        nameRow("tempBanDetail", row);
                        cell(row, 1).widget(new Label(
                            ServerTime.from(_account.tempBan).format()));
                        cell(row, 0).text(_msgs.tempBan());
                        Button cancelTempBan = new Button(_msgs.cancelTempBan());
                        cell(row, 2).widget(cancelTempBan);
                        new ClickCallback<Void>(cancelTempBan) {
                            @Override
                            public boolean callService ()
                            {
                                _ctx.svc.updateTempBan(
                                    _account.name.accountName, 0, null, this);
                                return true;
                            }

                            @Override
                            public boolean gotResult (Void result)
                            {
                                status.setText(_msgs.cancelledTempBan());
                                _account.warning = null;
                                _account.tempBan = null;
                                _updater.update();
                                return true;
                            }
                        };
                    }
                } else if (getNamedRow("tempBanDetail") != -1) {
                    removeRow(getNamedRow("tempBanDetail"));
                }

                cell("account", 2).widget(_account.isSet(Account.Flag.BANNED) ? unbanBtn : banBtn);
                clearIdents.setVisible(_account.isSet(Account.Flag.BANNED));
            }
        };

        new ClickCallback<Void>(unbanBtn) {
            @Override
            public boolean callService ()
            {
                _ctx.svc.updateBanned(_account.accountId,
                    false, null, clearIdents.getValue(), this);
                return true;
            }

            @Override
            public boolean gotResult (Void result)
            {
                status.setText(_msgs.accountEnabled());
                _account.set(Account.Flag.BANNED, false);
                _account.tempBan = null;
                _account.warning = null;
                _updater.update();
                return true;
            }
        };

        final LimitedTextArea warning = new LimitedTextArea(230, 50, 5);
        warning.setText(_account.warning);

        new InputClickCallback<Void, LimitedTextArea>(banBtn, warning) {
            @Override
            public boolean callService (String message)
            {
                _banMessage = message;
                _ctx.svc.updateBanned(_account.accountId,
                    true, message, false, this);
                return true;
            }

            @Override
            public boolean gotResult (Void result)
            {
                status.setText(_msgs.accountBanned());
                _account.set(Account.Flag.BANNED, true);
                _account.tempBan = null;
                _account.warning = _banMessage;
                _updater.update();
                return true;
            }

            String _banMessage;
        }.setPromptText(_msgs.enterAReasonForTheBan());

        new ClickCallback<Void>(update, email) {
            @Override
            public boolean callService ()
            {
                String value = email.getText();
                if (value.length() > 0) {
                    _ctx.svc.updateAccount(_account.accountId, value,
                        null, this);
                    return true;
                }
                return false;
            }

            @Override
            public boolean gotResult (Void result)
            {
                status.setText(_msgs.emailUpdated());
                return true;
            }
        };

        new ClickCallback<Void>(change, password) {
            @Override
            public boolean callService ()
            {
                String value = password.getText();
                if (value.length() == 0) {
                    return false;
                }
                _ctx.svc.updateAccount(_account.accountId, null,
                    _ctx.frame.md5hex(value), this);
                return true;
            }

            @Override
            public boolean gotResult (Void result)
            {
                status.setText(_msgs.passwordUpdated());
                return true;
            }
        };

        new InputClickCallback<Void, LimitedTextArea>(tempBanBtn, warning) {
            @Override
            protected boolean callService (String input)
            {
                _banDays = days.getNumber().intValue();
                _banMessage = input;
                if (_banDays <= 0) {
                    Popups.error(_msgs.tempBanDaysMustBePositiveNumber());
                    return false;
                }
                _ctx.svc.updateTempBan(_account.name.accountName,
                    _banDays, input, this);
                return true;
            }

            @Override
            protected boolean gotResult (Void result)
            {
                status.setText(_msgs.tempBanned());
                _account.tempBan = System.currentTimeMillis() + _banDays * TimeUnit.DAY.millis;
                _account.warning = _banMessage;
                _updater.update();
                return true;
            }

            int _banDays;
            String _banMessage;
        }.setPromptText(_msgs.enterWarningMessageForThisAccount());

        new InputClickCallback<Void, LimitedTextArea>(warnBtn, warning) {
            @Override
            protected boolean callService (String input)
            {
                _warnMessage = input;
                _ctx.svc.updateWarning(_account.name.accountName, input,
                    this);
                return true;
            }

            @Override
            protected boolean gotResult (Void result)
            {
                status.setText(_account.warning == null ? _msgs.warningSet() : _msgs.warningUpdated());
                _account.warning = _warnMessage;
                if (_warnMessage == null) {
                    Console.log("This shouldn't happen. How can the warn message be null?");
                    _account.tempBan = null;
                }
                _updater.update();
                return true;
            }

            String _warnMessage;
        }.setPromptText(_msgs.enterWarningMessageForThisAccount());

        new ClickCallback<Void>(clearWarnBtn) {
            @Override
            protected boolean callService ()
            {
                _ctx.svc.updateWarning(_account.name.accountName, null,
                    this);
                return true;
            }

            @Override
            protected boolean gotResult (Void result)
            {
                status.setText(_msgs.cancelledWarning());
                _account.warning = null;
                _account.tempBan = null;
                _updater.update();
                return true;
            }
        };

        new ClickCallback<Integer>(editFlagsBtn) {
            @Override
            protected boolean callService()
            {
                int setFlags = 0, clearFlags = 0;
                for (Map.Entry<Account.Flag, CheckBox> entry : _flags.entrySet()) {
                    if (entry.getValue().getValue()) {
                        setFlags |= entry.getKey().mask();
                    } else {
                        clearFlags |= entry.getKey().mask();
                    }
                }
                _ctx.svc.updateFlags(_account.name.accountName, setFlags, clearFlags, this);
                return true;
            }

            @Override
            protected boolean gotResult (Integer result)
            {
                for (Map.Entry<Account.Flag, CheckBox> entry : _flags.entrySet()) {
                    Account.Flag flag = entry.getKey();
                    if ((flag.mask() & result) != 0) {
                        _account.set(flag, entry.getValue().getValue());
                    }
                }
                _updater.update();
                return true;
            }

            @Override
            protected int addConfirmPopupMessage (SmartTable contents, int row)
            {
                row = super.addConfirmPopupMessage(contents, row);
                if (_ctx.ainfo.isMaintainer) {
                    row = addFlag(contents, row, Account.Flag.MAINTAINER, _msgs.maintainer());
                    row = addFlag(contents, row, Account.Flag.ADMIN, _msgs.admin());
                }
                if (_ctx.ainfo.isAdmin) {
                    row = addFlag(contents, row, Account.Flag.SUPPORT, _msgs.support());
                    row = addFlag(contents, row, Account.Flag.JR_SUPPORT, _msgs.jrSupport());
                }
                row = addFlag(contents, row, Account.Flag.INSIDER, _msgs.insider());
                row = addFlag(contents, row, Account.Flag.TESTER, _msgs.tester());
                row = addFlag(contents, row, Account.Flag.DEADBEAT, _msgs.deadBeat());
                return row;
            }

            protected int addFlag (SmartTable contents, int row, Account.Flag flag, String label)
            {
                CheckBox cb = new CheckBox(label);
                _flags.put(flag, cb);
                cb.setValue(_account.isSet(flag));
                contents.setWidget(row, 0, cb);
                row++;
                return row;
            }

            protected Map<Account.Flag, CheckBox> _flags = Maps.newHashMap();
        }.setConfirmText(_msgs.editFlagsMessage(_account.name.accountName));
    }

    /**
     * Adds controls after all of the core controls, but before the related accounts. Subclasses
     * may override to include additional application-specific support fields.
     */
    protected int addMoreControls (int row)
    {
        return row;
    }

    protected String addFlag (String flags, Account.Flag flag, String label)
    {
        if (!_account.isSet(flag)) {
            return flags;
        }
        if (flags.length() > 0) {
            flags = flags + ", ";
        }
        return flags + label;
    }

    protected interface UserStatusUpdater
    {
        public void update ();
    }

    public String getFlagsString ()
    {
        String flags = "";
        flags = addFlag(flags, Account.Flag.HAS_BOUGHT_COINS, _msgs.hasBoughtCoins());
        flags = addFlag(flags, Account.Flag.HAS_BOUGHT_TIME, _msgs.hasBoughtTime());
        flags = addFlag(flags, Account.Flag.FAMILY_SUBSCRIBER, _msgs.familySubscriber());
        flags = addFlag(flags, Account.Flag.ADMIN, _msgs.admin());
        flags = addFlag(flags, Account.Flag.MAINTAINER, _msgs.maintainer());
        flags = addFlag(flags, Account.Flag.INSIDER, _msgs.insider());
        flags = addFlag(flags, Account.Flag.TESTER, _msgs.tester());
        flags = addFlag(flags, Account.Flag.SUPPORT, _msgs.support());
        flags = addFlag(flags, Account.Flag.JR_SUPPORT, _msgs.jrSupport());
        flags = addFlag(flags, Account.Flag.BIG_SPENDER, _msgs.bigSpender());
        flags = addFlag(flags, Account.Flag.DEADBEAT, _msgs.deadBeat());
        if (flags.length() == 0) {
            flags = _msgs.noFlags();
        }
        return flags;
    }

    protected SlingContext _ctx;
    protected Account _account;
    protected UserStatusUpdater _updater;

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
