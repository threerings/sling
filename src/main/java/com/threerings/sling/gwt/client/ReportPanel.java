//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.ItemListBox;
import com.threerings.gwt.ui.NumberTextBox;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.Widgets;
import com.threerings.gwt.util.ClickCallback;
import com.threerings.gwt.util.DateUtil;
import com.threerings.sling.gwt.client.SlingNav.Reports;
import com.threerings.sling.gwt.ui.TimeRangeWidget;
import com.threerings.sling.gwt.util.PageAddress;
import com.threerings.sling.gwt.util.ServerTime;
import com.threerings.sling.gwt.util.TimeRanges;
import com.threerings.sling.web.client.SlingService.AverageEventVolumeByHour;
import com.threerings.sling.web.client.SlingService.EventResponses;
import com.threerings.sling.web.client.SlingService.EventVolume;
import com.threerings.sling.web.client.SlingService.TimeUnit;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventFilter;
import com.threerings.sling.web.data.EventSearch;
import com.threerings.sling.web.data.TimeRange;

/**
 * Superclass and static methods for generating reports.
 */
public abstract class ReportPanel<T> extends FlowPanel
{
    /**
     * Creates a new main page containing links to other reports pages.
     */
    public static Widget newMainPage (SlingContext ctx)
    {
        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("uReports");
        panel.add(Widgets.newLabel(_msgs.reportsTitle(), "Title"));
        panel.add(SlingUtils.makeLink(ctx, _msgs.firstResponse(),
            Reports.blank(Reports.Mode.FIRST_RESPONSE)));
        panel.add(SlingUtils.makeLink(ctx, _msgs.recentEventVolume(),
            Reports.blank(Reports.Mode.RECENT_VOLUME)));
        panel.add(SlingUtils.makeLink(ctx, _msgs.averageEventVolume(),
            Reports.blank(Reports.Mode.AVERAGE_VOLUME)));
        panel.add(SlingUtils.makeLink(ctx, _msgs.agentActivity(),
            Reports.blank(Reports.Mode.AGENT_ACTIVITY)));
        return panel;
    }

    /**
     * Creates a new panel for requesting a first response report.
     */
    public static Widget newFirstResponse (SlingContext ctx)
    {
        return newFirstResponse(ctx, Event.Type.PETITION, TimeRanges.priorMonth(),
            24 * TimeUnit.HOUR.millis, false);
    }

    /**
     * Creates a new panel displaying the given first response report (automatically calls report).
     */
    public static Widget newFirstResponse (
        SlingContext ctx, Event.Type type, TimeRange range, long time)
    {
        return newFirstResponse(ctx, type, range, time, true);
    }

    /**
     * Creates a new panel for requesting an event volume report, filled with some default values.
     */
    public static Widget newRecentVolume (SlingContext ctx)
    {
        return newRecentVolume(ctx, TimeUnit.DAY, 7, false);
    }

    /**
     * Creates a new panel for displaying the given event volume report (automatically generates).
     */
    public static Widget newRecentVolume (SlingContext ctx, TimeUnit timeUnit, int count)
    {
        return newRecentVolume(ctx, timeUnit, count, true);
    }

    /**
     * Creates a new panel for requesting an average event volume report, filled with some default
     * values.
     */
    public static Widget newAverageVolume (SlingContext ctx)
    {
        TimeRange range = TimeRanges.recentDays(30);
        return newAverageVolume(ctx, range,
            Sets.newHashSet(Event.Type.PETITION, Event.Type.COMPLAINT), false);
    }

    /**
     * Creates a new panel for displaying the given average event volume report (automatically
     * generates).
     */
    public static Widget newAverageVolume (SlingContext ctx, TimeRange range,
        Set<Event.Type> types)
    {
        return newAverageVolume(ctx, range, types, true);
    }

    /**
     * Creates a new panel for requesting an agent activity report, filled with some default values.
     */
    public static Widget newAgentActivity (SlingContext ctx)
    {
        return newAgentActivity(ctx, false);
    }

