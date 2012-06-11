//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a particular user account.
 */
public class Account
    implements IsSerializable
{
    /** Flags on this account, mostly translated from OOOUser flags and tokens. */
    public enum Flag
    {
        HAS_BOUGHT_COINS, HAS_BOUGHT_TIME, FAMILY_SUBSCRIBER, ADMIN, MAINTAINER,
        INSIDER, TESTER, SUPPORT, JR_SUPPORT, BIG_SPENDER, BANNED, DEADBEAT;

        public int mask ()
        {
            // These aren't in the database, so just use the ordinal
            return 1 << ordinal();
        }
    }

    /** State values for billing account (from OOOUser). */
    public enum BillingStatus
    {
        TRIAL, SUBSCRIBER, FAILURE, EX_SUBSCRIBER, BANNED
    }

    /** A unique numeric identifier associated with this account. */
    public int accountId;

    /** The names associated with this account. */
    public AccountName name;

    /** An altername name not included in the account name. */
    public String altName;

    /** The email address on file for this account. */
    public String email;

    /** The tag for the affiliate that referred this user. */
    public String affiliate;

    /** The date on which this account was created. */
    public UniversalTime created;

    /** The date on which this account first logged ion the game. */
    public UniversalTime firstSession;

    /** The date on which this account last logged into the game. */
    public UniversalTime lastSession;

    /** The date on which this account's temp ban will end. */
    public UniversalTime tempBan;

    /** The current warning for the user. */
    public String warning;

    /** The billing status of this user. */
    public BillingStatus billingSatus;

    /** Checks if the given flag is set for this account. */
    public boolean isSet (Flag flag)
    {
        return (_flags & flag.mask()) != 0;
    }

    /** Sets the given flag for this account to the given state. */
    public void set (Flag flag, boolean on)
    {
        if (on) {
            _flags |= flag.mask();
        } else {
            _flags &= ~flag.mask();
        }
    }

    protected int _flags;
}
