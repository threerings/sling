//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Holds all parameters for an advanced event search.
 */
public class EventSearch
    implements IsSerializable
{
    /** Ways to sort the results. */
    public enum Sort
    {
        CREATION
    }

    /** The filters for the search. */
    public List<EventFilter> filters = new ArrayList<EventFilter>();

    /** The sort value for results. */
    public Sort sort = Sort.CREATION;

    public EventSearch ()
    {
    }

    public EventSearch (EventFilter... filters)
    {
        for (EventFilter filter : filters) {
            this.filters.add(filter);
        }
    }

    @Override  public String toString ()
    {
        return "EventSearch [filters=" + filters + ", sort=" + sort + "]";
    }
}
