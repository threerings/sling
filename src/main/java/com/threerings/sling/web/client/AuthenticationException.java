//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.client;

/**
 * Thrown when there is a problem during or due to a lack of authentication.
 */
public class AuthenticationException extends SlingException
{
    public AuthenticationException ()
    {
    }

    public AuthenticationException (String message)
    {
        super(message);
    }
}
