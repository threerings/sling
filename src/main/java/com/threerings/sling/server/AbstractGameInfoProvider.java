//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Multimap;

import com.threerings.sling.web.data.Account;

/**
 * Provides no game-specific info. This is just a class to make testing easier.
 */
public class AbstractGameInfoProvider
    implements GameInfoProvider
{
    @Override // from GameInfoProvider
    public void resolveGameNames (Set<String> names, Multimap<String, String> gameNames)
    {
        for (String name : names) {
            gameNames.put(name, name);
        }
    }

    @Override // from GameInfoProvider
    public boolean isDeleted (String gameName)
    {
        return false;
    }

    @Override // from GameInfoProvider
    public List<String> lookupAccountNames (String gameName)
    {
        return null;
    }

    @Override // from GameInfoProvider
    public void populateAccount (Account account)
    {
    }
}