    protected static Widget newFirstResponse (
        SlingContext ctx, Event.Type type, TimeRange range, long time, boolean run)
    {
        final ItemListBox<Event.Type> typeCtrl = ItemListBox.<Event.Type>builder()
            .select(type)
            .add(Event.Type.PETITION, _msgs.eventTypePetition())
            .add(Event.Type.COMPLAINT, _msgs.eventTypeComplaint())
            .build();
        final TimeRangeWidget rangeCtrl = new TimeRangeWidget(range);
        final NumberTextBox hoursCtrl = NumberTextBox.newIntBox(3, 2)
            .withValue(time / TimeUnit.HOUR.millis);

        return new ReportPanel<EventResponses>(ctx, _msgs.firstResponseTitle(), run) {
            Event.Type type;
            TimeRange range;
            long millis;

            @Override protected void addInputs () {
                addInput(_msgs.eventTypeLabel(), typeCtrl);
                addInput(_msgs.created(), rangeCtrl);
                addInput(_msgs.responseTime(),
                    Widgets.newRow(hoursCtrl, Widgets.newLabel(_msgs.hours())));
            }

            @Override protected PageAddress callService () {
                range = rangeCtrl.require();
                type = typeCtrl.getSelectedItem();
                millis = hoursCtrl.getNumber().intValue() * TimeUnit.HOUR.millis;
                _ctx.svc.checkResponseTimes(type, range, millis, _callback);
                return Reports.firstResponse(type, range, millis);
            }

            @Override protected Widget createResultWidget (EventResponses result) {
                SmartTable table = new SmartTable("FirstResponse", 0, 0);
                table.cell(0, 0).text(_msgs.totalEventsReported()).styles("Label");
                table.cell(0, 1).text(String.valueOf(result.total));
                table.cell(1, 0).text(_msgs.qualifiedEvents()).styles("Label");
                table.cell(1, 1).text(String.valueOf(result.qualified));
                table.cell(2, 0).text(_msgs.failedEvents()).styles("Label");
                table.cell(2, 1).widget(Widgets.newFlowPanel("Row",
                    Widgets.newLabel(String.valueOf(result.total - result.qualified)),
                    SlingUtils.makeLink(_ctx,
                        _msgs.showEventsWithFailedResponses(), SlingNav.Events.search(
                            new EventSearch(
                                EventFilter.createdIn(range),
                                EventFilter.typeIs(type),
                                EventFilter.firstResponseIsMoreThan(millis))))));
                table.cell(3, 0).text(_msgs.qualifiedScore()).styles("Label");
                if (result.total == 0) {
                    table.cell(3, 1).text("-");
                } else {
                    table.cell(3, 1).text(NumberFormat.getFormat("#0.00%").format(
                        (double)result.qualified / result.total));
                }
                return table;
            }
        };
    }

    protected static Widget newRecentVolume (
        SlingContext ctx, TimeUnit timeUnit, int count, boolean run)
    {
        final ItemListBox<TimeUnit> timeUnitsCtrl = ItemListBox.<TimeUnit>builder()
            .select(timeUnit)
            .add(TimeUnit.HOUR, _msgs.hoursItem())
            .add(TimeUnit.DAY, _msgs.daysItem())
            .build();
        final NumberTextBox countCtrl = NumberTextBox.newIntBox(3, 2).withValue(count);

        return new ReportPanel<EventVolume>(ctx, _msgs.recentEventVolumeTitle(), run) {
            long now;
            TimeUnit unit;
            int count;

            @Override protected void addInputs () {
                addInput(_msgs.volumePrefix(), countCtrl);
                addInput("", timeUnitsCtrl);
            }

            @Override protected PageAddress callService () {
                unit = timeUnitsCtrl.getSelectedItem();
                count = countCtrl.getNumber().intValue();
                now = System.currentTimeMillis();
                _ctx.svc.getVolume(now, unit, count, _callback);
                return Reports.recentVolume(unit, count);
            }

            @Override protected Widget createResultWidget (EventVolume result) {
                DateTimeFormat fmt = unit == TimeUnit.DAY ? DAY_FMT : TIME_FMT;
                SmartTable table = new SmartTable("RecentVolume", 0, 0);
                table.cell(0, 0).widget(Widgets.newLabel(unit == TimeUnit.DAY ?
                    _msgs.dayHdr() : _msgs.hourHdr())).styles("Header", "col0");
                table.cell(0, 1).widget(Widgets.newLabel(_msgs.volumeHdr()))
                    .styles("Header", "col1");

                ServerTime serverTime = ServerTime.create(result.begin);
                for (int ii = 0; ii < result.eventCounts.length; ++ii) {
                    ServerTime currentTime = (unit == TimeUnit.DAY) ?
                        serverTime.addDays(ii) : serverTime.addHours(ii);
                    Date currentDate = new Date(currentTime.getTime());
                    table.cell(ii + 1, 0).widget(Widgets.newLabel(fmt.format(currentDate)))
                        .styles("col0");
                    table.cell(ii + 1, 1).widget(Widgets.newLabel(String.valueOf(
                        result.eventCounts[ii]))).styles("col1");
                    table.getRowFormatter().setStyleName(ii + 1, "row" + (ii % 2));
                }
                return table;
            }
        };
    }

