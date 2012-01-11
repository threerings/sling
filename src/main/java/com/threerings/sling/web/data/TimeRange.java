//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Some useful code for elegantly dealing with time ranges.
 */
public class TimeRange implements IsSerializable
{
    public UniversalTime from;
    public UniversalTime to;

    public TimeRange ()
    {
    }

    public TimeRange (long from, long to)
    {
        this(UniversalTime.fromMillis(from), UniversalTime.fromMillis(to));
    }

    public TimeRange (UniversalTime from, UniversalTime to)
    {
        if (from.after(to)) {
            this.from = to;
            this.to = from;
        } else {
            this.from = from;
            this.to = to;
        }
    }
}
