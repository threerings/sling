//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a set of accounts related by having logged in via the same machine identifier.
 */
public class MachineIdentity
    implements IsSerializable
{
    public static class AccountInfo
        implements IsSerializable
    {
        public AccountName name;
        public boolean paid;
        public boolean banned;
    }

    public MachineIdentity (String id, boolean tainted, boolean banned)
    {
        this.machIdent = id;
        this.tainted = tainted;
        this.banned = banned;
    }

    public MachineIdentity ()
    {
    }

    /** The machine identifier in question. */
    public String machIdent;

    /** Whether or not this machine identifier has been tainted. */
    public boolean tainted;

    /** Whether or not this machine identifier has been banned. */
    public boolean banned;

    /** The accounts that have logged in with this machine identifier. */
    public List<AccountInfo> accounts;
}
