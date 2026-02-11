//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import com.google.gwt.user.client.ui.Widget;

/**
 * Represents a section in a sling application. Sections are registered with the
 * {@link com.threerings.sling.gwt.client.SlingApp} so that requests for URLs in that section can
 * be resolved to a view of a page.
 * @param <Ctx> the context object this section will use to generate its view
 */
public interface Section<Ctx extends BaseContext>
{
    /**
     * Returns the id of this section.
     */
    SectionId getId ();

    /**
     * Creates a view for the given arguments.
     * @param args the arguments the user is navigating to. The section may modify the args in
     * order to update page fragment shown after the view is displayed. (This allows for certain
     * trickery such as pages that always refresh. See
     * {@link com.threerings.sling.gwt.client.EventsSection}.)
     * @param previous for improved performance, the application may cache the last page created by
     * this section and pass it in
     * @throws AuthorizationException if the user is not authorized to view the requested page
     */
    Widget createView (Ctx ctx, Arguments args, Widget previous)
        throws AuthorizationException;
}
