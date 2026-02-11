//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.List;
import java.util.MissingResourceException;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.ItemListBox;
import com.threerings.gwt.util.Console;
import com.threerings.sling.gwt.client.SlingNav.Accounts;
import com.threerings.sling.gwt.client.SlingNav.Events;
import com.threerings.sling.gwt.util.PageAddress;
import com.threerings.sling.web.client.SlingException;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.Event.Status;
import com.threerings.sling.web.data.Event;

/**
 * Static utilities for the sling client library and library users.
 */
public class SlingUtils
{
    /**
     * Translates an error from the server using messages defined in {@link ServerMessages} as
     * well as any application bundles added with {@link #addServerErrorBundle}.
     */
    public static String translateServerError (Throwable error)
    {
        if (error instanceof SlingException) {
            return translateServerMessage(error.getMessage());
        } else {
            return _serverErrorBundles.get(0).getString("internal_error");
        }
    }

    /**
     * Translates a message from the server using messages defined in {@link ServerMessages} as
     * well as any application bundles added with {@link #addServerErrorBundle}.
     */
    public static String translateServerMessage (String msg)
    {
        // ConstantsWithLookup can't handle things that don't look like method names, yay!
        if (msg.startsWith("m.") || msg.startsWith("e.")) {
            msg = msg.substring(2);
        }
        for (ConstantsWithLookup bundle : _serverErrorBundles) {
            try {
                return bundle.getString(msg);
            } catch (MissingResourceException e) {
                // looking up a missing translation message throws an exception, yay!
            }
        }
        return msg;
    }

    /**
     * Add a new bundle of translations for server error messages. All added bundles will be
     * consulted in order by {@link #translateServerError}.
     */
    public static void addServerErrorBundle (ConstantsWithLookup bundle)
    {
        _serverErrorBundles.add(bundle);
    }

    /**
     * Creates a new link to the given address with the given label.
     */
    public static Hyperlink makeLink (SlingContext ctx, String label, PageAddress address)
    {
        return new Hyperlink(label, ctx.frame.toToken(address));
    }

    /**
     * Creates a link to an account name with the given label.
     */
    public static Widget linkToAccount (SlingContext ctx, String label, String acctName)
    {
        return makeLink(ctx, label, Accounts.findAccount(acctName));
    }

    /**
     * Creates a widget to display for the given account. This will be a link if the account name
     * and user auth level support it, otherwise just a label.
     */
    public static Widget linkToAccount (SlingContext ctx, AccountName acct)
    {
        if (acct.accountName.length() > 0 && ctx.isSupport()) {
            return linkToAccount(ctx, acct.toString(), acct.accountName);
        }
        return new Label(acct.toString());
    }

    /**
     * Creates a widget linking to the support history of the given account.
     */
    public static Widget linkToSupportHistory (SlingContext ctx, AccountName acct)
    {
        return makeLink(ctx, _cmsgs.supportHistory(),
            Events.quickSearchAccount(acct.accountName));
    }

    /**
     * Translates an event type using the i18n messages.
     */
    public static String translate (Event.Type type)
    {
        if (type == null) {
            return _cmsgs.eventTypeLegacy();
        }
        switch (type) {
        case COMPLAINT:
            return _cmsgs.eventTypeComplaint();
        case NOTE:
            return _cmsgs.eventTypeNote();
        case PETITION:
            return _cmsgs.eventTypePetition();
        case SUPPORT_ACTION:
            return _cmsgs.eventTypeSupportAction();
        default:
            Console.log("Missing event type case", "type", type);
            return type.toString();
        }
    }

