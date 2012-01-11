//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.util.Collection;
import java.util.List;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserCard;
import com.threerings.user.OOOUserRepository;

/**
 * A SupportRepository interfaced OOOUserRepository.
 */
public class OOOUserSupportRepository
    implements UserSupportRepository
{
    public OOOUserSupportRepository (ConnectionProvider conprov)
    {
        try {
            _delegate = new OOOUserRepository(conprov);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    public OOOUserRepository getDelegate ()
    {
        return _delegate;
    }

    // from interface SupportRepository
    public boolean updateUser (OOOUser user)
    {
        try {
            return _delegate.updateUser(user);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public OOOUser loadUser (int userId)
    {
        try {
            return (OOOUser)_delegate.loadUser(userId);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public OOOUser loadUserBySession (String sessionKey)
    {
        try {
            return (OOOUser)_delegate.loadUserBySession(sessionKey);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public OOOUser loadUser (String username, boolean loadIdents)
    {
        try {
            return _delegate.loadUser(username, false);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public OOOUser loadUserByAccountName (String accountName)
    {
        try {
            return _delegate.loadUser(accountName, false);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public boolean refreshSession (String sessionKey, int expireDays)
    {
        try {
            return _delegate.refreshSession(sessionKey, expireDays);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public void loadMachineIdents (OOOUser user)
    {
        try {
            _delegate.loadMachineIdents(user);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public String[] getUsernames (String email)
    {
        try {
            return _delegate.getUsernames(email);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public Collection<String> filterTaintedIdents (String[] idents)
    {
        try {
            return _delegate.filterTaintedIdents(idents);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public Collection<String> filterBannedIdents (String[] idents, int siteId)
    {
        try {
            return _delegate.filterBannedIdents(idents, siteId);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public List<OOOUserCard> getUsersOfMachIdent (String machIdent)
    {
        throw new UnsupportedOperationException("Only depot retrieval currently supported.");
    }

    // from interface SupportRepository
    public List<String> getTokenUsernames (Collection<String> usernames, byte token)
    {
        try {
            return _delegate.getTokenUsernames(usernames, token);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public boolean ban (int site, String username)
    {
        try {
            return _delegate.ban(site, username);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public boolean unban (int site, String username, boolean untaint)
    {
        try {
            return _delegate.unban(site, username, untaint);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public void addBannedIdent (String machIdent, int siteId)
    {
        try {
            _delegate.addBannedIdent(machIdent, siteId);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public void removeBannedIdent (String machIdent, int siteId)
    {
        try {
            _delegate.removeBannedIdent(machIdent, siteId);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public void addTaintedIdent (String machIdent)
    {
        try {
            _delegate.addTaintedIdent(machIdent);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    // from interface SupportRepository
    public void removeTaintedIdent (String machIdent)
    {
        try {
            _delegate.removeTaintedIdent(machIdent);
        } catch (PersistenceException pe) {
            throw new RuntimeException(pe);
        }
    }

    protected OOOUserRepository _delegate;
}