    protected static Widget newAverageVolume (
        SlingContext ctx, TimeRange range, Set<Event.Type> types, boolean run)
    {
        final TimeRangeWidget rangeCtrl = new TimeRangeWidget(range);
        final ItemListBox<Set<Event.Type>> typesCtrl = ItemListBox.<Set<Event.Type>>builder()
            .select(types)
            .add(Sets.newHashSet(Event.Type.PETITION, Event.Type.COMPLAINT),
                _msgs.petitionsAndComplaints())
            .add(Sets.newHashSet(Event.Type.PETITION), _msgs.petitions())
            .add(Sets.newHashSet(Event.Type.COMPLAINT), _msgs.complaints())
            .build();

        return new ReportPanel<AverageEventVolumeByHour>(ctx, _msgs.averageEventVolumeTitle(), run) {
            TimeRange range;
            Set<Event.Type> types;

            @Override protected void addInputs () {
                addInput(_msgs.created(), rangeCtrl);
                addInput(_msgs.eventTypes(), typesCtrl);
            }

            @Override protected PageAddress callService () {
                range = rangeCtrl.require();
                types = typesCtrl.getSelectedItem();
                _ctx.svc.getAverageVolume(range, types, _callback);
                return Reports.averageVolume(range, types);
            }

            @Override protected Widget createResultWidget (AverageEventVolumeByHour result) {
                SmartTable table = new SmartTable("AverageVolume", 0, 0);
                table.cell(0, 0).text("").styles("HeaderRow", "HeaderCol");

                // days of the week across the top
                Date tmpDate = new Date();
                for (int ii = 0; ii < 7; ++ii) {
                    int dow = DateUtil.getDayOfWeek(tmpDate);
                    table.cell(0, dow + 1).widget(
                        Widgets.newLabel(DOW_FMT.format(tmpDate))).styles("HeaderRow");
                    tmpDate.setTime(tmpDate.getTime() + TimeUnit.DAY.millis);
                }

                // hours of the day along the left
                DateUtil.zeroTime(tmpDate);
                for (int ii = 0; ii < 24; ++ii) {
                    table.cell(ii + 1, 0).widget(
                        Widgets.newLabel(TOD_FMT.format(tmpDate))).styles("HeaderCol");
                    table.getRowFormatter().setStyleName(ii + 1, "row" + (ii % 2));
                    tmpDate.setTime(tmpDate.getTime() + TimeUnit.HOUR.millis);
                }

                for (int day = 0; day < 7; ++day) {
                    for (int hour = 0; hour < 24; ++hour) {
                        float count = result.getEventCount(day, hour);
                        String label = count < 0 ? "-" : NUM_FMT.format(count);
                        table.cell(hour + 1, day + 1).widget(Widgets.newLabel(label));
                    }
                }

                return table;
            }
        };
    }

    protected static Comparator<Map.Entry<String, Long>> BY_TIME_VALUE =
            new Comparator<Map.Entry<String, Long>>() {
        public int compare (Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) {
            return Longs.compare(e2.getValue(), e1.getValue());
        }
    };

