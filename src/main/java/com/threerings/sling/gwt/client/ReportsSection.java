//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Iterator;
import java.util.Set;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.sling.gwt.client.SlingNav.Reports;
import com.threerings.sling.gwt.util.Arguments;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.AuthorizationException;
import com.threerings.sling.gwt.util.Nav;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;
import com.threerings.sling.web.client.SlingService.TimeUnit;
import com.threerings.sling.web.client.SlingService;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.TimeRange;

public class ReportsSection<Ctx extends SlingContext>
    implements Section<Ctx>
{
    @Override // from Section
    public SectionId getId ()
    {
        return Reports.ID;
    }

    @Override // from Section
    public Widget createView (Ctx ctx, Arguments args, Widget previous)
        throws AuthorizationException
    {
        AuthLevel.ADMIN.require(ctx);
        Iterator<String> shifter = args.iterator();
        Reports.Mode mode = shifter.hasNext() ? Nav.asEnum(Reports.Mode.class, shifter.next()) :
            Reports.Mode.SELECT;
        switch (mode) {
        case SELECT:
            return ReportPanel.newMainPage(ctx);
        case FIRST_RESPONSE: {
            if (!shifter.hasNext()) {
                return ReportPanel.newFirstResponse(ctx);
            }
            Event.Type type = Nav.asEnum(Event.Type.class, shifter.next());
            TimeRange range = Nav.asTimeRange(shifter.next());
            long millis = Integer.parseInt(shifter.next());
            return ReportPanel.newFirstResponse(ctx, type, range, millis);
        }
        case RECENT_VOLUME: {
            if (!shifter.hasNext()) {
                return ReportPanel.newRecentVolume(ctx);
            }
            TimeUnit unit = Nav.asEnum(SlingService.TimeUnit.class, shifter.next());
            int count = Integer.parseInt(shifter.next());
            return ReportPanel.newRecentVolume(ctx, unit, count);
        }
        case AVERAGE_VOLUME: {
            if (!shifter.hasNext()) {
                return ReportPanel.newAverageVolume(ctx);
            }
            TimeRange range = Nav.asTimeRange(shifter.next());
            Set<Event.Type> types = Nav.asSet(Event.Type.class, shifter.next());
            return ReportPanel.newAverageVolume(ctx, range, types);
        }
        case AGENT_ACTIVITY: {
            return ReportPanel.newAgentActivity(ctx);
        }
        }
        return null;
    }
}
