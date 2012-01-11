//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

/**
 * All sling-related contexts implement this.
 */
public interface BaseContext
{
    /**
     * Gets the auth level currently held by the app user. Note this should not attempt any session
     * validation, just return what is currently known.
     */
    AuthLevel getCurrentAuthLevel ();
}
