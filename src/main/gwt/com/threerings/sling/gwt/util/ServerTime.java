//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import java.util.Date;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;

import com.threerings.gwt.util.Console;
import com.threerings.gwt.util.DateUtil;

/**
 * Represents a time value that is offset by the server's time zone. All regular accessors,
 * methods and factories work with values in the server's time zone.
 */
public class ServerTime
{
    /**
     * Converts a universal time to a server time.
     */
    public static ServerTime from (long time)
    {
        return new ServerTime(time, TIME_ZONE.getOffset(time));
    }

    /**
     * Creates a new server time instance representing the given point in time on the server.
     */
    @SuppressWarnings(value = "deprecation")
    public static ServerTime fromFields (int year, int mo, int day, int hrs, int min, int sec)
    {
        return new ServerTime(Date.UTC(year - 1900, mo, day, hrs, min, sec), 0);
    }

    /**
     * Creates a new server time instance representing the server's current time. The accuracy
     * is limited to the accuracy of the user's local clock.
     */
    public static ServerTime now ()
    {
        return from(System.currentTimeMillis());
    }

    /**
     * Parses a user input date or time according to the given format and interpreted as being in
     * the server time zone.
     */
    public static ServerTime parse (DateTimeFormat fmt, String input)
    {
        // parsing a string to a date returns a local time representation
        Date local = fmt.parse(input);

        // convert 2 digit date to 4
        local = bumpYear(local);

        // take away the time zone offset
        long time = local.getTime();
        time -= getLocalTzOffsetMinutes(time) * MINUTE_MILLIS;

        // now we should have a UTC time
        return dbgLog("ServerTime.parse", new ServerTime(time, 0), "input", input);
    }

    /**
     * If enabled, logs various and sundry aspects of a time to the console, with optional extras.
     */
    public static ServerTime dbgLog (String debug, ServerTime time, Object ...more)
    {
        boolean log = false;
        if (!log || time == null) {
            return time;
        }
        long millis = time.getTime();
        Object[] args = new Object[] {"time", time.internalFormat(TIME), "millis", millis,
            "tod", millis % (86400000), "utc", time.toUTCString()};
        if (more.length > 0) {
            Object[] appended = new Object[args.length + more.length];
            System.arraycopy(args, 0, appended, 0, args.length);
            System.arraycopy(more, 0, appended, args.length, more.length);
            args = appended;
        }
        Console.log(debug, args);
        return time;
    }

