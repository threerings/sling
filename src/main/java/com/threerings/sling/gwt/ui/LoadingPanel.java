//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Widgets;

/**
 * A simple panel that shows a loading message while waiting for a reply from the server.
 * Subclasses provide the service call and the widget to set after completion. If a failure occurs,
 * the widget is set to a generic error message using the translation of the error.
 */
public abstract class LoadingPanel<T> extends SimplePanel
    implements AsyncCallback<T>
{
    /**
     * Creates a new loading panel with the given error translator.
     */
    public LoadingPanel()
    {
    }

    /**
     * Calls the service and sets the loading text.
     */
    public LoadingPanel<T> start ()
    {
        setText(callService());
        return this;
    }

    @Override // from AsyncCallback
    public void onFailure (Throwable caught)
    {
        setText(_umsgs.anErrorOccurred(formatError(caught)));
    }

    @Override // from AsyncCallback
    public void onSuccess (T result)
    {
        setWidget(finish(result));
    }

    /**
     * Sets the widget to a label with the given string.
     */
    public void setText (String str)
    {
        setWidget(Widgets.newLabel(str));        
    }

    /**
     * Translates the error from the server for display. The default implementation returns
     * {@link Throwable#getMessage()}.
     */
    abstract protected String formatError (Throwable caught);

    /**
     * Calls the service and returns the string to display for the loading message.
     */
    abstract protected String callService ();

    /**
     * Creates the widget to display using the result from the server.
     */
    abstract protected Widget finish (T result);

    protected static final UiMessages _umsgs = GWT.create(UiMessages.class);
}
