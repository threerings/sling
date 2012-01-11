//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Encapsulates an immutable point in time in the unchanging universal coordinated time (UTC).
 * Normally the sling server generates UniversalTime instances and the client uses ServerTime
 * instances as necessary to emulate the server's time zone.
 */
public class UniversalTime
    implements IsSerializable
{
    /**
     * Creates a new time representing the given milliseconds since the epoch, UTC.
     */
    public static UniversalTime fromMillis (long milliseconds)
    {
        return new UniversalTime(milliseconds);
    }

    /**
     * Creates a new time from the given date object's milliseconds since the epoch, UTC.
     * Returns null if date is null.
     */
    public static UniversalTime fromDate (Date date)
    {
        return date == null ? null : fromMillis(date.getTime());
    }

    /**
     * Returns a time representing the current millisecond since the epoch in UTC.
     */
    public static UniversalTime now ()
    {
        return new UniversalTime(System.currentTimeMillis());
    }

    /**
     * Compares this time to another one, return -1 if this time is before the other one, 0 if
     * they are the same and 1 if this is after the other one.
     */
    public int compareTo (UniversalTime o)
    {
        return _millis < o._millis ? -1 : (_millis > o._millis ? 1 : 0);
    }

    /**
     * Returns the milliseconds elapsed since the epoch to reach this time.
     */
    public long getTime ()
    {
        return _millis;
    }

    /**
     * Returns true if this time is before the given one.
     */
    public boolean before (UniversalTime o)
    {
        return _millis < o._millis;
    }

    /**
     * Returns true if this time is after the given one.
     */
    public boolean after (UniversalTime o)
    {
        return _millis > o._millis;
    }

    /**
     * Returns a new time the given number of days further in the future than this one.
     */
    public UniversalTime addDays (int days)
    {
        return addHours(days * 24);
    }

    /**
     * Returns a new time the given number of hours further in the future than this one.
     */
    public UniversalTime addHours (int hours)
    {
        return addMillis(hours * 60 * 60 * 1000L);
    }

    /**
     * Returns a new time the given number of milliseconds further in the future than this one.
     */
    public UniversalTime addMillis (long millis)
    {
        return fromMillis(_millis + millis);
    }

    // for deserialization
    private UniversalTime ()
    {
    }

    private UniversalTime (long millis)
    {
        _millis = millis;
    }

    private long _millis;
}
