//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.sql.Timestamp;

import com.threerings.sling.web.client.SlingException;

/**
 * Provides game-specific action handling to our support system. The application is expected to
 * bind an instance of this or else guice will complain.
 */
public interface GameActionHandler
{
    /**
     * Performs game-side banning of an account.  The account will be banned on the site, but this
     * allows the game to boot the player from the server if they're active.
     */
    void ban (String accountName)
        throws SlingException;

    /**
     * Puts a temporary ban on the account.
     */
    void tempBan (String accountName, Timestamp expires, String warning)
        throws SlingException;

    /**
     * Changes the warning message on the account.
     */
    void warn (String accountName, String warning)
        throws SlingException;

    /**
     * Sends a message telling the account there's an update to their support request.
     * @throws SlingException
     */
    void sendMessage (String senderAccount, String recipAccount,
        String recipHandle, String message)
        throws SlingException;
}
