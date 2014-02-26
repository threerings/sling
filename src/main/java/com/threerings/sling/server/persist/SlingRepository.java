//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DateFuncs;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Funcs;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.util.Tuple3;

import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventFilter;
import com.threerings.sling.web.data.EventSearch;
import com.threerings.sling.web.data.TimeRange;

/**
 * Manages various support persistent data.
 */
@Singleton
public class SlingRepository extends DepotRepository
{
    /**
     * Defines a repo operation for a paged result. Provides the total count and the slice in
     * separate calls for efficiency.
     */
    public interface PagedQuery<T>
    {
        /** Counts the total number of results. */
        int count();

        /** Loads a page of results. */
        List<T> load(int offset, int count);
    }

    @Inject public SlingRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads and returns the specified event record. Returns null if no such event exists.
     */
    public EventRecord loadEvent (int eventId)
    {
        return load(EventRecord.getKey(eventId));
    }

    /**
     * Loads currently open support records that are alo not waiting for a player response.
     */
    public PagedQuery<EventRecord> loadOpenEvents ()
    {
        return new BasicEventQuery(new Where(Ops.and(STATUS_OPEN, NOT_WAITING)));
    }

    /**
     * Loads support records that were claimed by the given owner.
     */
    public PagedQuery<EventRecord> loadClaimedEvents (String owner)
    {
        return new BasicEventQuery(new Where(lowerEq(EventRecord.OWNER, owner)));
    }

    /**
     * Loads a page of support records.
     */
    public PagedQuery<EventRecord> loadAllEvents ()
    {
        return new BasicEventQuery();
    }

    /**
     * Loads support events matching the supplied account as source or target.
     */
    public PagedQuery<EventRecord> loadEvents (String account)
    {
        return new BasicEventQuery(new Where(Ops.or(
            lowerEq(EventRecord.SOURCE, account),
            lowerEq(EventRecord.TARGET, account)))).sort(EventRecord.LAST_UPDATED);
    }

