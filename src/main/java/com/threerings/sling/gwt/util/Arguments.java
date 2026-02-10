//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import java.util.List;
import java.util.ListIterator;

import com.google.common.collect.Lists;

/**
 * A list of strings used as arguments. Arguments are passed into a section in order to create a
 * display widget.
 */
public class Arguments
{
    public List<String> values = Lists.newArrayList();

    /**
     * Creates a new arguments containing the given strings.
     */
    public Arguments (String... values)
    {
        for (String value : values) {
            this.values.add(value);
        }
    }

    /**
     * Creates a new arguments containing all of the given list of strings. Note the list contents
     * are added to this' list.
     */
    public Arguments (List<String> values)
    {
        this.values.addAll(values);
    }

    /**
     * Gets the size of the contained {@link #values}.
     */
    public int size ()
    {
        return values.size();
    }

    /**
     * Gets the element with the given index from the contained {@link #values}.
     */
    public String get (int idx)
    {
        return values.get(idx);
    }

    public ListIterator<String> iterator ()
    {
        return values.listIterator();
    }
}
