//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Set;

import com.threerings.sling.gwt.util.Arguments;
import com.threerings.sling.gwt.util.Nav;
import com.threerings.sling.gwt.util.PageAddress;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;
import com.threerings.sling.web.data.Account;
import com.threerings.sling.web.client.SlingService;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventFilter;
import com.threerings.sling.web.data.EventSearch;
import com.threerings.sling.web.data.TimeRange;

/**
 * Static representations of all sling app navigation to internal sections and their pages.
 * Generally code can refer to the URLs of other parts of the sling application in the form of
 * {@link PageAddress} instances created using methods in this class. {@link Section}
 * implementations are then responsible for parsing arguments and generating a view of a given
 * {@code PageAddress}, normally taken from the {@code History.getToken()} value. The enumerations
 * in this class can be used for that parsing.
 */
public class SlingNav
{
    /**
     * Navigation related to the #accounts section.
     */
    public static class Accounts
    {
        public static final SectionId ID = new SectionId("accounts");

        public static enum Mode {
            SEARCH, POSTNOTE, RELATED, CREATE;
        }

        public static enum SearchBy {
            USERNAME, ACCOUNTNAME, EMAIL, GAMENAME, ID;
        }

        public static PageAddress find (SearchBy by, String query)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.SEARCH), Nav.toArg(by), query));
        }

        public static PageAddress findAccount (String accountName)
        {
            return find(SearchBy.ACCOUNTNAME, accountName);
        }

        public static PageAddress findCharacter (String characterName)
        {
            return find(SearchBy.GAMENAME, characterName);
        }

        public static PageAddress postNote (String accountName)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.POSTNOTE), accountName));
        }

        public static PageAddress findRelated (int accountId)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.RELATED),
                Integer.toString(accountId)));
        }

        public static PageAddress createAccount ()
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.CREATE)));
        }
    }

    /**
     * Navigation related to the #events section.
     */
    public static class Events
    {
        public static final SectionId ID = new SectionId("events");

        public static enum Mode {
            QUICKSEARCH, SEARCH, VIEW, POST_NOTE, POST_REPLY;
        }

        public static PageAddress quickSearch (SlingService.Events criterion, String query)
        {
            Arguments args = new Arguments(Nav.toArg(Mode.QUICKSEARCH), Nav.toArg(criterion));
            if (criterion == SlingService.Events.ACCOUNT) {
                if (query == null) {
                    query = "";
                }
                args.values.add(query);
            }
            return new PageAddress(ID, args);
        }

        public static PageAddress quickSearchOpen ()
        {
            return quickSearch(SlingService.Events.OPEN, null);
        }

        public static PageAddress quickSearchMy ()
        {
            return quickSearch(SlingService.Events.MY, null);
        }

        public static PageAddress quickSearchAll ()
        {
            return quickSearch(SlingService.Events.ALL, null);
        }

        public static PageAddress quickSearchAccount (String name)
        {
            return quickSearch(SlingService.Events.ACCOUNT, name);
        }

        public static PageAddress loadEvent (int id)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.VIEW), Integer.toString(id)));
        }

        public static PageAddress search ()
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.SEARCH)));
        }

        public static PageAddress search (EventSearch search)
        {
            Arguments args = new Arguments(Nav.toArg(Mode.SEARCH), Nav.toArg(search.sort));
            for (EventFilter filter : search.filters) {
                args.values.add(Nav.toArg(filter.type));
                args.values.add(filter.exposeRawQuery());
            }
            return new PageAddress(ID, args);
        }

        public static PageAddress postNote (int id)
        {
            return new PageAddress(ID, new Arguments(
                Nav.toArg(Mode.POST_NOTE), Integer.toString(id)));
        }

        public static PageAddress postReply (int id)
        {
            return new PageAddress(ID, new Arguments(
                Nav.toArg(Mode.POST_REPLY), Integer.toString(id)));
        }
    }

    /**
     * Navigation related to the #faq section.
     */
    public static class FAQ
    {
        public static final SectionId ID = new SectionId("faq");

        public static enum Mode {
            VIEW, EDIT, EDITQ;
        }

        public static PageAddress view ()
        {
            return new PageAddress(ID, new Arguments());
        }

        public static PageAddress edit ()
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.EDIT)));
        }

        public static PageAddress addQuestion ()
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.EDITQ)));
        }

        public static PageAddress editQuestion (int questionId)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.EDITQ),
                String.valueOf(questionId)));
        }
    }

    /**
     * Navigation related to the #requests section.
     */
    public static class Requests
    {
        public static SectionId ID = new SectionId("requests");

        public static enum Mode {
            VIEW, NEW;
        }

        public static PageAddress viewMine ()
        {
            return new PageAddress(ID, new Arguments()); // Mode.VIEW is the default
        }

        public static PageAddress addNew ()
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.NEW)));
        }
    }

    /**
     * Navigation related to the #reports section.
     */
    public static class Reports
    {
        public static SectionId ID = new SectionId("reports");

        public static enum Mode {
            SELECT, FIRST_RESPONSE, RECENT_VOLUME, AVERAGE_VOLUME, AGENT_ACTIVITY
        }

        public static PageAddress select ()
        {
            return new PageAddress(ID);
        }

        public static PageAddress blank (Mode mode)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(mode)));
        }

        public static PageAddress firstResponse (Event.Type type, TimeRange range, long millis)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.FIRST_RESPONSE),
                Nav.toArg(type), Nav.toArg(range), String.valueOf(millis)));
        }

        public static PageAddress recentVolume (SlingService.TimeUnit timeUnit, int count)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.RECENT_VOLUME),
                Nav.toArg(timeUnit), String.valueOf(count)));
        }

        public static PageAddress averageVolume (TimeRange range, Set<Event.Type> types)
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.AVERAGE_VOLUME),
                Nav.toArg(range), Nav.toArg(types)));
        }

        public static PageAddress agentActivity ()
        {
            return new PageAddress(ID, new Arguments(Nav.toArg(Mode.AGENT_ACTIVITY)));
        }
    }
}