    /**
     * Loads a paged query of events matching a structured set of conditions.
     */
    public PagedQuery<EventRecord> searchEvents (EventSearch search)
    {
        boolean joinMessages = false;

        List<SQLExpression<?>> primaries = Lists.newArrayList();
        for (EventFilter filter : search.filters) {
            switch (filter.type) {
            case OWNER_IS:
                primaries.add(lowerEq(EventRecord.OWNER, filter.getOwner()));
                break;
            case OWNER_ID_IS:
                throw new IllegalArgumentException(); // caller has to convert these
            case CHAT_HISTORY_MATCHES:
                primaries.add(termSearch(EventRecord.CHAT_HISTORY, filter.getSearchTerms()));
                break;
            case SUBJECT_MATCHES:
                primaries.add(termSearch(EventRecord.SUBJECT, filter.getSearchTerms()));
                break;
            case ACCOUNT_NAME_IS:
                primaries.add(Ops.or(
                    lowerEq(EventRecord.SOURCE, filter.getAccountName()),
                    lowerEq(EventRecord.TARGET, filter.getAccountName())));
                break;
            case GAME_NAME_IS:
                primaries.add(Ops.or(
                    lowerEq(EventRecord.SOURCE_HANDLE, filter.getGameName()),
                    lowerEq(EventRecord.TARGET_HANDLE, filter.getGameName())));
                break;
            case NOTE_MATCHES:
                primaries.add(termSearch(MessageRecord.TEXT, filter.getSearchTerms()));
                joinMessages = true;
                break;
            case FIRST_RESPONSE_IS_MORE_THAN:
                primaries.add(Ops.or(
                    EventRecord.FIRST_RESPONSE.isNull(),
                    EventRecord.FIRST_RESPONSE.greaterThan(filter.getMillis())));
                break;
            case HAS_NOTE:
                joinMessages = true;
                break;
            case CREATED_BETWEEN:
            case UPDATED_BETWEEN:
                ColumnExp<Timestamp> dcol = DATE_COLS.get(filter.type);
                TimeRange range = filter.getTimeRange();
                primaries.add(Ops.and(
                    dcol.greaterEq(new Timestamp(range.from)),
                    dcol.lessThan(new Timestamp(range.to))));
                break;
            case STATUS_IS:
                primaries.add(EventRecord.STATUS.eq(filter.getEventStatus()));
                break;
            case IP_ADDRESS_IS:
                primaries.add(Ops.or(
                    EventRecord.SOURCE_IP_ADDRESS.like("%/" + filter.getIpAddress()),
                    EventRecord.TARGET_IP_ADDRESS.like("%/" + filter.getIpAddress())));
                break;
            case MACHINE_IDENT_IS:
                primaries.add(Ops.or(
                    EventRecord.SOURCE_MACHINE_IDENT.eq(filter.getMachineIdent()),
                    EventRecord.TARGET_MACHINE_IDENT.eq(filter.getMachineIdent())));
                break;
            case TYPE_IS:
                primaries.add(EventRecord.TYPE.eq(filter.getEventType()));
                break;
            case TYPE_IS_IN:
                primaries.add(EventRecord.TYPE.in(filter.getEventTypes()));
                break;
            case WAITING_FOR_PLAYER:
                primaries.add(EventRecord.WAITING_FOR_PLAYER.eq(filter.getBoolean()));
                break;
            case LANGUAGE_IS:
                if (filter.getLanguage().equals("")) {
                    primaries.add(Ops.or(
                        EventRecord.LANGUAGE.isNull(), EventRecord.LANGUAGE.eq("")));
                } else {
                    primaries.add(EventRecord.LANGUAGE.eq(filter.getLanguage()));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown search filter type: " + filter);
            }
        }

        Where where = new Where(Ops.and(primaries));
        BasicEventQuery query = joinMessages ?
            new EventJoinMessagesQuery(where) : new BasicEventQuery(where);

        switch (search.sort) {
        case CREATION:
            query.sort(EventRecord.ENTERED);
            break;
        default:
            throw new IllegalArgumentException("Unknown sort type " + search.sort);
        }

        return query;
    }

    /**
     * Returns a 7x24 array of all events entered during each hour on each day of the week, limited
     * to events reported in the given time range.
     */
    public int[][] getVolumeByDayAndHour (
        long from, long to, Collection<Event.Type> types)
    {
        int[][] volumes = new int[7][];
        for (int ii = 0; ii < volumes.length; ++ii) {
            volumes[ii] = new int[24];
        }

        SQLExpression<Number> dayOfWeek = DateFuncs.dayOfWeek(EventRecord.ENTERED);
        SQLExpression<Number> hour = DateFuncs.hour(EventRecord.ENTERED);
        Where where = new Where(Ops.and(
            EventRecord.ENTERED.greaterEq(new Timestamp(from)),
            EventRecord.ENTERED.lessThan(new Timestamp(to)),
            EventRecord.TYPE.in(types)));

        for (Tuple3<Number, Number, Number> tup : from(EventRecord.class).where(where)
            .groupBy(dayOfWeek, hour).select(dayOfWeek, hour, Funcs.countStar())) {
            volumes[tup.a.intValue()][tup.b.intValue()] += tup.c.intValue();
        }

        return volumes;
    }

    /**
     * Loads all petition events matching the supplied source account.
     */
    public Collection<EventRecord> loadPetitions (String account)
    {
        // continue to support legacy events that have target == null to denote petitions (there
        // was previously no type field)
        SQLExpression<Boolean> isPetition = Ops.or(
            Ops.and(EventRecord.TYPE.notNull(), EventRecord.TYPE.eq(Event.Type.PETITION)),
            Ops.and(EventRecord.TYPE.isNull(), EventRecord.TARGET.isNull()));

        return findAll(EventRecord.class,new Where(Ops.and(
            lowerEq(EventRecord.SOURCE, account), isPetition)),
            OrderBy.descending(EventRecord.LAST_UPDATED));
    }

    /**
     * Loads all messages for the supplied set of events.
     */
    public Collection<MessageRecord> loadMessages (Collection<Integer> eventIds)
    {
        return findAll(MessageRecord.class, new Where(MessageRecord.EVENT_ID.in(eventIds)),
                       OrderBy.descending(MessageRecord.ENTERED));
    }

    /**
     * Registers a new support record with the system. The {@link EventRecord#eventId}, {@link
     * EventRecord#entered} and {@link EventRecord#lastUpdated} fields will be filled in by this
     * method.
     */
    public void insertEvent (EventRecord record)
    {
        record.entered = new Timestamp(System.currentTimeMillis());
        record.lastUpdated = record.entered;
        insert(record);
    }

    /**
     * Updates the specified fields of the supplied event record.
     */
    public void updateEvent (int eventId, Event.Status newStatus, String newOwner)
    {
        updatePartial(EventRecord.getKey(eventId),
            EventRecord.STATUS, newStatus,
            EventRecord.OWNER, newOwner);
    }

    /**
     * Sets whether the event with the given id is waiting for a player response.
     */
    public void setWaitingForPlayer (int eventId, boolean waiting)
    {
        updatePartial(EventRecord.getKey(eventId),
            EventRecord.WAITING_FOR_PLAYER, waiting);
    }

    /**
     * Sets the language of an event.
     */
    public void setLanguage (int eventId, String language)
    {
        updatePartial(EventRecord.getKey(eventId), EventRecord.LANGUAGE, language);
    }

    /**
     * Records the supplied message and optionally updates the event's
     * {@link EventRecord#lastUpdated} field. The {@link MessageRecord#entered} field will be
     * filled in by this method.
     */
    public void insertMessage (MessageRecord record, boolean touchEvent)
    {
        record.entered = new Timestamp(System.currentTimeMillis());
        insert(record);
        if (touchEvent) {
            // update the last modified time of the associated event
            updatePartial(EventRecord.getKey(record.eventId),
                          ImmutableMap.of(EventRecord.LAST_UPDATED, DateFuncs.now()));
        }
    }

    /**
     * Writes the {@link EventRecord#firstResponse} field of the supplied record to the database.
     */
    public void updateFirstResponse (EventRecord event)
    {
        updatePartial(EventRecord.getKey(event.eventId),
            EventRecord.FIRST_RESPONSE, event.firstResponse);
    }

    /**
     * Loads all of the FAQ categories.
     */
    public Collection<CategoryRecord> loadCategories ()
    {
        return findAll(CategoryRecord.class);
    }

    /**
     * Stores the supplied category record in the database, updating it if it already exists,
     * inserting it if not.
     */
    public void storeCategory (CategoryRecord category)
    {
        if (category.categoryId == 0) {
            insert(category);
        } else {
            update(category);
        }
    }

    /**
     * Deletes the supplied category from the database.
     */
    public void deleteCategory (CategoryRecord category)
    {
        delete(category);
        deleteAll(QuestionRecord.class,
                  new Where(QuestionRecord.CATEGORY_ID, category.categoryId));
        /* TODO: invalidate cache */
    }

    /**
     * Loads all of the FAQ questions.
     */
    public Collection<QuestionRecord> loadQuestions ()
    {
        return findAll(QuestionRecord.class);
    }

    /**
     * Stores the supplied question record in the database, updating it if it already exists,
     * inserting it if not.
     */
    public void storeQuestion (QuestionRecord question)
    {
        if (question.questionId == 0) {
            insert(question);
        } else {
            update(question);
        }
    }

    /**
     * Deletes the supplied question from the database.
     */
    public void deleteQuestion (QuestionRecord question)
    {
        delete(question);
    }

    /**
     * Inserts or updates the {@link AgentActivityRecord} corresponding to the given account name
     * to the current time. Returns the time inserted.
     */
    public long noteAgentActivity (String accountName)
    {
        AgentActivityRecord rec = new AgentActivityRecord();
        rec.accountName = accountName;
        long now = System.currentTimeMillis();
        rec.time = new Timestamp(now);
        store(rec);
        return now;
    }

    /**
     * Records a support action event for when a user is automatically banned, usually during
     * authentication.
     */
    public void noteAutoBan (String username, String reason)
    {
        EventRecord event = new EventRecord();
        event.source = username;
        event.chatHistory = "";
        event.status = Event.Status.RESOLVED_CLOSED;
        event.subject = "AUTO-BAN: " + reason;
        event.type = Event.Type.SUPPORT_ACTION;
        insertEvent(event);
    }

    /**
     * Gets a map of all agent activity time stamps.
     */
    public Map<String, Long> getAgentActivity()
    {
        Map<String, Long> activity = Maps.newHashMap();
        for (AgentActivityRecord rec : findAll(AgentActivityRecord.class)) {
            activity.put(rec.accountName, rec.time.getTime());
        }
        return activity;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CategoryRecord.class);
        classes.add(MessageRecord.class);
        classes.add(EventRecord.class);
        classes.add(QuestionRecord.class);
        classes.add(AgentActivityRecord.class);
    }

