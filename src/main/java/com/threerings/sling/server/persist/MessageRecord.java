//
// Sling - Copyright 2012 Three Rings Design, Inc.

//
// $Id: MessageRecord.java 546 2011-06-22 03:25:20Z jamie $

package com.threerings.sling.server.persist;

import java.sql.Timestamp;
import java.util.Map;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.Message;
import com.threerings.sling.web.data.UniversalTime;

/**
 * Represents a single message associated with a support event.
 */
@Entity
public class MessageRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MessageRecord> _R = MessageRecord.class;
    public static final ColumnExp<Integer> EVENT_ID = colexp(_R, "eventId");
    public static final ColumnExp<Timestamp> ENTERED = colexp(_R, "entered");
    public static final ColumnExp<String> AUTHOR = colexp(_R, "author");
    public static final ColumnExp<String> TEXT = colexp(_R, "text");
    public static final ColumnExp<Message.Access> ACCESS = colexp(_R, "access");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you make a change to this class that must be propagated to its
     * database representation. */
    public static final int SCHEMA_VERSION = 3;

    /** The event to which this message is associated. */
    @Index public int eventId;

    /** The time at which this message was recorded. */
    public Timestamp entered;

    /** The account of the author of this message. */
    public String author;

    /** The text of the message. */
    @Column(length=65535)
    public String text;

    /** The access control of this message. */
    @Column(defaultValue="'NORMAL'")
    public Message.Access access;

    /**
     * Converts this message record to an over the wire object.
     */
    public Message toMessage (Map<String,AccountName> accounts)
    {
        Message msg = new Message();
        msg.authored = UniversalTime.fromDate(entered);
        msg.body = text;
        msg.author = accounts.get(author);
        msg.access = access;
        return msg;
    }
}
