//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.sql.Timestamp;
import java.util.Map;

import com.google.common.collect.Lists;

import com.samskivert.util.Tuple;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.annotation.*;
import com.samskivert.depot.clause.OrderBy.Order;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.Event.Type;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.UniversalTime;
import com.threerings.sling.web.data.UserPetition;

/**
 * Defines the basic data associated with all support events.
 */
@Entity(indices={
    @Index(name="ixLowerOwner"),
    @Index(name="ixLowerSource"),
    @Index(name="ixLowerSourceHandle"),
    @Index(name="ixLowerTarget"),
    @Index(name="ixLowerTargetHandle")})
public class EventRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EventRecord> _R = EventRecord.class;
    public static final ColumnExp<Integer> EVENT_ID = colexp(_R, "eventId");
    public static final ColumnExp<Event.Type> TYPE = colexp(_R, "type");
    public static final ColumnExp<Timestamp> ENTERED = colexp(_R, "entered");
    public static final ColumnExp<Timestamp> LAST_UPDATED = colexp(_R, "lastUpdated");
    public static final ColumnExp<Long> FIRST_RESPONSE = colexp(_R, "firstResponse");
    public static final ColumnExp<String> SOURCE = colexp(_R, "source");
    public static final ColumnExp<String> SOURCE_HANDLE = colexp(_R, "sourceHandle");
    public static final ColumnExp<String> SOURCE_IP_ADDRESS = colexp(_R, "sourceIpAddress");
    public static final ColumnExp<String> SOURCE_MACHINE_IDENT = colexp(_R, "sourceMachineIdent");
    public static final ColumnExp<String> TARGET = colexp(_R, "target");
    public static final ColumnExp<String> TARGET_HANDLE = colexp(_R, "targetHandle");
    public static final ColumnExp<String> TARGET_IP_ADDRESS = colexp(_R, "targetIpAddress");
    public static final ColumnExp<String> TARGET_MACHINE_IDENT = colexp(_R, "targetMachineIdent");
    public static final ColumnExp<String> OWNER = colexp(_R, "owner");
    public static final ColumnExp<Event.Status> STATUS = colexp(_R, "status");
    public static final ColumnExp<Boolean> WAITING_FOR_PLAYER = colexp(_R, "waitingForPlayer");
    public static final ColumnExp<String> SUBJECT = colexp(_R, "subject");
    public static final ColumnExp<String> CHAT_HISTORY = colexp(_R, "chatHistory");
    public static final ColumnExp<String> LINK = colexp(_R, "link");
    public static final ColumnExp<String> LANGUAGE = colexp(_R, "language");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you make a change to this class that must be propagated to its
     * database representation. */
    public static final int SCHEMA_VERSION = 9;

    /** A unique identifier for this record. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int eventId;

    /** The type of this event. */
    @Column(nullable=true)
    public Type type;

    /** The time at which this event was entered into the system. */
    public Timestamp entered;

    /** The time at which this event was last updated. */
    public Timestamp lastUpdated;

    /** The number of milliseconds after entering at which the first support qualifying action was
     * taken, or null if no such action has been taken. Qualifying actions are:
     * <ul><li>For petitions, the posting of a reply (user-accessible message)</li>
     * <li>For complaints, the posting of any message or the closure of the event</li></ul> */
    @Column(nullable = true)
    public Long firstResponse;

    /** The account for or by which this record was generated. */
    public String source;

    /** The handle of the source account at the time this record was generated. */
    @Column(nullable=true)
    public String sourceHandle;

    /** The ip address of the source at the time the event was generated, if any. */
    @Column(nullable=true)
    public String sourceIpAddress;

    /** The machine identifier of the source at the time the event was generated, if any. */
    @Column(nullable=true)
    public String sourceMachineIdent;

    /** The account against which this record was filed. Only valid for complaints. */
    @Column(nullable=true)
    public String target;

    /** The handle for the target account at the time this record was generated.
     * Only valid for complaints.*/
    @Column(nullable=true)
    public String targetHandle;

    /** The ip address of the target at the time the event was generated, if any. */
    @Column(nullable=true)
    public String targetIpAddress;

    /** The machine identifier of the target at the time the event was generated, if any. */
    @Column(nullable=true)
    public String targetMachineIdent;

    /** The account of the support personnel that owns this event. */
    @Column(nullable=true)
    public String owner;

    /** Indicates the status of this event. */
    public Event.Status status;

    /** Indicates agents are waiting for the player to respond. */
    public boolean waitingForPlayer;

    /** The user provided subject for this event. */
    public String subject;

    /** The chat history associated with this event, if any. */
    @Column(length=Event.CHAT_HISTORY_LENGTH)
    public String chatHistory;

    /** Optional link information for the event. */
    @Column(nullable=true)
    public String link;

    /** The optional language of the event, usually copied from the source's preferences. */
    @Column(nullable=true, length=2)
    public String language;

    public Event toEvent (Map<String, AccountName> accounts)
    {
        Event event = new Event();
        event.eventId = eventId;
        event.type = type;
        event.entered = UniversalTime.fromDate(entered);
        event.source = new Event.Participant(sourceHandle != null ?
            new AccountName(source, sourceHandle) : accounts.get(source), sourceIpAddress,
            sourceMachineIdent);
        AccountName targetAccountName = targetHandle != null ?
            new AccountName(target, targetHandle) : accounts.get(target);
        if (targetAccountName != null) {
            event.target = new Event.Participant(
                targetAccountName, targetIpAddress, targetMachineIdent);
        }
        event.owner = accounts.get(owner);
        event.status = status;
        event.waitingForPlayer = waitingForPlayer;
        event.subject = subject;
        event.chatHistory = chatHistory;
        event.lastUpdated = UniversalTime.fromDate(lastUpdated);
        event.firstResponse = firstResponse;
        event.link = link;
        event.language = language;
        return event;
    }

    public UserPetition toUserPetition ()
    {
        if (target != null) {
            throw new IllegalStateException("Requested to convert non-petition to UserPetition.");
        }

        UserPetition up = new UserPetition();
        up.eventId = eventId;
        up.entered = UniversalTime.fromDate(entered);
        up.status = status;
        up.subject = subject;
        up.messages = Lists.newArrayList();
        return up;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EventRecord}
     * with the supplied key values.
     */
    public static Key<EventRecord> getKey (int eventId)
    {
        return newKey(_R, eventId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(EVENT_ID); }
    // AUTO-GENERATED: METHODS END

    /**
     * Defines the index on {@link #owner} converted to lower case.
     */
    public static Tuple<SQLExpression<?>, Order> ixLowerOwner ()
    {
        return ix(EventRecord.OWNER);
    }

    /**
     * Defines the index on {@link #source} converted to lower case.
     */
    public static Tuple<SQLExpression<?>, Order> ixLowerSource ()
    {
        return ix(EventRecord.SOURCE);
    }

    /**
     * Defines the index on {@link #sourceHandle} converted to lower case.
     */
    public static Tuple<SQLExpression<?>, Order> ixLowerSourceHandle ()
    {
        return ix(EventRecord.SOURCE_HANDLE);
    }

    /**
     * Defines the index on {@link #target} converted to lower case.
     */
    public static Tuple<SQLExpression<?>, Order> ixLowerTarget ()
    {
        return ix(EventRecord.TARGET);
    }

    /**
     * Defines the index on {@link #targetHandle} converted to lower case.
     */
    public static Tuple<SQLExpression<?>, Order> ixLowerTargetHandle ()
    {
        return ix(EventRecord.TARGET_HANDLE);
    }

    protected static Tuple<SQLExpression<?>, Order> ix (ColumnExp<String> col)
    {
        return new Tuple<SQLExpression<?>, Order>(StringFuncs.lower(col), Order.ASC);
    }
}
