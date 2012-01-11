//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.InputException;

/**
 * A text entry widget for time spans. Handles several components separated by spaces. Each is
 * added to get the final result in milliseconds. Each component is a number followed by a suffix:
 * d, h, m, s or ms.
 */
public class TimeSpanWidget extends TextBox
{
    /**
     * Creates a new time span widget set to the given timespan value.
     */
    public TimeSpanWidget (long millis)
    {
        Widgets.initTextBox(this, "", 30, 10);
        setTimespan(millis);
    }

    /**
     * Sets the timespan displayed. Natural limits are used for example 61 seconds would show up
     * as "1m 1s".
     */
    public void setTimespan (long millis)
    {
        // english only
        if (millis <= 0) {
            setText("0ms");
            return;
        }

        long seconds = millis / 1000;
        millis %= 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;
        long days = hours / 24;
        hours %= 24;
        String span = "";
        span = append(span, days, "d");
        span = append(span, hours, "h");
        span = append(span, minutes, "m");
        span = append(span, seconds, "s");
        span = append(span, millis, "ms");
        setText(span);
    }

    /**
     * Converts the current text value to a timespan in milliseconds. If the format is incorrect,
     * optionally shows an error message and returns null.
     */
    public Long getTimespan (boolean popupError)
    {
        String span = getText();
        long millis = 0;
        for (int pos = 0, next; pos < span.length(); pos = next + 1) {
            next = span.indexOf(" ", pos);
            if (next == -1) {
                next = span.length();
            }
            String c = span.substring(pos, next).trim();
            if (c.length() == 0) {
                continue;
            }
            try {
                long cvalue = -1;
                cvalue = tryComponent(cvalue, c, "d", 24 * 60 * 60 * 1000L, popupError);
                cvalue = tryComponent(cvalue, c, "h", 60 * 60 * 1000L, popupError);
                cvalue = tryComponent(cvalue, c, "m", 60 * 1000L, popupError);
                cvalue = tryComponent(cvalue, c, "s", 1000L, popupError);
                cvalue = tryComponent(cvalue, c, "ms", 1L, popupError);
                if (cvalue == -1) {
                    Popups.errorBelow(_msgs.timeSpanFormatError(c), this);
                    return null;
                }
                millis += cvalue;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (millis < 0) {
            if (popupError) {
                Popups.errorBelow(_msgs.timeSpanNegativeError(), this);
            }
            return null;
        }
        return millis;
    }

    /**
     * Converts the current text value to a timespan in milliseconds. If the format is incorrect,
     * optionally shows an error message and throws an InputException.
     */
    public Long require (boolean popupError)
    {
        Long value = getTimespan(popupError);
        if (value == null) {
            throw new InputException();
        }
        return value;
    }

    protected long tryComponent (
        long lastValue, String component, String suffix, long multiple, boolean popupErr)
    {
        if (lastValue != -1) {
            return lastValue;
        }
        if (!component.endsWith(suffix)) {
            return -1;
        }
        try {
            long value = Long.valueOf(component.substring(0, component.length() - suffix.length()));
            return value * multiple;

        } catch (NumberFormatException e) {
            if (popupErr) {
                Popups.errorBelow(_msgs.timeSpanFormatError(component), this);
            }
            throw e;
        }
    }

    protected static String append (String span, long qty, String suffix)
    {
        if (qty == 0) {
            return span;
        }
        if (span.length() > 0) {
            span += " ";
        }
        return span + qty + suffix;
    }

    protected static final UiMessages _msgs = GWT.create(UiMessages.class);
}
