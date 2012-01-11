//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import com.threerings.sling.web.data.TimeRange;
import com.threerings.sling.web.data.UniversalTime;

/**
 * Some useful methods for handling gwt webapp navigation.
 */
public class Nav
{
    /**
     * Converts an enumerated value to an argument. Removes underscores and converts to lower case.
     */
    public static <E extends Enum<E>> String toArg (E val)
    {
        return val.name().toLowerCase().replace("_", "");
    }

    /**
     * Converts an argument string to an enum constant.
     * @throws IllegalArgumentException if there is no constant c such that toArg(c) equals arg
     */
    public static <E extends Enum<E>> E asEnum (Class<E> eclass, String arg)
    {
        for (E val : eclass.getEnumConstants()) {
            if (toArg(val).equals(arg)) {
                return val;
            }
        }
        throw new IllegalArgumentException("Enum " + eclass + " not found for arg " + arg);
    }

    /**
     * Converts a universal time to an argument.
     */
    public static String toArg (UniversalTime time)
    {
        return String.valueOf(time.getTime());
    }

    /**
     * Gets a universal time from an argument.
     */
    public static UniversalTime asTime (String arg)
    {
        return UniversalTime.fromMillis(Long.valueOf(arg));
    }

    /**
     * Converts a time range to an argument.
     */
    public static String toArg (TimeRange range)
    {
        return toArg(range.from) + "/" + toArg(range.to);
    }

    /**
     * Gets a time range from an argument.
     */
    public static TimeRange asTimeRange (String arg)
    {
        int slashPos = arg.indexOf("/");
        return new TimeRange(
            asTime(arg.substring(0, slashPos)),
            asTime(arg.substring(slashPos + 1)));
    }

    /**
     * Converts a set of enums to an argument.
     */
    public static <E extends Enum<E>> String toArg (Collection<E> set)
    {
        String arg = "";
        for (E e : set) {
            if (arg.length() > 0) {
                arg += "/";
            }
            arg += toArg(e);
        }
        return arg;
    }

    /**
     * Gets a set of enums from an argument.
     */
    public static <E extends Enum<E>> Set<E> asSet (Class<E> eclass, String arg)
    {
        Set<E> set = Sets.newHashSet();
        for (String e : arg.split("/")) {
            set.add(asEnum(eclass, e));
        }
        return set;
    }
}
