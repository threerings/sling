//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import com.google.common.primitives.Longs;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnum;

/**
 * Contains client displayable metadata for a support event.
 */
public class Event
    implements IsSerializable, Comparable<Event>
{
    /**
     * Types of events. Caution: these are referenced by name in the database.
     */
    public enum Type
    {
        SUPPORT_ACTION, NOTE, PETITION, COMPLAINT
    }

    /**
     * Enumeration of event status values. Caution: the instances are referenced by name in the
     * client.
     * <p>NOTE: inheritance from ByteEnum is legacy, avoiding a complex schema change.</p>
     */
    public enum Status implements ByteEnum
    {
        OPEN(1), IN_PROGRESS(2), PLAYER_CLOSED(3), RESOLVED_CLOSED(4), IGNORED_CLOSED(5),
        ESCALATED_LEAD(6), ESCALATED_ADMIN(7);

        public final byte byteVal;

        @Override public byte toByte ()
        {
            return byteVal;
        }

        /**
         * Returns true if this status is one associated with an open event.
         */
        public boolean isOpen ()
        {
            return (this == OPEN || this == IN_PROGRESS ||
                    this == ESCALATED_LEAD || this == ESCALATED_ADMIN);
        }

        Status (int value)
        {
            this.byteVal = (byte)value;
        }
    }

    /**
     * Account and identity info for the source or target of an Event.
     */
    public static class Participant
        implements IsSerializable
    {
        /** The accout name of this participant. */
        public AccountName name;

        /** The ip address in use by the participant at the time of this event, if available. */
        public String ipAddress;

        /** The machine identity of the participant at the time of this event, if available. */
        public String machineIdent;

        /** Creates a new empty participant. */
        public Participant()
        {
        }

        /** Creates a participant populated with the given values. */
        public Participant (AccountName name, String ipAddress, String machineIdent)
        {
            this.name = name;
            this.ipAddress = ipAddress;
            this.machineIdent = machineIdent;
        }
    }

    /** Database-enforced maximum length of the chat history field. */
    public static final int CHAT_HISTORY_LENGTH = 65535;

    /** A unique identifier for this event. */
    public int eventId;

    /** The type of this event. */
    public Type type;

    /** The time at which this event was entered into the system (UTC). */
    public long entered;

    /** The time at which this event was last updated (UTC). */
    public long lastUpdated;

    /** The number of milliseconds after entering at which the first support qualifying action was
     * taken, or null if no such action has been taken. Qualifying actions are:
     * <ul><li>For petitions, the posting of a reply (user-accessible message)</li>
     * <li>For complaints, the posting of any message or the closure of the event</li></ul> */
    public Long firstResponse;

    /** The participant for or by which this record was generated. */
    public Participant source;

    /** The participant against which this record was generated. Only valid for complaints. */
    public Participant target;

    /** The account of the support personnel that owns this event. */
    public AccountName owner;

    /** Indicates the type-specific status of this event. */
    public Status status;

    /** Indicates agents are waiting for the player to respond. */
    public boolean waitingForPlayer;

    /** The user provided subject for this event. */
    public String subject;

    /** The chat history associated with this event, if any. */
    public String chatHistory;

    /** The (optional) link associated with this event. */
    public String link;

    /** The (optional) language of the event, usually copied from the source's preferences. */
    public String language;

    /**
     * Returns a string used to style the status display of this event.
     */
    public String getStatusStyle ()
    {
        switch (status) {
        case IN_PROGRESS:
            return "claimed";
        case PLAYER_CLOSED:
        case RESOLVED_CLOSED:
        case IGNORED_CLOSED:
            return "closed";
        default:
            return "unclaimed";
        }
    }

    /**
     * Returns a string used to the style the type display of this event.
     */
    public String getTypeStyle ()
    {
        switch (type) {
        case SUPPORT_ACTION:
            return "support";
        case NOTE:
            return "note";
        case PETITION:
            return "petition";
        case COMPLAINT:
        default:
            return "default";
        }
    }

    // from interface Comparable
    public int compareTo (Event oevent)
    {
        if (oevent.status != status) {
            return status.compareTo(oevent.status);
        } else {
            return Longs.compare(oevent.entered, entered);
        }
    }
}
