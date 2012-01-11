//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.client;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Thrown when we encounter some problem on the server.
 */
public class SlingException extends Exception
    implements IsSerializable
{
    public SlingException ()
    {
    }

    public SlingException (String message)
    {
        super(message);
    }
}
