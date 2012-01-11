//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;
import com.threerings.sling.gwt.ui.ParaPanel;
import com.threerings.sling.gwt.util.Arguments;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.AuthorizationException;
import com.threerings.sling.gwt.util.Nav;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;
import com.threerings.sling.web.data.Account;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.MachineIdentity;

import static com.threerings.sling.gwt.client.SlingNav.Accounts;
import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * Implementation of the #accounts section. 
 */
public class AccountsSection<Ctx extends SlingContext>
    implements Section<Ctx>
{
    @Override // from Section
    public SectionId getId ()
    {
        return Accounts.ID;
    }

    @Override // from Section
    public Widget createView (final Ctx ctx, Arguments args, Widget previous)
        throws AuthorizationException
    {
        AuthLevel.ADMIN.require(ctx);

        Iterator<String> shifter = args.iterator();
        Accounts.Mode mode = Nav.asEnum(Accounts.Mode.class, shifter.next());
        switch(mode) {
        case POSTNOTE:
            return new PostNotePanel(ctx, shifter.next());
        case RELATED:
            final int accountId = Integer.parseInt(shifter.next());
            final SimplePanel container = Widgets.newSimplePanel(null,
                Widgets.newInlineLabel(_msgs.findingRelatedAccounts()));
            ctx.undersvc.getRelatedAccounts(accountId,
                new AsyncCallback<List<MachineIdentity>>() {
                    @Override
                    public void onFailure (Throwable caught)
                    {
                        container.setWidget(Widgets.newInlineLabel(
                            _msgs.anErrorOccurred(translateServerError(caught)), "uError"));
                    }

                    @Override
                    public void onSuccess (List<MachineIdentity> result)
                    {
                        if (result.isEmpty()) {
                            container.setWidget(Widgets.newInlineLabel(
                                _msgs.noRelatedAccountsFoundForAccountId(String.valueOf(accountId))));
                        } else {
                            container.setWidget(new RelatedAccountsPanel(ctx, accountId, result));
                        }
                    }
                });
            return container;
        case SEARCH:
            break; // continued below
        default:
            return null;
        }

        SimplePanel container = Widgets.newSimplePanel(null,
            Widgets.newInlineLabel(_msgs.searching()));

        String search = shifter.next();
        String query = shifter.next();
        switch(Nav.asEnum(Accounts.SearchBy.class, search)) {
        case USERNAME:
            ctx.undersvc.getAccount(query, createAccountCallback(ctx, container,
                _msgs.accountNotFound(query)));
            break;
        case ACCOUNTNAME:
            ctx.undersvc.getAccountByName(query, createAccountCallback(ctx, container,
                _msgs.accountNotFoundWithName(query)));
            break;
        case GAMENAME:
            ctx.undersvc.findAccountsByGameName(query, createListCallback(ctx,
                container, _msgs.noAccountsFoundMatchingGameName(query),
                _msgs.accountsMatchingGameName(query)));
            break;
        case EMAIL:
            ctx.undersvc.findAccountsByEmail(query, createListCallback(ctx, container,
                _msgs.noAccountsFoundMatchingEmail(query),
                _msgs.accountsMatchingEmail(query)));
            break;
        default:
            return null;
        }
        return container;
    }

    protected AsyncCallback<Account> createAccountCallback (final Ctx ctx,
        final SimplePanel target, final String error)
    {
        return new AsyncCallback<Account>() {
            public void onSuccess (Account result)
            {
                if (result == null) {
                    target.setWidget(Widgets.newInlineLabel(error));
                } else {
                    target.setWidget(createAccountPanel(ctx, result));
                }
            }

            public void onFailure (Throwable cause)
            {
                target.setWidget(Widgets.newInlineLabel(translateServerError(cause)));
            }
        };
    }

    protected AsyncCallback<AccountName[]> createListCallback (final Ctx ctx,
        final SimplePanel target, final String error, final String panelTitle)
    {
        return new AsyncCallback<AccountName[]>() {
            public void onSuccess (AccountName[] names)
            {
                if (names.length == 0) {
                    target.setWidget(Widgets.newInlineLabel(error));
                } else if (names.length == 1) {
                    target.setWidget(Widgets.newInlineLabel(_msgs.foundOneMatchOpening()));
                    ctx.undersvc.getAccountByName(names[0].accountName,
                        createAccountCallback(ctx, target, _msgs.accountNotFound(
                            names[0].toString())));
                } else {
                    target.setWidget(createAccountListPanel(ctx, names, panelTitle));
                }
            }

            public void onFailure (Throwable cause)
            {
                target.setWidget(Widgets.newInlineLabel(translateServerError(cause)));
            }
        };
    }

    protected Widget createAccountPanel (Ctx ctx, Account account)
    {
        return new AccountPanel(ctx, account);
    }

    protected Widget createAccountListPanel (Ctx ctx, AccountName[] names, String title)
    {
        FlowPanel panel = Widgets.newFlowPanel("uAccountList",
            Widgets.newInlineLabel(title, "Title"));
        for (AccountName name : names) {
            panel.add(new ParaPanel(SlingUtils.linkToAccount(ctx, name), "Account"));
        }
        return panel;
    }

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
