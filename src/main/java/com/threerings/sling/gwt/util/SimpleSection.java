//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

/**
 * A simple section implementation that has just one page and a single authorization level
 * requirement. Delegates to the subclass for widget creation using a simpler method.
 *
 * @param <Ctx> the context type the section is based on
 */
public abstract class SimpleSection<Ctx extends BaseContext>
    implements Section<Ctx>
{
    /**
     * Creates and returns a section with an empty section id. This means that the section, once
     * registered, will be used whenever no token is specified. If the user's auth level is below
     * the given level, an auth error will be raised on view creation. If a view is requested with
     * some arguments, a runtime exception is raised.
     */
    public static <Ctx extends BaseContext> SimpleSection<Ctx> makeDefault (
            AuthLevel authLevel)
    {
        return new SimpleSection<Ctx>(new SectionId(""), authLevel) {
            @Override public Widget createView (Ctx ctx) {
                return Widgets.newLabel("");
            }
        };
    }

    /** The id of the section. */
    public final SectionId sectionId;

    /** The required auth level of the section. */
    public final AuthLevel authLevel;

    @Override // from Section
    public Widget createView (Ctx ctx, Arguments args, Widget previous)
        throws AuthorizationException
    {
        if (args.size() > 0) {
            throw new RuntimeException("Unexpected arguments");
        }
        authLevel.require(ctx);
        return createView(ctx);
    }

    @Override // from Section
    public SectionId getId ()
    {
        return sectionId;
    }

    /**
     * Creates a new section with the given id and no auth level requirement.
     */
    protected SimpleSection (SectionId id)
    {
        this(id, AuthLevel.NONE);
    }

    /**
     * Creates a new section with the given id and auth level requirements.
     */
    protected SimpleSection (SectionId id, AuthLevel authLevel)
    {
        this.sectionId = id;
        this.authLevel = authLevel;
    }

    /**
     * Creates the widget for this section using the given context.
     */
    protected abstract Widget createView (Ctx ctx);
}
