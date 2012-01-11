//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.Comparators;
import com.samskivert.util.Tuple;

import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.User;
import com.samskivert.servlet.user.UserRepository;

import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.depot.PersistenceContext;

import com.threerings.sling.server.persist.DepotUserSupportRepository;
import com.threerings.sling.server.persist.OOOUserSupportRepository;
import com.threerings.sling.server.persist.UserSupportRepository;
import com.threerings.sling.web.client.SlingException;
import com.threerings.sling.web.data.Account;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.MachineIdentity;
import com.threerings.sling.web.data.UniversalTime;
import com.threerings.sling.web.util.CacheEntry;
import com.threerings.sling.web.util.CacheEntryMap;
import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserCard;
import com.threerings.user.OOOUserManager;
import com.threerings.user.depot.DepotUserManager;
import com.threerings.user.depot.DepotUserRepository;

import static com.threerings.sling.Log.log;

/**
 * Provides user-related logic neeed by the servlet.
 */
public class UserLogic
{
    /** Used to manage authentication. */
    public static class Caller
    {
        public String authtok;
        public String username;
        public String email;
        public boolean isSupport;
    }

    /**
     * Defines the method for starting a new user session. Normally defers to a user manager, which
     * is responsible for authentication.
     */
    public interface LoginHandler
    {
        /**
         * Authenticates and creates a session for the the given username and password. The user
         * object is returned with an authentication token in a tuple. The token is valid for the
         * requested number of days. The client should store the token and pass it with subsequent
         * requests.
         * @see UserLogic#loadCaller()
         * @return tuple with the user that just logged in and the session token
         */
        Tuple<OOOUser,String> login (String username, String password, int expireDays)
            throws AuthenticationFailedException, InvalidPasswordException;
    }

    /**
     * Create a new user logic that will defer to a ooo user database for persistence.
     * @see OOOUserSupportRepository
     * @see OOOUserManager
     */
    public static UserLogic newOOOUserLogic (ConnectionProvider conprov, Properties oooconf)
    {
        try {
            final OOOUserSupportRepository repo = new OOOUserSupportRepository(conprov);
            final OOOUserManager mgr = new OOOUserManager(oooconf, conprov) {
                @Override protected UserRepository createRepository (ConnectionProvider conprov)
                    throws PersistenceException {
                    return repo.getDelegate();
                }
            };
            LoginHandler loginHandler = new LoginHandler() {
                @Override public Tuple<OOOUser,String> login (
                        String username, String password, int expireDays)
                    throws AuthenticationFailedException, InvalidPasswordException
                {
                    try {
                        Tuple<User, String> bits = mgr.login(username,
                            Password.makeFromCrypto(password), expireDays,
                            OOOUserManager.AUTH_PASSWORD);
                        return new Tuple<OOOUser, String>((OOOUser)bits.left, bits.right);
                    } catch (PersistenceException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            return new UserLogic(repo, loginHandler);

        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new user logic that will defer to a depot user database for persistence.
     * @see DepotUserSupportRepository
     * @see DepotUserManager
     */
    public static UserLogic newDepotUserLogic (PersistenceContext pctx, Properties oooconf)
    {
        final DepotUserSupportRepository repo = new DepotUserSupportRepository(pctx);
        final DepotUserManager mgr = new DepotUserManager(oooconf, pctx) {
            @Override protected DepotUserRepository createRepository (PersistenceContext pctx) {
                return repo.getDelegate();
            }
        };
        LoginHandler loginHandler = new LoginHandler() {
            @Override public Tuple<OOOUser,String> login (
                    String username, String password, int expireDays)
                throws AuthenticationFailedException, InvalidPasswordException
            {
                return mgr.login(username, Password.makeFromCrypto(password),
                    expireDays, DepotUserManager.AUTH_PASSWORD);
            }
        };
        return new UserLogic(repo, loginHandler);
    }

    /**
     * Creates a new user logic using the given repo and login handler.
     */
    public UserLogic (UserSupportRepository supportRepo, LoginHandler loginHandler)
    {
        _supportrepo = supportRepo;
        _loginHandler = loginHandler;
    }

    public void init (SlingEnvironment ctx)
    {
        _ctx = ctx;
    }

    public Caller loadCaller (String authtok)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUserBySession(authtok);
        if (user == null) {
            return null;
        }

        Caller caller = new Caller();
        caller.authtok = authtok;
        caller.username = getUsername(user.username);
        caller.email = user.email;
        caller.isSupport = user.isSupportPlus();
        return caller;
    }

    public Caller userLogin (String username, String password, int expireDays)
        throws AuthenticationFailedException, InvalidPasswordException
    {
        Tuple<OOOUser, String> bits = _loginHandler.login(username, password, expireDays);
        Caller caller = new Caller();
        caller.authtok = bits.right;
        caller.username = bits.left.username;
        caller.email = bits.left.email;
        caller.isSupport = bits.left.isSupportPlus();
        return caller;
    }

    public String getSupportUsername (String accountName)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUserByAccountName(accountName);
        return (user == null) ? null : getUsername(user);
    }

    public boolean refreshSession (String sessionKey, int expireDays)
    {
        return _supportrepo.refreshSession(sessionKey, expireDays);
    }

    public void updateEmail (Caller caller, String email)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUserByAccountName(caller.username);
        if (user == null) {
            throw new SlingException("m.internal_error");
        }
        setUserEmail(user, email);
        _supportrepo.updateUser(user);
    }

    public Account getAccountByName (int siteId, String accountName)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUserByAccountName(accountName);
        return (user == null) ? null : toAccount(siteId, user);
    }

    public Account getAccount (int siteId, String name)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUser(name, false);
        if (user == null) {
            return null;
        }
        return toAccount(siteId, user);
    }

