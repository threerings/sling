//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

/**
 * Thrown in cases where a user has requested to access something in sling that is not permitted.
 * This is a checked exception since we always want to inform the user that they are not allowed,
 * or at least consciously decide not to show them anything.
 */
public class AuthorizationException extends Exception
{
    /** The auth level that was required and the user did not have. */
    public final AuthLevel requiredLevel;

    /**
     * Creates a new exception for accessing a page with the given required authorization level.
     */
    public AuthorizationException (AuthLevel requiredLevel)
    {
        this.requiredLevel = requiredLevel;
    }
}
