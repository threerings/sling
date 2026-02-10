//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Iterator;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.sling.gwt.client.SlingNav.Requests;
import com.threerings.sling.gwt.util.Arguments;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.AuthorizationException;
import com.threerings.sling.gwt.util.Nav;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;

/**
 * Implementation of the #petition section.
 */
public class PetitionSection<Ctx extends SlingContext>
    implements Section<Ctx>
{
    @Override // from Section
    public SectionId getId ()
    {
        return Requests.ID;
    }

    @Override // from Section
    public Widget createView (Ctx ctx, Arguments args, Widget previous)
        throws AuthorizationException
    {
        AuthLevel.USER.require(ctx);
        Iterator<String> shifter = args.iterator();
        Requests.Mode mode = shifter.hasNext() ?
            Nav.asEnum(Requests.Mode.class, shifter.next()) : Requests.Mode.VIEW;
        switch (mode) {
        case VIEW:
            return createPetitionsPanel(ctx);
        case NEW:
            return createSubmitPanel(ctx);
        }
        return null;
    }

    protected Widget createSubmitPanel (Ctx ctx)
    {
        return new SubmitPetitionPanel(ctx, null, true);
    }

    protected Widget createPetitionsPanel (Ctx ctx)
    {
        return new PetitionsPanel(ctx);
    }
}
