//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.ui;

public interface UiMessages extends com.google.gwt.i18n.client.Messages
{
    @Key("dateFormatError")
    String dateFormatError (String arg0);

    @Key("timeFormatError")
    String timeFormatError (String arg0);

    @Key("dateBetween")
    String dateBetween ();

    @Key("timeSpanFormatError")
    String timeSpanFormatError (String arg0);

    @Key("anErrorOccurred")
    String anErrorOccurred (String arg0);

    @Key("timeSpanNegativeError")
    String timeSpanNegativeError ();

    @Key("dateAnd")
    String dateAnd ();
}
