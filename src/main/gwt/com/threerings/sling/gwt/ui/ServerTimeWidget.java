//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.InputException;
import com.threerings.sling.gwt.util.ServerTime;

/**
 * A text entry widget for a date, optionally allowing time as well. For easy typing, handles MM/DD
 * dates within the current year, MM/DD/YY dates within our century of 2000 and full MM/DD/YYYY
 * dates. Time is entered using ##(:##)?[a|p]m? where ## is 1 to 12 and 00 to 60, or ##(:##)?
 * where ## is 0 to 23 and ## is 00 to 60.
 *
 * <p>NOTE on time zones: The public API for this class uses UniversalTime objects, since those
 * are most often used for communicating with the server. Internally, the universal times are
 * converted to and from {@link ServerTime} instances in order to display and parse the date and
 * time values.</p>
 */
public class ServerTimeWidget extends Composite
{
    /**
     * Creates a new widget set to the date part of the given time. The time component is ignored
     * and may not be entered.
     */
    public ServerTimeWidget (long value)
    {
        this(value, false);
    }

    /**
     * Creates a new widget set to the given time, optionally allowing time to be edited. If
     * time is not allowed, then only the year, month and day are edited and the time returned by
     * {@link #getTime()} will be at midnight (all time fields set to zero).
     */
    public ServerTimeWidget (long value, boolean allowTime)
    {
        initWidget(_hpanel = new HorizontalPanel());
        setStyleName("uDate");
        _hpanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _hpanel.add(_date = Widgets.newTextBox("", 10, 10));
        if (allowTime) {
            _time = Widgets.newTextBox("", 7, 7);
            Image image = new Image(_rsrcs.clock());
            _hpanel.add(image);
            image.addClickHandler(new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    toggleTime();
                }
            });
        }
        setTime(value);
    }

    /**
     * Sets the date and time displayed. Note that if time is not being edited by this widget,
     * then only the date part is set.
     */
    public void setTime (long utime)
    {
        ServerTime time = ServerTime.from(utime);
        ServerTime now = ServerTime.now();
        int fmt = 0;
        if (time.getYear() == now.getYear()) {
            fmt = 2;
        } else if (time.getYear() >= 2000) {
            fmt = 1;
        }

        _date.setText(time.format(_fmts[fmt]));

        if (_time != null) {
            fmt = time.getMinutes() == 0 ? 0 : 1;
            _time.setText(time.format(_tfmts[fmt]));

            if (!hasTime() && (time.getHours() != 0 || time.getMinutes() != 0)) {
                toggleTime();
            }
        }
    }

    /**
     * Converts the current widget input text to a time. If any value is incorrectly formatted and
     * the time value could not be calculated, optionally shows an error message and returns null.
     * If the widget was configured to allow time and the user has chosen to enter the time, then
     * it will be included. Otherwise, the returned date will be at midnight (have all time fields
     * set to zero).
     */
    public Long getTime (boolean popupError)
    {
        ServerTime time = getDateOnly(popupError);
        if (time == null || !hasTime()) {
            return time.toUniversal();
        }
        long tod = getTimeOfDay(popupError);
        if (tod < 0) {
            return null;
        }
        return ServerTime.dbgLog("ServerTimeWidget.getTime", time.addMillis(tod)).toUniversal();
    }

    /**
     * Converts the current widget input text to a time. If any value is incorrectly formatted and
     * the time value could not be calculated, shows an error message and throws
     * {@link InputException}. If the widget was configured to allow time and the user has chosen
     * to enter the time, then it will be included. Otherwise, the returned date will have no time
     * component.
     */
    public long require ()
    {
        Long time = getTime(true);
        if (time == null) {
            throw new InputException();
        }
        return time;
    }

    protected long getTimeOfDay (boolean popupError)
    {
        String time = _time.getText();
        for (DateTimeFormat fmt : _tfmts) {
            try {
                ServerTime value = ServerTime.parse(fmt, time);
                return (value.getHours() * 60 + value.getMinutes()) * 60 * 1000L;
            } catch (IllegalArgumentException ex) {
            }
        }
        Popups.errorBelow(_msgs.timeFormatError(time), this);
        _time.setFocus(true);
        return -1;
    }

    protected ServerTime getDateOnly (boolean popupError)
    {
        String date = _date.getText();
        for (DateTimeFormat fmt : _fmts) {
            try {
                return ServerTime.parse(fmt, date);
            } catch (IllegalArgumentException ex) {
            }
        }
        Popups.errorBelow(_msgs.dateFormatError(date), this);
        _date.setFocus(true);
        return null;
    }

    protected boolean hasTime ()
    {
        return _time != null && _hpanel.getWidgetIndex(_time) != -1;
    }

    protected void toggleTime ()
    {
        if (hasTime()) {
            _hpanel.remove(_time);
        } else {
            _hpanel.add(_time);
        }
    }

    protected HorizontalPanel _hpanel;
    protected TextBox _date;
    protected TextBox _time;
    protected Widget _timeToggle;

    protected static DateTimeFormat _fmts[] = {
        DateTimeFormat.getFormat("MM/dd/yyyy"),
        DateTimeFormat.getFormat("MM/dd/yy"),
        DateTimeFormat.getFormat("MM/dd")
    };

    protected static DateTimeFormat _tfmts[] = {
        DateTimeFormat.getFormat("ha"),
        DateTimeFormat.getFormat("h:mma"),
        DateTimeFormat.getFormat("HH"),
        DateTimeFormat.getFormat("HH:mm")
    };

    protected static final UiMessages _msgs = GWT.create(UiMessages.class);
    protected static final UiResources _rsrcs = GWT.create(UiResources.class);
}
