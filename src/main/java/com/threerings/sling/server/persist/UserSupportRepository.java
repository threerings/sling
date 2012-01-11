//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.util.Collection;
import java.util.List;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserCard;

/**
 * The required functionality of a UserRepository for use with sling.
 */
public interface UserSupportRepository
{
    public OOOUser loadUser (int userId);
    public OOOUser loadUser (String username, boolean loadIdents);
    public OOOUser loadUserByAccountName (String accountName);
    public boolean updateUser (OOOUser user);
    public void loadMachineIdents (OOOUser user);
    public String[] getUsernames (String email);
    public Collection<String> filterTaintedIdents (String[] idents);
    public Collection<String> filterBannedIdents (String[] idents, int siteId);
    public List<OOOUserCard> getUsersOfMachIdent (String machIdent);
    public List<String> getTokenUsernames (Collection<String> usernames, byte token);
    public boolean ban (int site, String username);
    public boolean unban (int site, String username, boolean untaint);
    public void addBannedIdent (String machIdent, int siteId);
    public void removeBannedIdent (String machIdent, int siteId);
    public void addTaintedIdent (String machIdent);
    public void removeTaintedIdent (String machIdent);
    public OOOUser loadUserBySession (String sessionKey);
    public boolean refreshSession (String sessionKey, int expireDays);
}
