//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.slink.data;

import com.threerings.util.Name;

/**
 * Represents an authenticated slink client on the game server.
 */
public class SlinkAuthName extends Name
{
    /**
     * Creates a new auth name with the given client id.
     */
    public SlinkAuthName (String clientId)
    {
        super(clientId);
    }
}