    public Account getAccountById (int siteId, int userId)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUser(userId);
        if (user == null) {
            return null;
        }
        return toAccount(siteId, user);
    }

    public List<MachineIdentity> getRelatedAccounts (int siteId, int accountId)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUser(accountId);
        if (user == null) {
            throw new SlingException("m.unknown_user");
        }
        _supportrepo.loadMachineIdents(user);

        if (user.machIdents.length == 0) {
            return Lists.newArrayList();
        }

        // determine which are tainted
        Collection<String> tainted = _supportrepo.filterTaintedIdents(user.machIdents);

        // determine which are banned
        Collection<String> banned = _supportrepo.filterBannedIdents(user.machIdents, siteId);

        // collect all account names and a list of related accounts for each ident
        Set<String> names = new HashSet<String>();
        Map<String, List<OOOUserCard>> cardsForIdents = Maps.newHashMap();
        for (String id : user.machIdents) {
            List<OOOUserCard> idNames = Lists.newArrayList(_supportrepo.getUsersOfMachIdent(id));
            cardsForIdents.put(id, idNames);
            names.addAll(Lists.transform(idNames, OOOUserCard.TO_USERNAME));
        }

        // get banned names
        byte bannedToken = OOOUser.getBannedToken(siteId);
        Set<String> bannedNames = Collections.emptySet();
        if (bannedToken > 0) {
            bannedNames = Sets.newHashSet(_supportrepo.getTokenUsernames(names, bannedToken));
        }

        // convert games names to "usernames"
        // TODO: this is confusing, do we need the concept of usernames?
        HashSet<String> usernames = new HashSet<String>();
        for (String name : names) {
            String username = getUsername(name);
            if (username != null) {
                usernames.add(username);
            }
        }

        // now get games names for all usernames
        Map<String, AccountName> accountNamesForUsernames = resolveNames(usernames);

        // coallate it all into a nice tidy list of MachineIdentity
        // Beware, this is complicated by names != usernames
        List<MachineIdentity> relations = Lists.newArrayListWithCapacity(user.machIdents.length);
        for (String id : user.machIdents) {
            MachineIdentity ident = new MachineIdentity(
                id, tainted.contains(id), banned.contains(id));
            List<OOOUserCard> idCards = cardsForIdents.get(id);
            ident.accounts = Lists.newArrayListWithCapacity(idCards.size());
            for (OOOUserCard card : idCards) {
                MachineIdentity.AccountInfo info = new MachineIdentity.AccountInfo();
                info.name = accountNamesForUsernames.get(getUsername(card.username));
                if (info.name == null) {
                    log.warning("Unable to resolve related account",
                        "accountId", accountId, "name", card.username);
                    continue;
                }
                info.banned = bannedNames.contains(card.username);
                info.paid = (card.flags & OOOUSER_PAID_MASK) != 0;
                ident.accounts.add(info);
            }
            relations.add(ident);
        }

        return relations;
    }

    public void updateAccount (int accountId, String email, String password)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUser(accountId);
        if (user == null) {
            throw new SlingException("m.unknown_user");
        }

        if (email != null) {
            if (!MailUtil.isValidAddress(email)) {
                throw new SlingException("m.invalid_email");
            }
            setUserEmail(user, email);
        }
        if (password != null) {
            user.setPassword(Password.makeFromCrypto(password));
        }
        if (!_supportrepo.updateUser(user)) {
            throw new SlingException("m.user_update_error");
        }
    }

    public Account updateBanned (int siteId, int accountId, boolean banned, String reason,
                                 boolean untaintIdents)
        throws SlingException
    {
        OOOUser user = _supportrepo.loadUser(accountId);
        if (user == null) {
            throw new SlingException("m.unknown_user");
        }

        Account account = toAccount(siteId, user);
        boolean updated;
        if (banned) {
            updated = _supportrepo.ban(siteId, user.username);
        } else {
            updated = _supportrepo.unban(siteId, user.username, untaintIdents);
        }

        // the only way we could fail here is if we're trying to ban someone on a site for which we
        // do not support banning, so freak out
        if (!updated) {
            throw new SlingException("m.internal_error");
        }

        return account;
    }

    public AccountName[] findAccountsByEmail (String query)
        throws SlingException
    {
        // build a map of matching emails to account names
        HashMap<String, String> names = Maps.newHashMap();
        for (String username : _supportrepo.getUsernames(query)) {
            names.put(username, getUsername(username)); // may be null-valued
        }

        // now resolve to account name structures
        Map<String, AccountName> resolved = resolveNames(Sets.newHashSet(names.values()));

        // for each matching email address, output an AccountName
        AccountName[] accnames = new AccountName[names.size()];
        int idx = 0;
        for (Map.Entry<String, String> pair : names.entrySet()) {
            AccountName accName = resolved.get(pair.getValue());
            accnames[idx++] = accName == null ?
                new AccountName(pair.getKey(), "[deleted?]") : accName;
        }
        return accnames;
    }

    public void updateIdentBanned (int siteId, String machIdent, boolean banned)
    {
        if (banned) {
            _supportrepo.addBannedIdent(machIdent, siteId);
        } else {
            _supportrepo.removeBannedIdent(machIdent, siteId);
        }
    }

    public void updateIdentTainted (int siteId, String machIdent, boolean tainted)
    {
        if (tainted) {
            _supportrepo.addTaintedIdent(machIdent);
        } else {
            _supportrepo.removeTaintedIdent(machIdent);
        }
    }

    /**
     * Resolves the game names for the given set of account names and returns a mapping.
     * @param accounts set of account names to resolve
     * @return mapping of all non-null entries in account to {@link AccountName} objects
     */
    public Map<String, AccountName> resolveNames (Set<String> accounts)
    {
        // remove old names before we start
        _names.removeOld(NAME_REFRESH_INTERVAL);

        accounts = Sets.filter(accounts, Predicates.notNull());
        Set<String> toResolve = Sets.difference(accounts, _names.keySet());

        if (toResolve.size() > 0) {
            ArrayListMultimap<String, String> gameNames = ArrayListMultimap.create();
            _ctx.info.resolveGameNames(toResolve, gameNames);

            for (String account : toResolve) {
                List<String> gameNamesEntry = Lists.newArrayList(gameNames.get(account));
                Collections.sort(gameNamesEntry, _sortGameNames);
                _names.put(account, CacheEntry.wrap(new AccountName(account, gameNamesEntry)));
            }
        }

        Map<String, AccountName> resolved = Maps.newHashMap();
        for (String account : accounts) {
            AccountName accName = _names.getUnwrappedValue(account);
            resolved.put(account, accName == null ? new AccountName(account) : accName);
        }
        return resolved;
    }

    public AccountName resolveName (String username)
    {
        return resolveNames(Collections.singleton(username)).get(username);
    }

    /**
     * Ensures that we will not return the given character name in a subsequent query without
     * hitting the db.
     */
    public void invalidateCharacterName (String oldName)
    {
        for (Iterator<AccountName> iter = _names.unwrappedValues().iterator(); iter.hasNext();) {
            AccountName acct = iter.next();
            if (acct.gameNames.contains(oldName)) {
                iter.remove();
                break;
            }
        }
    }

    /**
     * Converts the supplied OOOUser record into an Account record.
     */
    protected Account toAccount (int siteId, OOOUser user)
        throws SlingException
    {
        String username = getUsername(user);
        resolveName(username);

        // populate the standard bits
        Account account = createAccount();
        account.accountId = user.userId;
        account.name = resolveName(username);
        account.email = user.email;
        account.created = UniversalTime.fromDate(user.created);
        account.affiliate = _ctx.site.getSiteString(user.siteId);

        account.set(Account.Flag.HAS_BOUGHT_COINS, user.hasBoughtCoins());
        account.set(Account.Flag.HAS_BOUGHT_TIME, user.hasBoughtTime());
        account.set(Account.Flag.FAMILY_SUBSCRIBER, user.isFamilySubscriber());
        account.set(Account.Flag.ADMIN, user.isAdmin());
        account.set(Account.Flag.MAINTAINER, user.isMaintainer());
        account.set(Account.Flag.INSIDER, user.isInsider());
        account.set(Account.Flag.TESTER, user.holdsToken(OOOUser.TESTER));
        account.set(Account.Flag.SUPPORT, user.isSupport());
        account.set(Account.Flag.BIG_SPENDER, user.isBigSpender());
        account.set(Account.Flag.BANNED, user.isBanned(siteId));
        account.set(Account.Flag.DEADBEAT, user.isDeadbeat(siteId));

        switch (user.getBillingStatus(siteId)) {
        case OOOUser.TRIAL_STATE:
        default:
            account.billingSatus = Account.BillingStatus.TRIAL;
            break;
        case OOOUser.SUBSCRIBER_STATE:
            account.billingSatus = Account.BillingStatus.SUBSCRIBER;
            break;
        case OOOUser.BILLING_FAILURE_STATE:
            account.billingSatus = Account.BillingStatus.FAILURE;
            break;
        case OOOUser.EX_SUBSCRIBER_STATE:
            account.billingSatus = Account.BillingStatus.EX_SUBSCRIBER;
            break;
        case OOOUser.BANNED_STATE:
            account.billingSatus = Account.BillingStatus.BANNED;
            break;
        }

        // populate the game specific bits
        _ctx.info.populateAccount(account);

        // TODO: cache the results?

        return account;
    }

    protected Account createAccount ()
    {
        return new Account();
    }

    /**
     * Returns the username for this user.  Overriden in cases where the username isn't the
     * standard value in OOOUser. May return null if the user could not be found for some reason.
     */
    protected String getUsername (OOOUser user)
        throws SlingException
    {
        return user.username;
    }

    /**
     * Returns the username for this user.  Overriden in cases where the username isn't the
     * standard value in OOOUser, in which case it may also return null in cases when the user
     * no longer exists from the point of view of the subclasser.
     */
    protected String getUsername (String username)
        throws SlingException
    {
        return username;
    }

    /**
     * Sets the given record to use the given username. Overridden in cases where the setting of
     * the name may have other side effects within the passed record. Callers must treat all fields
     * as potentially modified after this call.
     */
    protected void setUserEmail (OOOUser user, String email)
    {
        user.setEmail(email);
    }

    protected Comparator<String> _sortGameNames = new Comparator<String>() {
        @Override public int compare (String gn1, String gn2) {
            int cmp = Comparators.compare(
                    _ctx.info.isDeleted(gn1), _ctx.info.isDeleted(gn2));
            return cmp == 0 ? gn1.compareTo(gn2) : cmp;
        }
    };

    protected SlingEnvironment _ctx;

    /** A cache of resolved account names. */
    protected CacheEntryMap<String, AccountName> _names = CacheEntryMap.makeNew();

    protected LoginHandler _loginHandler;

    protected UserSupportRepository _supportrepo;

    protected static final int OOOUSER_PAID_MASK =
        OOOUser.HAS_BOUGHT_COINS_FLAG | OOOUser.HAS_BOUGHT_TIME_FLAG;

    /** Refresh our cached namess every five minutes. */
    protected static final long NAME_REFRESH_INTERVAL = 5 * 60 * 1000L;
}
