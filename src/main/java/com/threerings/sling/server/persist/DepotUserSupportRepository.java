//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.util.Collection;
import java.util.List;

import com.samskivert.depot.PersistenceContext;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserCard;
import com.threerings.user.depot.DepotUserRepository;

/**
 * A SuportRepository interfaced DepotUserRepository.
 */
public class DepotUserSupportRepository
    implements UserSupportRepository
{
    public DepotUserSupportRepository (PersistenceContext pctx)
    {
        _delegate = new DepotUserRepository(pctx);
    }

    public DepotUserRepository getDelegate ()
    {
        return _delegate;
    }

    // from interface SupportRepository
    public boolean updateUser (OOOUser user)
    {
        return _delegate.updateUser(user);
    }

    // from interface SupportRepository
    public OOOUser loadUser (int userId)
    {
        return _delegate.loadUser(userId);
    }

    // from interface SupportRepository
    public OOOUser loadUserBySession (String sessionKey)
    {
        return _delegate.loadUserBySession(sessionKey, false);
    }

    // from interface SupportRepository
    public OOOUser loadUser (String username, boolean loadIdents)
    {
        return _delegate.loadUser(username, false);
    }

    // from interface SupportRepository
    public OOOUser loadUserByAccountName (String accountName)
    {
        return _delegate.loadUser(accountName, false);
    }

    // from interface SupportRepository
    public boolean refreshSession (String sessionKey, int expireDays)
    {
        return _delegate.refreshSession(sessionKey, expireDays);
    }

    // from interface SupportRepository
    public void loadMachineIdents (OOOUser user)
    {
        _delegate.loadMachineIdents(user);
    }

    // from interface SupportRepository
    public String[] getUsernames (String email)
    {
        return _delegate.getUsernames(email);
    }

    // from interface SupportRepository
    public Collection<String> filterTaintedIdents (String[] idents)
    {
        return _delegate.filterTaintedIdents(idents);
    }

    // from interface SupportRepository
    public Collection<String> filterBannedIdents (String[] idents, int siteId)
    {
        return _delegate.filterBannedIdents(idents, siteId);
    }

    // from interface SupportRepository
    public List<OOOUserCard> getUsersOfMachIdent (String machIdent)
    {
        return _delegate.getUsersOfMachIdentCards(machIdent);
    }

    // from interface SupportRepository
    public List<String> getTokenUsernames (Collection<String> usernames, byte token)
    {
        return _delegate.getTokenUsernames(usernames, token);
    }

    // from interface SupportRepository
    public boolean ban (int site, String username)
    {
        return _delegate.ban(site, username);
    }

    // from interface SupportRepository
    public boolean unban (int site, String username, boolean untaint)
    {
        return _delegate.unban(site, username, untaint);
    }

    // from interface SupportRepository
    public void addBannedIdent (String machIdent, int siteId)
    {
        _delegate.addBannedIdent(machIdent, siteId);
    }

    // from interface SupportRepository
    public void removeBannedIdent (String machIdent, int siteId)
    {
        _delegate.removeBannedIdent(machIdent, siteId);
    }

    // from interface SupportRepository
    public void addTaintedIdent (String machIdent)
    {
        _delegate.addTaintedIdent(machIdent);
    }

    // from interface SupportRepository
    public void removeTaintedIdent (String machIdent)
    {
        _delegate.removeTaintedIdent(machIdent);
    }

    /** Our delegate repository. */
    protected DepotUserRepository _delegate;
}
