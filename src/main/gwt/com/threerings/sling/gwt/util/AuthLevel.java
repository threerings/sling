//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * The levels of authorization a user of a sling application may have.
 */
public enum AuthLevel implements IsSerializable {
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
