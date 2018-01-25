//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a single message in a conversation associated with a particular support event.
 */
public class Message
    implements IsSerializable
{
    public enum Access implements IsSerializable
    {
        NORMAL, SUPPORT
    };

    /** The date on which this message was authored (UTC). */
    public long authored;

    /** The author of this message. */
    public AccountName author;

    /** The body of the message. */
    public String body;

    /** Access control for this message. */
    public Access access = Access.NORMAL;
}
