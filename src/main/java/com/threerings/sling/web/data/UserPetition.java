//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains only the information needed to disply a petition to a user.
 */
public class UserPetition
    implements IsSerializable
{
    /** A unique identifier for this petition. */
    public int eventId;

    /** The time at which this petition was entered into the system (UTC). */
    public long entered;

    /** Indicates the status of this petition. */
    public Event.Status status;

    /** The user provided subject for this petition. */
    public String subject;

    /** Messages recorded for this petition. */
    public List<Message> messages;
}