    /**
     * Gets the year of this time (the <b>full</b> year, not decreased by 1900).
     */
    public native int getYear ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCFullYear();
    }-*/;

    /**
     * Gets the month of this time, 0 - 11.
     */
    public native int getMonth ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCMonth();
    }-*/;

    /**
     * Gets the date (day of the month) of this time, 1 - 31.
     */
    public native int getDate ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCDate();
    }-*/;

    /**
     * Gets the hour of the day of this time, 0-23.
     */
    public native int getHours ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCHours();
    }-*/;

    /**
     * Gets the minutes past the hour of this time, 0-59.
     */
    public native int getMinutes ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCMinutes();
    }-*/;

    /**
     * Gets the seconds past the minute of this time, 0-59.
     */
    public native int getSeconds ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCSeconds();
    }-*/;

    /**
     * Gets the milliseconds past the second of this time, 0-999.
     */
    public native int getMillis ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getUTCMilliseconds();
    }-*/;

    /**
     * Converts this server time into a universal time.
     */
    public long toUniversal ()
    {
        return getTime() - TIME_ZONE.getOffset(getTime());
    }

    /**
     * Returns a new time that is the same as this one, but rolled back to the start of the
     * hour, i.e. has zero minutes, seconds and milliseconds.
     */
    public ServerTime startOfHour ()
    {
        return fromFields(getYear(), getMonth(), getDate(), getHours(), 0, 0);
    }

    /**
     * Returns a new time that is the same as this one, but rolled back to the start of the
     * day on the server, i.e. has zero hours, minutes, seconds and milliseconds.
     */
    public ServerTime startOfDay ()
    {
        return fromFields(getYear(), getMonth(), getDate(), 0, 0, 0);
    }

    /**
     * Returns a new time that is the same as this one, but rolled back to midnight on the first
     * day of the month on the server, i.e. the date is 1 and other smaller time units are zero.
     * @return
     */
    public ServerTime firstOfMonth ()
    {
        return fromFields(getYear(), getMonth(), 1, 0, 0, 0);
    }

    /**
     * Returns a new time that is the same as this one, but with the given number of months added.
     */
    public ServerTime addMonths (int months)
    {
        // we have to be careful since computer division rounds toward zero
        int newMonth = getMonth() + months;
        int newYear = getYear();
        if (newMonth >= 0) {
            newYear += newMonth / 12;
            newMonth %= 12;
        } else {
            newYear -= -newMonth / 12 + 1;
            newMonth = 12 - newMonth % 12;
        }
        return fromFields(newYear, newMonth, 1, 0, 0, 0);
    }

    /**
     * Returns a new time that is the given number of days further in the future than this.
     */
    public ServerTime addDays (int days)
    {
        return addMillis(DAY_MILLIS * days);
    }

    /**
     * Returns a new time that is the given number of hours further in the future than this.
     */
    public ServerTime addHours (int hours)
    {
        return addMillis(HOUR_MILLIS * hours);
    }

    /**
     * Returns a new time that is the given number of milliseconds further in the future than this.
     */
    public ServerTime addMillis (long millis)
    {
        return new ServerTime(getTime() + millis, 0);
    }

    /**
     * Formats this time according to a supplied format object.
     */
    public String format (DateTimeFormat fmt)
    {
        return dbgLog("ServerTime.format", this).internalFormat(fmt);
    }

    /**
     * Formats the date component of this into a short string suitable for display in table
     * columns.
     */
    public String formatDatePart ()
    {
        return format(DATE);
    }

    /**
     * Formats a server time into a short unambiguous string suitable for display in table columns.
     * Seconds and milliseconds are not represented.
     */
    public String format ()
    {
        return format(TIME);
    }

    /**
     * Formats this time into in a human readable, non tabular format. For example
     * "Yesterday at 4pm".
     */
    public String formatReadably ()
    {
        // DateUtil is blissfully unaware of time zones, translate to the default
        long time = getTime();
        time += getLocalTzOffsetMinutes(time) * MINUTE_MILLIS;
        return DateUtil.formatDateTime(new Date(time));
    }

    private String internalFormat (DateTimeFormat fmt)
    {
        return fmt.format(new Date(getTime()), UTC);
    }

    private native String toUTCString ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.toUTCString();
    }-*/;

    private ServerTime (long utcMillis, long serverTimeZoneOffset)
    {
        _adjusted = newDate(utcMillis + serverTimeZoneOffset);
        _adjusted.equals(null);
    }

    private long getTime ()
    {
        return (long)getNativeTime();
    }

    private native double getNativeTime ()
    /*-{
        var date = this.@com.threerings.sling.gwt.util.ServerTime::_adjusted;
        return date.getTime();
    }-*/;

    private static native JavaScriptObject newDate (double millis) /*-{
        return new Date(millis);
    }-*/;

    private static native int getLocalTzOffsetMinutes (double millis) /*-{
        return new Date(millis).getTimezoneOffset();
    }-*/;

    /**
     * Returns a new date with the year up by 2000 if the year of the given date is less than 100.
     * This is to make it easy for users to specify a year without rewriting gwt's DateTimeFormat.
     */
    @SuppressWarnings("deprecation")
    private static Date bumpYear (Date date)
    {
        Date result = date;

        // if a 2-digit year was put in, make it relative to 2000
        if (date.getYear() < 100) {
            result = new Date(date.getTime());
            result.setYear(result.getYear() + 2000);
        }

        return result;
    }

    private JavaScriptObject _adjusted;

    private static final long MINUTE_MILLIS = 60 * 1000L;
    private static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final long DAY_MILLIS = 24 * HOUR_MILLIS;
    private static final TimeZone UTC = TimeZone.createTimeZone(0);
    private static final DateTimeFormat DATE = DateTimeFormat.getFormat("MMM dd yyyy");
    private static final DateTimeFormat TIME = DateTimeFormat.getFormat("MMM dd yyyy h:mmaa");
    private static java.util.TimeZone TIME_ZONE = java.util.TimeZone.getTimeZone("PST8PDT");
}
