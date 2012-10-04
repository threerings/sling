//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Tuple;

import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.Password;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.PersistenceContext;

import com.threerings.sling.web.client.SlingException;
import com.threerings.sling.web.data.Account;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.MachineIdentity;
import com.threerings.sling.web.data.UniversalTime;
import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserCard;
import com.threerings.user.depot.DepotUserManager;
import com.threerings.user.depot.DepotUserRepository;

import static com.threerings.sling.Log.log;

/**
 * Provides user-related logic neeed by the servlet.
 */
@Singleton
public class UserLogic
{
    /** Used to manage authentication. */
    public static class Caller
    {
        public String authtok;
        public String username;
        public String email;
        public boolean isJrSupport;
        public boolean isSupport;
        public boolean isAdmin;
        public boolean isMaintainer;
    }

    /**
     * Creates a new user logic. Note that the application must bind the named properties to a
     * suitable instance.
     */
    @Inject public UserLogic (final @Named("com.threerings.oooauth") Properties conf,
        final PersistenceContext pctx)
    {
        // this needs to be a supplier so we can properly nest our _userRepo in there later
        // TODO: make ooo-user lib guice compatible? this code is bending over backwards to \
        // "inject" by overriding a method (furthermore the method is named "create", which \
        // is not what its override is doing).
        _userMgr = new Supplier<DepotUserManager> () {
            DepotUserManager mgr;
            @Override public DepotUserManager get () {
                if (mgr == null) {
                    mgr = new DepotUserManager(conf, pctx) {
                        @Override protected DepotUserRepository createRepository (
                                PersistenceContext pctx) throws DatabaseException {
                            // not creating! using an injected instance from parent class
                            return _userRepo;
                        }
                    };
                }
                return mgr;
            }
        };
    }

    public Caller loadCaller (String authtok)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUserBySession(authtok);
        if (user == null) {
            return null;
        }

