//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.sql.Timestamp;

/**
 * Performs no game-specific actions. This is just a class to make testing easier.
 */
public class AbstractGameActionHandler
    implements GameActionHandler
{
    @Override // from GameActionHandler
    public void ban (String accountName)
    {
    }

    @Override // from GameActionHandler
    public void tempBan (String accountName, Timestamp expires, String warning)
    {
    }

    @Override // from GameActionHandler
    public void warn (String accountName, String warning)
    {
    }

    @Override // from GameActionHandler
    public void sendMessage (String senderAccount, String recipAccount,
        String recipHandle, String message)
    {
    }
}