    protected static Widget newAgentActivity (
        SlingContext ctx, boolean run)
    {
        return new ReportPanel<HashMap<String, Long>>(ctx, _msgs.agentActivityTitle(), run) {
            @Override protected void addInputs () {
            }

            @Override protected PageAddress callService () {
                _ctx.svc.getAgentActivity(_callback);
                return Reports.agentActivity();
            }

            @Override protected Widget createResultWidget (HashMap<String, Long> result) {
                List<Map.Entry<String, Long>> entries = Lists.newArrayList(result.entrySet());
                Collections.sort(entries, BY_TIME_VALUE);
                SmartTable table = new SmartTable("AgentActivity", 0, 0);
                int row = 0;
                table.cell(row++, 0)
                    .widget(Widgets.newLabel("Agent")).styles("Header", "col0").nextCol()
                    .widget(Widgets.newLabel("Time")).styles("Header", "col1");
                for (Map.Entry<String, Long> e : entries) {
                    Widget link = SlingUtils.makeLink(_ctx, e.getKey(), SlingNav.Events.search(
                        new EventSearch(EventFilter.ownerIs(e.getKey()))));
                    String time = ServerTime.from(e.getValue()).formatReadably();
                    table.cell(row, 0)
                        .widget(link).styles("col0").nextCol()
                        .widget(Widgets.newLabel(time)).styles("col1");
                    table.getRowFormatter().setStyleName(row, "row" + (row % 2));
                    row++;
                }
                return table;
            }
        };
    }

    protected ReportPanel (SlingContext ctx, String title, final boolean run)
    {
        _ctx = ctx;
        setStyleName("uReportPanel");

        add(Widgets.newLabel(title, "Title"));
        add(_inputs);
        add(_resultHdr);
        add(_result);

        addInputs();

        Button btn = new Button(_msgs.generate());
        int row = _inputs.getRowCount();
        _inputs.cell(row, 1).widget(btn);

        _callback = new ClickCallback<T>(btn) {
            @Override protected boolean callService () {
                PageAddress address = ReportPanel.this.callService();
                if (address != null) {
                    _resultHdr.setText(_msgs.generating());
                    _ctx.frame.navigatedTo(address);
                }
                return address != null;
            }

            @Override protected boolean gotResult (T result) {
                _resultHdr.setText(_msgs.result());
                _result.setWidget(createResultWidget(result));
                return true;
            }

            @Override protected String formatError (Throwable cause) {
                return SlingUtils.translateServerError(cause);
            }
        };

        if (run) {
            _callback.click();
        }
    }

    protected void addInput (String label, Widget widget)
    {
        int row = _inputs.getRowCount();
        _inputs.cell(row, 0).text(label).styles("Label");
        _inputs.cell(row, 1).widget(widget);
    }

    /**
     * Adds the inputs for the specific kind of report to the {@link #_inputs} table.
     */
    protected abstract void addInputs ();

    /**
     * Reads the inputs and calls the service to generate the result. Returns the address that
     * will go directly to the report.
     */
    protected abstract PageAddress callService ();

    /**
     * Creates a widget to display the result received from the server.
     */
    protected abstract Widget createResultWidget (T result);

    protected SlingContext _ctx;
    protected SmartTable _inputs = new SmartTable("Inputs", 0, 0);
    protected Label _resultHdr = Widgets.newLabel("", "ResultHeader");
    protected SimplePanel _result = new SimplePanel();
    protected ClickCallback<T> _callback;

    protected static final DateTimeFormat DAY_FMT = DateTimeFormat.getFormat("EEE MMM dd");
    protected static final DateTimeFormat TIME_FMT = DateTimeFormat.getFormat("EEE MMM dd hh:mm");
    protected static final DateTimeFormat DOW_FMT = DateTimeFormat.getFormat("EEE");
    protected static final DateTimeFormat TOD_FMT = DateTimeFormat.getFormat("HH");
    protected static final NumberFormat NUM_FMT = NumberFormat.getFormat("0.0");
    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