    protected static SQLExpression<?> looseMatch (FluentExp<String> exp, String term)
    {
        term = term.toLowerCase();
        if (term.charAt(0) == '"') {
            return StringFuncs.lower(exp).like("%" + term.substring(1, term.length() - 1) + "%");
        } else {
            if (!term.startsWith("*")) {
                term = "*" + term;
            }
            if (!term.endsWith("*")) {
                term = term + "*";
            }
            return StringFuncs.lower(exp).like(term.replace('*', '%'));
        }
    }

    protected static SQLExpression<?> termSearch (final FluentExp<String> exp, String query)
    {
        List<SQLExpression<?>> exps = Lists.transform(Splitter.split(query),
                new Function<String, SQLExpression<?>>() {
            @Override public SQLExpression<?> apply (String input) {
                return looseMatch(exp, input);
            }
        });

        if (exps.size() == 1) {
            return exps.get(0);
        }
        return Ops.and(exps);
    }

    protected static class Splitter
    {
        public StringBuilder buff = new StringBuilder();
        public List<String> terms = Lists.newArrayList();

        public static List<String> split (String query)
        {
            boolean inQuote = false;
            Splitter s = new Splitter();
            for (int pos = 0, len = query.length(); pos <= len; ++pos) {
                char c = pos < len ? query.charAt(pos) : ' ';
                if (c == '"') {
                    if (inQuote) {
                        inQuote = false;
                        s.appendAndPush(c);
                    } else {
                        inQuote = true;
                        s.append(c);
                    }
                } else if (c == ' ') {
                    if (inQuote) {
                        s.append(c);
                    } else {
                        s.push();
                    }
                } else {
                    s.append(c);
                }
            }
            return s.terms;
        }

