//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.slink.data;

import com.threerings.presents.net.ServiceCreds;

/**
 * Credentials for a slink service to connect to a presents game server.
 */
public class SlinkCreds extends ServiceCreds
{
    /**
     * Creates new credentials using the given client identifier and shared secret.
     */
    public SlinkCreds (String clientId, String sharedSecret)
    {
        super(clientId, sharedSecret);
    }

    /** Deserialization. */
    public SlinkCreds ()
    {
    }
}
