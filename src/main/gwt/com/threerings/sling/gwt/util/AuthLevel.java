//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

/**
 * The levels of authorization a user of a sling application may have.
 */
public enum AuthLevel {
    /** Not logged in. */
    NONE,

    /** Logged in as a regular user. */
    USER,

    /** Logged in as an administrator. */
    ADMIN;

    /**
     * Throws an exception if the {@link BaseContext#getCurrentAuthLevel()} does not meet the given
     * one.
     * @throws AuthorizationException
     */
    public void require (BaseContext ctx)
        throws AuthorizationException
    {
        if (ctx.getCurrentAuthLevel().ordinal() < ordinal()) {
            throw new AuthorizationException(this);
        }
    }
}
