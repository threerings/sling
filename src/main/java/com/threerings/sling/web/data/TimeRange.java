//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Some useful code for elegantly dealing with time ranges.
 */
public class TimeRange implements IsSerializable
{
    public long from;
    public long to;

    public TimeRange ()
    {
    }

    public TimeRange (long from, long to)
    {
        if (from > to) {
            this.from = to;
            this.to = from;
        } else {
            this.from = from;
            this.to = to;
        }
    }
}
