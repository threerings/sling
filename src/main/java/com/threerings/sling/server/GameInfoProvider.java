//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Multimap;

import com.threerings.sling.web.data.Account;

/**
 * Provides game-related information to our support system. The application is expected to
 * bind an instance of this or else guice will complain.
 */
public interface GameInfoProvider
{
    /**
     * Loads up the game names for the supplied account names.
     * @param gameNames collection to receive the [accountName, gameName] pairs.
     */
    void resolveGameNames (Set<String> names, Multimap<String, String> gameNames);

    /**
     * Checks if a game name is a deleted one.
     */
    boolean isDeleted (String gameName);

    /**
     * Looks up the account name(s) for the supplied game name. Returns null if the supplied game
     * name is not known.
     */
    List<String> lookupAccountNames (String gameName);

    /**
     * Fills the game-specific information in for the supplied account.
     */
    void populateAccount (Account account);
}