    /**
     * Translates an event status using the i18n messages.
     */
    public static String translate (Event.Status status)
    {
        switch(status) {
        case OPEN:
            return _cmsgs.statusOpen();
        case IN_PROGRESS:
            return _cmsgs.statusInProgress();
        case PLAYER_CLOSED:
            return _cmsgs.statusPlayerClosed();
        case RESOLVED_CLOSED:
            return _cmsgs.statusResolvedClosed();
        case IGNORED_CLOSED:
            return _cmsgs.statusIgnoredClosed();
        case ESCALATED_LEAD:
            return _cmsgs.statusEscalatedLead();
        case ESCALATED_ADMIN:
            return _cmsgs.statusEscalatedAdmin();
        default:
            Console.log("Missing event status case", "status", status);
            return status.toString();
        }
    }

    public static ItemListBox<Event.Status> makeStatusListBox (Event.Status selection)
    {
        ItemListBox<Event.Status> statuses = ItemListBox.<Event.Status>builder()
            .add(Status.OPEN, _cmsgs.statusOpen())
            .add(Status.IN_PROGRESS, _cmsgs.statusInProgress())
            .add(Status.PLAYER_CLOSED, _cmsgs.statusPlayerClosed())
            .add(Status.RESOLVED_CLOSED, _cmsgs.statusResolvedClosed())
            .add(Status.IGNORED_CLOSED, _cmsgs.statusIgnoredClosed())
            .add(Status.ESCALATED_LEAD, _cmsgs.statusEscalatedLead())
            .add(Status.ESCALATED_ADMIN, _cmsgs.statusEscalatedAdmin())
            .build();
        statuses.setSelectedItem(selection);
        return statuses;
    }

    /**
     * Escapes all the HTML in a string and adds link tags wrapping anything that appeared to be a
     * URL. This handles only http/https protocol.
     */
    public static String linkify (String text)
    {
        String linkified = "";
        final Element div = DOM.createDiv();
        for (int pos = 0, next; pos < text.length(); pos = next) {
            next = Math.min(
                wrappedIndexOf(text, "http://", pos),
                wrappedIndexOf(text, "https://", pos));
            if (next == pos) {
                // ah, we have a link, use a native regex to extract it
                String url = findURL(text, pos);
                next = pos + url.length();
                linkified += "<a href=\"" + url + "\">" + url + "</a>";
            } else {
                // some non-link text to deal with, use the DOM to sanitize it
                DOM.setInnerText(div, text.substring(pos, next));
                linkified += DOM.getInnerHTML(div);
            }
        }
        return linkified;
    }

    /**
     * Returns str.indexOf(substr, pos) if substr was found, otherwise str.length().
     */
    protected static int wrappedIndexOf (String str, String substr, int pos)
    {
        pos = str.indexOf(substr, pos);
        return pos == -1 ? str.length() : pos;
    }

    /**
     * Encloses the given regular expression character class such that the result will match
     * anything not in the class, i.e. "[^chars]".
     */
    protected static final String notClass (String chars)
    {
        return "[^" + chars + "]";
    }

    /**
     * Uses a native regular expression to find a URL in the given text starting at the given
     * offset. Returns the matched text.
     */
    private static native String findURL (String text, int pos) /*-{
        var rex = new RegExp(@com.threerings.sling.gwt.client.SlingUtils::_jsLinkPat, "ig");
        rex.lastIndex = pos;
        return rex.exec(text)[0].toString();
    }-*/;

    protected static final ClientMessages _cmsgs = GWT.create(ClientMessages.class);
    protected static final List<ConstantsWithLookup> _serverErrorBundles = Lists.newArrayList();

    /** Javascript regex for something that looks like a link. We don't really care about it,
     * just generate correct HTML that will not glitch the agent's rendering. */
    protected static final String _jsLinkPat;

    static
    {
        String ILLEGAL = "<> \t\"',;\r\n";
        String ILLEGAL_OR_DOT = ILLEGAL + ".";
        _jsLinkPat = "http(s?)://" + notClass(ILLEGAL) + "*" +
            notClass(ILLEGAL_OR_DOT);

        ServerMessages smsgs = GWT.create(ServerMessages.class);
        _serverErrorBundles.add(smsgs);
    }
}
