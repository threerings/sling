//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.gwt.util.StringUtil;

/**
 * Contains information on a particular account.
 */
public class AccountName
    implements IsSerializable
{
    /** The username associated with this account. */
    public String accountName;

    /** The game names associated with this account. */
    public List<String> gameNames;

    public AccountName ()
    {
    }

    public AccountName (String accountName, String... gameNames)
    {
        this(accountName, Arrays.asList(gameNames));
    }

    public AccountName (String accountName, List<String> gameNames)
    {
        init(accountName, gameNames);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof AccountName)) {
            return false;
        }
        return ((AccountName)other).accountName.equals(accountName);
    }

    @Override
    public int hashCode ()
    {
        return accountName.hashCode();
    }

    @Override
    public String toString ()
    {
        if (accountName.length() == 0) {
            return StringUtil.join(gameNames, ", ");
        }
        if (gameNames.size() == 0) {
            return accountName;
        }
        return StringUtil.join(gameNames, ", ") + " (" + accountName + ")";
    }

    /**
     * Initialize the name.
     */
    protected void init (String accountName, List<String> gameNames)
    {
        this.accountName = accountName;
        this.gameNames = gameNames;
    }
}
