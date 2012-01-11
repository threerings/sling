//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.slink.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes for slink invocation.
 */
public interface SlinkCodes extends InvocationCodes
{
    /** The invocation service group registered by the server and requested by the client. */
    public static final String SLINK_GROUP = "slink";
}
