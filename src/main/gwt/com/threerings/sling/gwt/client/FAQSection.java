//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.sling.gwt.client.SlingNav.FAQ;
import com.threerings.sling.gwt.ui.LoadingPanel;
import com.threerings.sling.gwt.util.Arguments;
import com.threerings.sling.gwt.util.AuthLevel;
import com.threerings.sling.gwt.util.AuthorizationException;
import com.threerings.sling.gwt.util.Nav;
import com.threerings.sling.gwt.util.Section;
import com.threerings.sling.gwt.util.SectionId;
import com.threerings.sling.web.data.Category;

import static com.threerings.sling.gwt.client.SlingUtils.translateServerError;

/**
 * Implementation of the #faq section.
 */
public class FAQSection<Ctx extends SlingContext>
    implements Section<Ctx>
{
    @Override // from Section
    public SectionId getId ()
    {
        return SlingNav.FAQ.ID;
    }

    @Override // from Section
    public Widget createView (SlingContext ctx, Arguments args, Widget previous)
        throws AuthorizationException
    {
        Iterator<String> shifter = args.iterator();
        FAQ.Mode mode = shifter.hasNext() ?
            Nav.asEnum(FAQ.Mode.class, shifter.next()) : FAQ.Mode.VIEW;
        switch (mode) {
        case VIEW:
            return new FAQLoadingPanel(ctx) {
                @Override
                protected Widget finish (Category[] result)
                {
                    return FAQPanels.view(_ctx, result);
                }
            }.start();
        case EDIT:
            AuthLevel.ADMIN.require(ctx);
            return new FAQLoadingPanel(ctx) {
                @Override
                protected Widget finish (Category[] result)
                {
                    return FAQPanels.edit(_ctx, result);
                }
            }.start();
        case EDITQ:
            AuthLevel.ADMIN.require(ctx);
            final int questionId = shifter.hasNext() ? Integer.valueOf(shifter.next()) : 0;
            return new FAQLoadingPanel(ctx) {
                @Override
                protected Widget finish (Category[] result)
                {
                    return FAQPanels.editQuestion(_ctx, result, questionId);
                }
            }.start();
        }
        return null;
    }

    protected abstract class FAQLoadingPanel extends LoadingPanel<Category[]>
    {
        public FAQLoadingPanel (SlingContext ctx)
        {
            _ctx = ctx;
        }

        @Override
        protected String callService ()
        {
            _ctx.svc.getFAQs(this);
            return _msgs.loadingFaqs();
        }

        @Override
        protected String formatError (Throwable caught)
        {
            return translateServerError(caught);
        }

        protected SlingContext _ctx;
    }

    protected static final ClientMessages _msgs = GWT.create(ClientMessages.class);
}
