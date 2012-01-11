//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import com.threerings.sling.web.data.TimeRange;

/**
 * A few methods for easily creating time ranges in gwt.
 */
public class TimeRanges
{
    public static TimeRange recentDays (int days)
    {
        ServerTime now = ServerTime.now().startOfDay();
        return new TimeRange(now.addDays(-days).toUniversal(), now.toUniversal());
    }

    public static TimeRange priorMonth ()
    {
        ServerTime to = ServerTime.now().firstOfMonth();
        return new TimeRange(to.addMonths(-1).toUniversal(), to.toUniversal());
    }

    public static TimeRange recentHours (int hours)
    {
        ServerTime now = ServerTime.now().startOfHour();
        return new TimeRange(now.addHours(-hours).toUniversal(), now.toUniversal());
    }
}