        return createCaller(user, authtok);
    }

    public Caller userLogin (String username, String password, int expireDays)
        throws AuthenticationFailedException, InvalidPasswordException, SlingException
    {
        Tuple<OOOUser, String> bits = _userMgr.get().login(username,
            Password.makeFromCrypto(password), expireDays, DepotUserManager.AUTH_PASSWORD);
        return createCaller(bits.left, bits.right);
    }

    public String getSupportUsername (String accountName)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(accountName, false);
        return (user == null) ? null : getUsername(user);
    }

    public boolean refreshSession (String sessionKey, int expireDays)
    {
        return _userRepo.refreshSession(sessionKey, expireDays);
    }

    public void updateEmail (Caller caller, String email)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(caller.username, false);
        if (user == null) {
            throw new SlingException("m.internal_error");
        }
        setUserEmail(user, email);
        _userRepo.updateUser(user);
    }

    public Account getAccountByName (int siteId, String accountName)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(accountName, false);
        return (user == null) ? null : toAccount(siteId, user);
    }

    public Account getAccount (int siteId, String name)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(name, false);
        if (user == null) {
            return null;
        }
        return toAccount(siteId, user);
    }

    public Account getAccountById (int siteId, int userId)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(userId);
        if (user == null) {
            return null;
        }
        return toAccount(siteId, user);
    }

    public List<MachineIdentity> getRelatedAccounts (int siteId, int accountId)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(accountId);
        if (user == null) {
            throw new SlingException("m.unknown_user");
        }
        _userRepo.loadMachineIdents(user);

        if (user.machIdents.length == 0) {
            return Lists.newArrayList();
        }

        // determine which are tainted
        Collection<String> tainted = _userRepo.filterTaintedIdents(user.machIdents);

        // determine which are banned
        Collection<String> banned = _userRepo.filterBannedIdents(user.machIdents, siteId);

        // collect all account names and a list of related accounts for each ident
        Set<String> names = Sets.newHashSet();
        Map<String, List<OOOUserCard>> cardsForIdents = Maps.newHashMap();
        for (String id : user.machIdents) {
            List<OOOUserCard> idNames = Lists.newArrayList(_userRepo.getUsersOfMachIdentCards(id));
            cardsForIdents.put(id, idNames);
            names.addAll(Lists.transform(idNames, OOOUserCard.TO_USERNAME));
        }

        // get banned names
        byte bannedToken = OOOUser.getBannedToken(siteId);
        Set<String> bannedNames = (bannedToken > 0)
            ? Sets.newHashSet(_userRepo.getTokenUsernames(names, bannedToken))
            : ImmutableSet.<String>of();

        // convert games names to "usernames"
        // TODO: this is confusing, do we need the concept of usernames?
        Set<String> usernames = Sets.newHashSet();
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
        OOOUser user = _userRepo.loadUser(accountId);
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
        if (!_userRepo.updateUser(user)) {
            throw new SlingException("m.user_update_error");
        }
    }

    public Account updateBanned (int siteId, int accountId, boolean banned, String reason,
                                 boolean untaintIdents)
        throws SlingException
    {
        OOOUser user = _userRepo.loadUser(accountId);
        if (user == null) {
            throw new SlingException("m.unknown_user");
        }

        Account account = toAccount(siteId, user);
        boolean updated;
        if (banned) {
            updated = _userRepo.ban(siteId, user.username);
        } else {
            updated = _userRepo.unban(siteId, user.username, untaintIdents);
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
        Map<String, String> names = Maps.newHashMap();
        for (String username : _userRepo.getUsernames(query)) {
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
            _userRepo.addBannedIdent(machIdent, siteId);
        } else {
            _userRepo.removeBannedIdent(machIdent, siteId);
        }
    }

    public void updateIdentTainted (int siteId, String machIdent, boolean tainted)
    {
        if (tainted) {
            _userRepo.addTaintedIdent(machIdent);
        } else {
            _userRepo.removeTaintedIdent(machIdent);
        }
    }

    public void updateFlags (String accountName, int setFlags, int clearFlags, int siteId)
    {
        OOOUser user = _userRepo.loadUser(accountName, false);
        if (user == null) {
            log.warning("no user found to update flags");
            return;
        }
        updateToken(user, setFlags, clearFlags, Account.Flag.MAINTAINER.mask(), OOOUser.MAINTAINER);
        updateToken(user, setFlags, clearFlags, Account.Flag.ADMIN.mask(), OOOUser.ADMIN);
        updateToken(user, setFlags, clearFlags, Account.Flag.SUPPORT.mask(), OOOUser.SUPPORT);
        updateToken(user, setFlags, clearFlags, Account.Flag.JR_SUPPORT.mask(), OOOUser.JR_SUPPORT);
        updateToken(user, setFlags, clearFlags, Account.Flag.INSIDER.mask(), OOOUser.INSIDER);
        updateToken(user, setFlags, clearFlags, Account.Flag.TESTER.mask(), OOOUser.TESTER);
        updateToken(user, setFlags, clearFlags, Account.Flag.DEADBEAT.mask(),
                OOOUser.getDeadbeatToken(siteId));
        _userRepo.updateUser(user);
    }

    protected void updateToken (OOOUser user, int setFlags, int clearFlags, int mask, byte token)
    {
        if ((setFlags & mask) != 0) {
            user.addToken(token);
        } else if ((clearFlags & mask) != 0) {
            user.removeToken(token);
        }
    }

    /**
     * Resolves the game names for the given set of account names and returns a mapping.
     * @param accounts set of account names to resolve
     * @return mapping of all non-null entries in account to {@link AccountName} objects
     */
    public Map<String, AccountName> resolveNames (Set<String> accounts)
    {
        accounts = Sets.filter(accounts, Predicates.notNull());
        Set<String> toResolve = Sets.difference(accounts, _names.asMap().keySet());

        if (!toResolve.isEmpty()) {
            ArrayListMultimap<String, String> gameNames = ArrayListMultimap.create();
            _infoProvider.resolveGameNames(toResolve, gameNames);

            for (String account : toResolve) {
                _names.put(account,
                    new AccountName(account, _sortGameNames.sortedCopy(gameNames.get(account))));
            }
        }

        Map<String, AccountName> resolved = Maps.newHashMap();
        for (String account : accounts) {
            AccountName accName = _names.getIfPresent(account);
            resolved.put(account, accName == null ? new AccountName(account) : accName);
        }
        return resolved;
    }

    public AccountName resolveName (String username)
    {
        return resolveNames(ImmutableSet.of(username)).get(username);
    }

    /**
     * Ensures that we will not return the given character name in a subsequent query without
     * hitting the db.
     */
    public void invalidateCharacterName (String oldName)
    {
        for (Iterator<AccountName> iter = _names.asMap().values().iterator(); iter.hasNext();) {
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
        account.affiliate = _siteIdentifier.getSiteString(user.siteId);

        account.set(Account.Flag.HAS_BOUGHT_COINS, user.hasBoughtCoins());
        account.set(Account.Flag.HAS_BOUGHT_TIME, user.hasBoughtTime());
        account.set(Account.Flag.FAMILY_SUBSCRIBER, user.isFamilySubscriber());
        account.set(Account.Flag.ADMIN, user.holdsToken(OOOUser.ADMIN));
        account.set(Account.Flag.MAINTAINER, user.holdsToken(OOOUser.MAINTAINER));
        account.set(Account.Flag.INSIDER, user.holdsToken(OOOUser.INSIDER));
        account.set(Account.Flag.TESTER, user.holdsToken(OOOUser.TESTER));
        account.set(Account.Flag.SUPPORT, user.holdsToken(OOOUser.SUPPORT));
        account.set(Account.Flag.JR_SUPPORT, user.holdsToken(OOOUser.JR_SUPPORT));
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
        _infoProvider.populateAccount(account);

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

    /**
     * Creates a caller from a user and authentication token.
     */
    protected Caller createCaller (OOOUser user, String authtok)
        throws SlingException
    {
        Caller caller = new Caller();
        caller.authtok = authtok;
        caller.username = getUsername(user.username);
        caller.email = user.email;
        caller.isSupport = user.isSupportPlus();
        caller.isJrSupport = user.isSupportPlus() || user.holdsToken(OOOUser.JR_SUPPORT);
        caller.isAdmin = user.isAdmin();
        caller.isMaintainer = user.isMaintainer();
        return caller;
    }

    protected Ordering<String> _sortGameNames = new Ordering<String>() {
        @Override public int compare (String gn1, String gn2) {
            return ComparisonChain.start()
                .compareFalseFirst(_infoProvider.isDeleted(gn1), _infoProvider.isDeleted(gn2))
                .compare(gn1, gn2)
                .result();
        }
    };

    @Inject protected GameInfoProvider _infoProvider;
    @Inject protected DepotUserRepository _userRepo;
    @Inject protected SiteIdentifier _siteIdentifier;

    protected Supplier<DepotUserManager> _userMgr;

    /** A cache of resolved account names. */
    protected Cache<String, AccountName> _names = CacheBuilder.newBuilder()
        .expireAfterWrite(NAME_REFRESH_INTERVAL, TimeUnit.MILLISECONDS)
        .build();

    protected static final int OOOUSER_PAID_MASK =
        OOOUser.HAS_BOUGHT_COINS_FLAG | OOOUser.HAS_BOUGHT_TIME_FLAG;

    /** Refresh our cached namess every five minutes. */
    protected static final long NAME_REFRESH_INTERVAL = 5 * 60 * 1000L;
}
