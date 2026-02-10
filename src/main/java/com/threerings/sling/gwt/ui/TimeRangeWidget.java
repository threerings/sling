//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;

import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.InputException;
import com.threerings.sling.web.data.TimeRange;

/**
 * Widget containing two date widgets and providing conversions to and from {@link TimeRange}.
 */
public class TimeRangeWidget extends Composite
{
    /** Widget for the first date of the range. */
    public final ServerTimeWidget from;

    /** Widget for the last date of the range. */
    public final ServerTimeWidget to;

    /**
     * Creates a new time range widgets set to the given range and only allowing dates to be put.
     * The time of day will be set to midnight for both dates.
     */
    public TimeRangeWidget (TimeRange init)
    {
        this(init, false);
    }

    /**
     * Creates a new time range widget set to the given range and allowing dates to be put in.
     */
    public TimeRangeWidget (TimeRange init, boolean allowTimes)
    {
        from = new ServerTimeWidget(init.from, allowTimes);
        to = new ServerTimeWidget(init.to, allowTimes);
        initWidget(Widgets.newFlowPanel("uTimeRange",
            Widgets.newLabel(_msgs.dateBetween()), from,
            Widgets.newLabel(_msgs.dateAnd()), to));
    }

    /**
     * Sets the widget to reflect the given time range.
     */
    public void setRange (TimeRange range)
    {
        from.setTime(range.from);
        to.setTime(range.to);
    }

    /**
     * Retrieves the current time range the user has input, or null if there is an error.
     * @param popupError if set then if there is a problem, pops up a helpful error message before
     * returning null
     */
    public TimeRange getRange (boolean popupError)
    {
        Long from, to;
        if ((from = this.from.getTime(popupError)) == null ||
                (to = this.to.getTime(popupError)) == null) {
            return null;
        }
        return new TimeRange(from, to);
    }

    /**
     * Retrieves the current time range the user has input, or throws an {@link InputException}
     * if there is a formatting problem. In the case of an error, shows a popup message describing
     * what the user must do to correct the problem.
     */
    public TimeRange require ()
    {
        return require(true);
    }

    /**
     * Retrieves the current time range the user has input, or throws an {@link InputException}
     * if there is a formatting problem. In the case of an error, optionally shows a popup message
     * describing what the user must do to correct the problem.
     */
    public TimeRange require (boolean popupError)
    {
        TimeRange range = getRange(popupError);
        if (range == null) {
            throw new InputException();
        }
        return range;
    }

    protected static final UiMessages _msgs = GWT.create(UiMessages.class);
}