        public void append (char c)
        {
            buff.append(c);
        }

        public void appendAndPush (char c)
        {
            buff.append(c);
            push();
        }

        public void push ()
        {
            if (buff.length() == 0) {
                return;
            }
            terms.add(buff.toString());
            buff.setLength(0);
        }
    }

    protected class BasicEventQuery implements PagedQuery<EventRecord>
    {
        BasicEventQuery ()
        {
        }

        BasicEventQuery (Where where)
        {
            _clauses.add(where);
        }

        BasicEventQuery sort (ColumnExp<?> exp)
        {
            _sort = exp;
            return this;
        }

        @Override public int count ()
        {
            QueryClause[] aclauses = _clauses.toArray(new QueryClause[_clauses.size() + 1]);
            aclauses[aclauses.length - 1] = new FromOverride(EventRecord.class);
            return SlingRepository.this.load(CountRecord.class, aclauses).count;
        }

        @Override public List<EventRecord> load (int offset, int count)
        {
            List<QueryClause> clauses = Lists.newArrayList(_clauses);
            clauses.add(OrderBy.descending(_sort));
            clauses.add(new Limit(offset, count));
            return findAll(EventRecord.class, _cache, clauses);
        }

        protected List<QueryClause> _clauses = Lists.newArrayList();
        protected ColumnExp<?> _sort = EventRecord.ENTERED;
        protected CacheStrategy _cache = CacheStrategy.BEST;
    }

    protected class EventJoinMessagesQuery extends BasicEventQuery
    {
        public EventJoinMessagesQuery (Where where)
        {
            super(where);

            _clauses.add(new Join(MessageRecord.class,
                EventRecord.EVENT_ID.eq(MessageRecord.EVENT_ID)));

            // TODO: support select distinct foo.id in depot, use that instead of GroupBy
            _clauses.add(new GroupBy(_ctx.getMarshaller(EventRecord.class).getSelections()));
            _cache = CacheStrategy.CONTENTS; // group by can't handle key caching
        }

        @Override public int count ()
        {
            // TODO: support select count(distinct "foo".id) in depot and rejig so a regular
            // load call can be used
            List<QueryClause> clauses = Lists.newArrayList(_clauses);
            clauses.add(new FromOverride(EventRecord.class));
            int count = 0;
            for (CountRecord rec : findAll(CountRecord.class, clauses)) {
                count += rec.count;
            }
            return count;
        }
    }

    protected FluentExp<Boolean> lowerEq (FluentExp<String> col, String value)
    {
        value = value == null ? null : value.toLowerCase();
        return StringFuncs.lower(col).eq(value);
    }

    protected static final SQLExpression<Boolean> STATUS_OPEN = Ops.not(Ops.or(
        EventRecord.STATUS.eq(Event.Status.PLAYER_CLOSED),
        EventRecord.STATUS.eq(Event.Status.RESOLVED_CLOSED),
        EventRecord.STATUS.eq(Event.Status.IGNORED_CLOSED)));
    protected static final SQLExpression<Boolean> NOT_WAITING =
        EventRecord.WAITING_FOR_PLAYER.eq(false);
    protected static final Map<EventFilter.Type, ColumnExp<Timestamp>> DATE_COLS = ImmutableMap.of(
        EventFilter.Type.CREATED_BETWEEN, EventRecord.ENTERED,
        EventFilter.Type.UPDATED_BETWEEN, EventRecord.LAST_UPDATED);
}
