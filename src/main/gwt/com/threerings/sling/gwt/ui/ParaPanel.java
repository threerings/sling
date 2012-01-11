//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Simple panel based on a &lt;p&gt; tag.
 */
public class ParaPanel extends SimplePanel
{
    /**
     * Creates a new paragraph panel.
     */
    public ParaPanel ()
    {
        super(Document.get().createPElement());
    }

    /**
     * Creates a new paragraph panel containing the given widget.
     */
    public ParaPanel (Widget widget)
    {
        this();
        setWidget(widget);
    }

    /**
     * Creates a new paragraph panel containing the given widget and having the given style class.
     */
    public ParaPanel (Widget widget, String style)
    {
        this(widget);
        setStyleName(style);
    }

    /**
     * Creates a new paragraph panel containing the given text.
     */
    public ParaPanel (String text)
    {
        this();
        getElement().setInnerText(text);
    }

    /**
     * Creates a new paragraph panel containing the given text and having the given style class.
     */
    public ParaPanel (String text, String style)
    {
        this(text);
        setStyleName(style);
    }
}
