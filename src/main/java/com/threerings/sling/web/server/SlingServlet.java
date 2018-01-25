//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.server;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.google.inject.Inject;

import com.samskivert.net.MailUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.Config;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.NoSuchUserException;
import com.samskivert.servlet.util.CookieUtil;
import com.samskivert.servlet.user.Username;

import com.threerings.gwt.util.PagedRequest;
import com.threerings.gwt.util.PagedResult;
import com.threerings.jsp.taglib.i18n.DefaultLocaleFilter;
import com.threerings.sling.server.GameActionHandler;
import com.threerings.sling.server.GameInfoProvider;
import com.threerings.sling.server.UserLogic.Caller;
import com.threerings.sling.server.UserLogic;
import com.threerings.sling.server.persist.CategoryRecord;
import com.threerings.sling.server.persist.EventRecord;
import com.threerings.sling.server.persist.MessageRecord;
import com.threerings.sling.server.persist.QuestionRecord;
import com.threerings.sling.server.persist.SlingRepository;
import com.threerings.sling.web.client.AuthenticationException;
import com.threerings.sling.web.client.SlingException;
import com.threerings.sling.web.client.SlingService;
import com.threerings.sling.web.data.Account;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.Category;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventFilter;
import com.threerings.sling.web.data.EventSearch;
import com.threerings.sling.web.data.MachineIdentity.AccountInfo;
import com.threerings.sling.web.data.MachineIdentity;
import com.threerings.sling.web.data.Message;
import com.threerings.sling.web.data.Question;
import com.threerings.sling.web.data.TimeRange;
import com.threerings.sling.web.data.UserPetition;
import com.threerings.sling.web.util.SimpleCache;
import com.threerings.user.OOOUser;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import static com.threerings.sling.Log.log;

/**
 * Handles requests from the GWT web application.
 */
public abstract class SlingServlet extends RemoteServiceServlet
    implements SlingService
{
    // from RemoteServiceServlet
    @Override public void init (ServletConfig config)
        throws ServletException
    {
        super.init(config);
        log.info("Sling servlet initialized", "tz", TimeZone.getDefault());
        _msgs = _msgmgr.getBundle("sling");
    }

    // from SlingService
    @Override public AuthInfo login (String username, String passwordHash)
        throws SlingException
    {
        try {
            Caller caller = _userLogic.userLogin(username, passwordHash, SESSION_EXPIRY_DAYS);
            _callers.put(caller.authtok, caller);
            Cookie cookie = new Cookie(getSessionCookieName(), caller.authtok);
            cookie.setPath("/");
            cookie.setMaxAge(SESSION_EXPIRY_DAYS * 24 * 60 * 60);
            getThreadLocalResponse().addCookie(cookie);

            // construct an AuthInfo record with their bits
            return createAuthInfo(caller);

        } catch (NoSuchUserException nsue) {
            throw new AuthenticationException("m.unknown_user");

        } catch (InvalidPasswordException ipe) {
            throw new AuthenticationException("m.invalid_password");

        } catch (AuthenticationFailedException afe) {
            throw new SlingException("m.internal_error");
        }
    }

    // from SlingService
    @Override public AuthInfo validateSession ()
        throws SlingException
    {
        AuthInfo info;
        if (!_userLogic.refreshSession(getAuthTok(), SESSION_EXPIRY_DAYS)) {
            String redirect = getLoginRedirectUrl();
            if (redirect == null) {
                throw new SlingException("m.session_expired");
            }
            // return a special AuthInto here to let the client know where to redirect
            // (we can't send one in the response because AJAX reports that as a failure)
            info = new AuthInfo();
            info.setUrl(AuthUrl.REDIRECT, redirect);
        } else {
            // construct an AuthInfo record with their bits
            info = createAuthInfo(requireAuthedUser());
        }
        return info;
    }

    // from SlingService
    @Override public String logout ()
        throws SlingException
    {
        String authtok = getAuthTok();
        if (authtok != null) {
            _callers.remove(authtok);
            CookieUtil.clearCookie(getThreadLocalResponse(), getSessionCookieName());
            // TODO: make _userLogic.deleteSession and call it
        }
        return getLogoutRedirectUrl();
    }

    // from SlingService
    @Override public void updateEmail (String email)
        throws SlingException
    {
        if (!allowEmailUpdate()) {
            throw new SlingException("m.internal_error");
        }

        Caller caller = requireAuthedUser();

        if (!MailUtil.isValidAddress(email)) {
            throw new SlingException("m.invalid_email");
        }
        _userLogic.updateEmail(caller, email);
    }

    // from SlingService
    @Override public Account createSupportAccount (String name, String password, String email)
        throws SlingException
    {
        requireAuthedAdmin();

        Username username;
        try {
            username = new Username(name);
        } catch (Exception e) {
            throw new SlingException(e.getMessage());
        }

        return _userLogic.createSupportAccount(username, password, email);
    }

    // from SlingService
    @Override public Account getAccountByName (String accountName)
        throws SlingException
    {
        requireAuthedSupport();
        return _userLogic.getAccountByName(getSiteId(), accountName);
    }

    // from SlingService
    @Override public Account getAccount (String name)
        throws SlingException
    {
        requireAuthedSupport();
        return _userLogic.getAccount(getSiteId(), name);
    }

    // from SlingService
    @Override public List<MachineIdentity> getRelatedAccounts (int accountId)
        throws SlingException
    {
        requireAuthedSupport();

        // check the cache
        return _relatedAccounts.get(accountId);
    }

    // from SlingService
    @Override public void updateAccount (int accountId, String email, String password)
        throws SlingException
    {
        requireAuthedSupport();

        if (!allowEmailUpdate() && email != null) {
            throw new SlingException("m.internal_error");
        }
        _userLogic.updateAccount(accountId, email, password);
    }

    // from SlingService
    @Override public void updateBanned (int accountId, boolean banned, String reason,
                              boolean untaintIdents)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();

        Account account = _userLogic.updateBanned(
            getSiteId(), accountId, banned, reason, untaintIdents);
        if (banned) {
            _actionHandler.ban(account.name.accountName);
            _actionHandler.warn(account.name.accountName, reason);
            recordEvent(Event.Type.SUPPORT_ACTION, caller.username, account.name.accountName,
                _msgs.get("m.banned", reason));
        } else {
            _actionHandler.warn(account.name.accountName, null);
            recordEvent(Event.Type.SUPPORT_ACTION, caller.username, account.name.accountName,
                _msgs.get("m.unbanned"));
        }
        updateRelatedAccounts(account, banned);
    }

    // from SlingService
    @Override public void updateTempBan (String accountName, int days, String warning)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();

        if (days == 0) {
            _actionHandler.warn(accountName, null);
        } else {
            Calendar expires = Calendar.getInstance();
            expires.add(Calendar.DAY_OF_MONTH, days);
            _actionHandler.tempBan(
                accountName, new Timestamp(expires.getTimeInMillis()), warning);
        }

        // record the temp ban
        if (days == 0) {
            recordEvent(Event.Type.SUPPORT_ACTION, caller.username, accountName,
                _msgs.get("m.cleared_temp_ban"));
        } else {
            recordEvent(Event.Type.SUPPORT_ACTION, caller.username, accountName,
                _msgs.get("m.temp_ban", days, warning));
        }
    }

    // from SlingService
    @Override public void updateWarning (String accountName, String warning)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();

        _actionHandler.warn(accountName, warning);

        // record the warning
        if (warning == null) {
            recordEvent(Event.Type.SUPPORT_ACTION, caller.username,
                accountName, _msgs.get("m.cleared_warning"));
        } else {
            String reason = "m.warned";
            Account account = _userLogic.getAccountByName(getSiteId(), accountName);
            if (account != null) {
                if (account.isSet(Account.Flag.BANNED)) {
                    reason = "m.changed_banned_reason";
                } else if (account.tempBan != null) {
                    reason = "m.changed_temp_ban_reason";
                } else if (!StringUtil.isBlank(account.warning)) {
                    reason = "m.updated_warning";
                }
            }
            recordEvent(Event.Type.SUPPORT_ACTION, caller.username, accountName,
                _msgs.get(reason, warning));
        }
    }

    // from SlingService
    @Override public AccountName[] findAccountsByEmail (String query)
        throws SlingException
    {
        requireAuthedSupport();
        return _userLogic.findAccountsByEmail(query);
    }

    // from SlingService
    @Override public AccountName[] findAccountsByGameName (String query)
        throws SlingException
    {
        requireAuthedSupport();

        List<String> names = _infoProvider.lookupAccountNames(query);
        ArrayList<AccountName> accountNames = new ArrayList<AccountName>(names.size());
        for (String name : names) {
            accountNames.add(new AccountName(name, query));
        }
        return accountNames.toArray(new AccountName[accountNames.size()]);
    }

    // from SlingService
    @Override public List<UserPetition> loadPetitions ()
        throws SlingException
    {
        Caller caller = requireAuthedUser();
        HashIntMap<UserPetition> petitions = new HashIntMap<UserPetition>();

        // first load up and convert their petitions
        for (EventRecord evrec : _slingRepo.loadPetitions(caller.username)) {
            petitions.put(evrec.eventId, evrec.toUserPetition());
        }

        // next load up all messages for those petitions
        if (petitions.size() > 0) {
            Collection<MessageRecord> msgrecs = _slingRepo.loadMessages(
                    Message.Access.NORMAL, petitions.keySet());
            Map<String, AccountName> names = resolveNames(msgrecs);

            // now convert the message records into
            for (MessageRecord msgrec : msgrecs) {
                UserPetition petition = petitions.get(msgrec.eventId);
                Message message = msgrec.toMessage(names);
                if (message.author != null) {
                    // these are going to normal users so convert the author info to a handle
                    message.author = toHandle(message.author);
                }
                petition.messages.add(message);
            }
        }

        List<UserPetition> result = Lists.newArrayList(petitions.values());
        Collections.sort(result, new Comparator<UserPetition>() {
            public int compare (UserPetition one, UserPetition two) {
                return Longs.compare(two.entered, one.entered); // reversed
            }
        });
        return result;
    }

    // from SlingService
    @Override public int registerPetition (
        UserPetition petition, String gameName, String message)
        throws SlingException
    {
        Caller caller = requireAuthedUser();
        return registerPetitionRecord(caller.username, gameName, petition, message);
    }

    // from SlingService
    @Override public void registerAnonymousPetition (String email, UserPetition petition, String message)
        throws SlingException
    {
        registerPetitionRecord("", email, petition, message);
    }

    // from SlingService
    @Override public PagedResult<Event> loadEvents (
        Events criterion, String query, PagedRequest request)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();

        // let's not go nuts (until maybe some automated service is doing this)
        if (request.count == 0 || request.count > 1000) {
            request.count = 1000;
        }

        // load the matching records

        SlingRepository.PagedQuery<EventRecord> pagedQuery = null;
        switch (criterion) {
        default:
        case OPEN:
            pagedQuery = _slingRepo.loadOpenEvents();
            noteAgentActivity(caller.username);
            break;
        case MY:
            pagedQuery = _slingRepo.loadClaimedEvents(caller.username);
            break;
        case ALL:
            pagedQuery = _slingRepo.loadAllEvents();
            break;
        case ACCOUNT:
            // query is an account name in this case
            pagedQuery = _slingRepo.loadEvents(query);
            break;
        }

        return toResult(pagedQuery, request);
    }

    // from SlingService
    @Override public PagedResult<Event> searchEvents (
        EventSearch search, PagedRequest request)
        throws SlingException
    {
        requireAuthedSupport();

        if (request.count > 1000) {
            request.count = 1000;
        }

        // resolve owner id to a name
        for (int ii = 0; ii < search.filters.size(); ++ii) {
            EventFilter filter = search.filters.get(ii);
            if (filter.type == EventFilter.Type.OWNER_ID_IS) {
                int ownerId = filter.getOwnerId();
                Account owner = _userLogic.getAccountById(getSiteId(), ownerId);
                if (owner == null) {
                    throw new SlingException("m.no_such_user");
                }
                search.filters.set(ii, EventFilter.ownerIs(owner.name.accountName));
            }
        }

        SlingRepository.PagedQuery<EventRecord> query = _slingRepo.searchEvents(search);

        if (query == null) {
            throw new SlingException("m.invalid_search");
        }

        return toResult(query, request);
    }

    // from SlingService
    @Override public EventResponses checkResponseTimes (
        Event.Type type, TimeRange range, long firstResponseMillis)
        throws SlingException
    {
        requireAuthedSupport();

        EventResponses result = new EventResponses();
        EventSearch search = new EventSearch(
            EventFilter.createdIn(range),
            EventFilter.typeIs(type));
        result.total = _slingRepo.searchEvents(search).count();
        search.filters.add(EventFilter.firstResponseIsMoreThan(firstResponseMillis));
        result.qualified = result.total - _slingRepo.searchEvents(search).count();
        return result;
    }

    // from SlingService
    @Override public EventVolume getVolume (long now, TimeUnit timeUnit, int count)
        throws SlingException
    {
        requireAuthedSupport();

        Calendars.Builder cal = Calendars.at(now).in(TimeZone.getTimeZone("PST8PDT"));

        // translate time units and make sure now is on a boundary
        int calUnit;
        switch (timeUnit) {
        case HOUR:
            cal = cal.set(Calendar.MINUTE, 0).set(Calendar.SECOND, 0).set(Calendar.MILLISECOND, 0);
            calUnit = Calendar.HOUR;
            break;
        case DAY:
            cal = cal.zeroTime();
            calUnit = Calendar.DATE;
            break;
        default:
            throw new AssertionError();
        }

        // load volume in all requested intervals
        Calendar d1 = cal.add(calUnit, -count).asCalendar();
        Calendar d2 = Calendars.withCopy(d1).add(calUnit, 1).asCalendar();
        EventFilter userReported = EventFilter.typeIsIn(Sets.newHashSet(
            Event.Type.COMPLAINT, Event.Type.PETITION));
        EventVolume volume = new EventVolume();
        volume.begin = d1.getTimeInMillis();
        volume.eventCounts = new int[count];
        for (int ii = 0; ii < count; ++ii) {
            volume.eventCounts[ii] = _slingRepo.searchEvents(new EventSearch(
                EventFilter.createdIn(new TimeRange(
                    d1.getTimeInMillis(),
                    d2.getTimeInMillis())),
                userReported)).count();
            d1.add(calUnit, 1);
            d2.add(calUnit, 1);
        }

        return volume;
    }

    // from SlingService
    @Override public AverageEventVolumeByHour getAverageVolume (TimeRange range,
        Set<Event.Type> types)
        throws SlingException
    {
        requireAuthedSupport();

        // no coping with hours
        range = new TimeRange(
            Calendars.at(range.from).zeroTime().toTime(),
            Calendars.at(range.to).zeroTime().toTime());

        // count the number of each day of the week in the interval. this will form the denominator
        // for the average
        int[] dayCounts = new int[7];
        for (Calendar cc = Calendars.at(range.from).asCalendar();
            cc.getTimeInMillis() < range.to; cc.add(Calendar.DATE, 1)) {
            dayCounts[cc.get(Calendar.DAY_OF_WEEK) - 1]++;
        }

        // load the data
        int[][] eventCounts = _slingRepo.getVolumeByDayAndHour(range.from, range.to, types);

        // calculate averages
        float[][] averages = new float[7][];
        for (int day = 0; day < 7; ++day) {
            float[] avg = averages[day] = new float[24];
            int[] num = eventCounts[day];
            float den = dayCounts[day];
            for (int hour = 0; hour < 24; ++hour) {
                avg[hour] = (den == 0) ? -1 : num[hour] / den;
            }
        }

        return new AverageEventVolumeByHour(averages);
    }

    // from SlingService
    @Override public Map<String, Long> getAgentActivity ()
        throws SlingException
    {
        requireAuthedSupport();
        return _slingRepo.getAgentActivity();
    }

    // from SlingService
    @Override public Event loadEvent (int eventId)
        throws SlingException
    {
        requireAuthedSupport();
        EventRecord evrec = _slingRepo.loadEvent(eventId);
        if (evrec == null) {
            return null;
        }
        HashSet<String> accounts = new HashSet<String>();
        accounts.add(evrec.source);
        accounts.add(evrec.target);
        accounts.add(evrec.owner);
        Map<String, AccountName> names = _userLogic.resolveNames(accounts);
        return evrec.toEvent(names);
    }

    // from SlingService
    @Override public List<Message> loadMessages (int eventId)
        throws SlingException
    {
        requireAuthedSupport();

        Collection<MessageRecord> msgrecs = _slingRepo.loadMessages(Collections.singleton(eventId));
        Map<String, AccountName> names = resolveNames(msgrecs);
        ArrayList<Message> msgs = new ArrayList<Message>();
        for (MessageRecord msgrec : msgrecs) {
            msgs.add(msgrec.toMessage(names));
        }
        return msgs;
    }

    // from SlingService
    @Override public Message updateEvent (int eventId, Event.Status status)
        throws SlingException
    {
        Caller caller = requireAuthedUser();
        EventRecord event = requireEvent(eventId);

        // TODO: find out if this is really called by non-support users. If not, remove this \
        //       check and the one below.
        if (!caller.isSupport &&
            !(caller.username.equals(event.source) && status == Event.Status.PLAYER_CLOSED)) {
            log.warning("Refusing to update status of event", "who", caller.username,
                "event", event);
            throw new SlingException("m.access_denied");
        }

        // update the event's status
        StringBuilder message = new StringBuilder("Status changed to " + status);

        // some state updates have side effects
        String newOwner = event.owner;
        switch (status) {
        case IN_PROGRESS: // setting to in_progress means claiming
            newOwner = caller.username;
            message.append("\nOwner changed to " + newOwner);
            noteAgentActivity(newOwner);
            break;

        case OPEN:
        case ESCALATED_LEAD:
        case ESCALATED_ADMIN: // reopening, or escalating means unclaiming
            newOwner = null;
            message.append("\nOwner cleared");
            break;
        }

        // now update the persistent event record
        _slingRepo.updateEvent(event.eventId, status, newOwner);

        // add a message to log the change
        MessageRecord msgrec = new MessageRecord();
        msgrec.eventId = event.eventId;
        msgrec.author = caller.username;
        msgrec.access = Message.Access.SUPPORT;
        msgrec.text = message.toString();
        _slingRepo.insertMessage(msgrec, false);

        // update the first reponse if this message is closing the complaint
        if (!status.isOpen() && event.firstResponse == null && event.type == Event.Type.COMPLAINT) {
            event.firstResponse = msgrec.entered.getTime() - event.entered.getTime();
            _slingRepo.updateFirstResponse(event);
        }

        Message msg = msgrec.toMessage(resolveNames(Collections.singleton(msgrec)));
        if (!caller.isSupport) {
            msg.author = toHandle(msg.author);
        }
        return msg;
    }

    // from SlingService
    @Override public Message setWaitingForPlayer (int eventId, boolean waitingForPlayer)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();
        EventRecord event = requireEvent(eventId);

        if (waitingForPlayer == event.waitingForPlayer) {
            throw new SlingException("e.invalid_state");
        }

        _slingRepo.setWaitingForPlayer(eventId, waitingForPlayer);

        // add a message to log the change
        MessageRecord msgrec = new MessageRecord();
        msgrec.eventId = event.eventId;
        msgrec.author = caller.username;
        msgrec.access = Message.Access.SUPPORT;
        msgrec.text = (waitingForPlayer ? "Set" : "Cleared") + " waiting for player";
        _slingRepo.insertMessage(msgrec, false);

        Message msg = msgrec.toMessage(resolveNames(Collections.singleton(msgrec)));
        if (!caller.isSupport) {
            msg.author = toHandle(msg.author);
        }
        return msg;
    }

    // from SlingService
    @Override public Message setLanguage (int eventId, String language)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();
        EventRecord event = requireEvent(eventId);

        if (Objects.equal(language, event.language)) {
            throw new SlingException("e.invalid_state");
        }

        // check language is supported, explicitly allow null
        if (language != null && !getSupportedLanguages().contains(language)) {
            throw new SlingException("e.language_not_supported");
        }

        _slingRepo.setLanguage(eventId, language);

        // add a message to log the change
        MessageRecord msgrec = new MessageRecord();
        msgrec.eventId = event.eventId;
        msgrec.author = caller.username;
        msgrec.access = Message.Access.SUPPORT;
        msgrec.text = "Changed language from " + event.language + " to " + language;
        _slingRepo.insertMessage(msgrec, false);

        return msgrec.toMessage(resolveNames(Collections.singleton(msgrec)));
    }

    // from SlingService
    @Override public void updateEvents (int[] eventIds, Event.Status status)
        throws SlingException
    {
        for (int eventId : eventIds) {
            updateEvent(eventId, status);
        }
    }

    // from SlingService
    @Override public AssignEventResult assignEvent (int eventId, Event.Status status,
        String accountName) throws SlingException
    {
        Caller caller = requireAuthedSupport();
        EventRecord event = requireEvent(eventId);

        Account account = _userLogic.getAccountByName(getSiteId(), accountName);
        if (account == null) {
            throw new SlingException("m.no_such_user");
        }

        if (!(account.isSet(Account.Flag.SUPPORT) || account.isSet(Account.Flag.ADMIN))) {
            throw new SlingException("m.player_not_authorized");
        }

        AccountName owner = account.name;

        // now update the persistent event record
        _slingRepo.updateEvent(event.eventId, status, owner.accountName);

        // add a message to log this assignment
        MessageRecord msgrec = new MessageRecord();
        msgrec.eventId = event.eventId;
        msgrec.author = caller.username;
        msgrec.access = Message.Access.SUPPORT;
        msgrec.text = "Status changed to " + status +
            "Owner changed to " + owner.accountName + "\n";
        _slingRepo.insertMessage(msgrec, false);

        // return the result
        AssignEventResult ret = new AssignEventResult();
        ret.newOwner = owner;
        ret.logMessage = msgrec.toMessage(resolveNames(Collections.singleton(msgrec)));
        return ret;
    }

    // from SlingService
    @Override public int addNote (String accountName, String subject, String note)
        throws SlingException
    {
        Caller caller = requireAuthedSupport();
        int eventId = recordEvent(Event.Type.NOTE, caller.username, accountName, subject, "");

        // create and add the message record
        MessageRecord msgrec = new MessageRecord();
        msgrec.eventId = eventId;
        msgrec.author = caller.username;
        msgrec.text = note;
        msgrec.access = Message.Access.SUPPORT;
        _slingRepo.insertMessage(msgrec, false);

        return eventId;
    }

    // from SlingService
    @Override public PostMessageResult postMessage (int eventId, Message.Access access,
        String message, boolean updateWaitingFlag) throws SlingException
    {
        Caller caller = requireAuthedUser();
        EventRecord event = requireEvent(eventId);

        // if this user created this event and the access is normal, they can add a message;
        // otherwise they must be support
        boolean isOwner = caller.username.equals(event.source);
        if (!caller.isSupport && (!isOwner || access == Message.Access.SUPPORT)) {
            throw new SlingException("m.access_denied");
        }

        // require that the event still be in an open state for publicly visible messages
        if (access == Message.Access.NORMAL && !event.status.isOpen()) {
            throw new SlingException("m.event_closed");
        }

        if (caller.isSupport) {
            noteAgentActivity(caller.username);
        }

        // is this message a reply from support team to the user?
        boolean isReply = caller.isSupport && event.type == Event.Type.PETITION &&
            access == Message.Access.NORMAL;

        // create and add the message record
        MessageRecord msgrec = new MessageRecord();
        msgrec.eventId = eventId;
        msgrec.author = caller.username;
        msgrec.text = message;
        msgrec.access = access;
        _slingRepo.insertMessage(msgrec, isReply || event.type != Event.Type.PETITION);

        PostMessageResult result = new PostMessageResult();

        result.waitingForPlayer = event.waitingForPlayer;
        if (updateWaitingFlag) {
            if (!event.waitingForPlayer && caller.isSupport &&
                    access == Message.Access.NORMAL) {
                _slingRepo.setWaitingForPlayer(event.eventId, result.waitingForPlayer = true);
            } else if (event.waitingForPlayer && isOwner) {
                _slingRepo.setWaitingForPlayer(event.eventId, result.waitingForPlayer = false);
            }
        }

        // if this is the fist support reply to a petition or the first support message for a
        // complaint, init the first response field
        if (event.firstResponse == null && (isReply || event.type == Event.Type.COMPLAINT)) {
            event.firstResponse = msgrec.entered.getTime() - event.entered.getTime();
            _slingRepo.updateFirstResponse(event);
        }

        // if this is a reply to a non-anonymous user petition, send a message to the user
        if (isReply && event.source.length() != 0) {
            // don't crash if send fails, just return the error string
            try {
                _actionHandler.sendMessage(caller.username, event.source, event.sourceHandle,
                    event.subject);
            } catch (SlingException e) {
                result.sendError = e.getMessage();
            }
        }

        if (isOwner && Event.Status.IN_PROGRESS.equals(event.status)) {
            _slingRepo.updateEvent(event.eventId, Event.Status.OPEN, null);
        }

        result.message = msgrec.toMessage(_userLogic.resolveNames(
            Collections.singleton(caller.username)));
        if (!caller.isSupport) {
            result.message.author = toHandle(result.message.author);
        }

        return result;
    }

    // from SlingService
    @Override public void updateIdentBanned (String machIdent, boolean banned)
        throws SlingException
    {
        requireAuthedSupport();

        // update the persistent store
        _userLogic.updateIdentBanned(getSiteId(), machIdent, banned);

        // update the cache
        updateRelatedAccounts(machIdent, null, banned);
    }

    // from SlingService
    @Override public void updateIdentTaint (String machIdent, boolean tainted)
        throws SlingException
    {
        requireAuthedSupport();

        // update the persistent store
        _userLogic.updateIdentTainted(getSiteId(), machIdent, tainted);

        // update the cache
        updateRelatedAccounts(machIdent, tainted, null);
    }

    // from SlingService
    @Override public int updateFlags (String accountName, int setFlags, int clearFlags)
        throws SlingException
    {
        Caller user = requireAuthedSupport();
        int mask = Account.Flag.DEADBEAT.mask() | Account.Flag.INSIDER.mask() |
            Account.Flag.TESTER.mask();
        if (user.isMaintainer) {
            mask |= Account.Flag.ADMIN.mask() | Account.Flag.MAINTAINER.mask();
        }
        if (user.isAdmin) {
            mask |= Account.Flag.SUPPORT.mask() | Account.Flag.JR_SUPPORT.mask();
        }
        setFlags &= mask;
        clearFlags &= mask;

        _userLogic.updateFlags(accountName, setFlags, clearFlags, getSiteId());
        return mask;
    }

    // from SlingService
    @Override public List<Category> getFAQs ()
        throws SlingException
    {
        return _faqs.get(0);
    }

    // from SlingService
    @Override public int storeCategory (Category category)
        throws SlingException
    {
        requireAuthedSupport();
        CategoryRecord record = new CategoryRecord(category);
        _slingRepo.storeCategory(record);
        _faqs.clear();
        return record.categoryId;
    }

    // from SlingService
    @Override public int storeQuestion (Question question)
        throws SlingException
    {
        requireAuthedSupport();
        QuestionRecord record = new QuestionRecord(question);
        _slingRepo.storeQuestion(record);
        _faqs.clear();
        return record.questionId;
    }

    // from SlingService
    @Override public String processCall (String payload)
        throws SerializationException
    {
        try {
            return super.processCall(payload);
        } catch (UnexpectedException e) {
            log.error("Call failed because: ", e.getCause());
            throw e;
        }
    }

    // from SlingService
    @Override public AuthUrl dummy1 ()
        throws SlingException
    {
        requireAuthedSupport();
        return AuthUrl.BILLING;
    }

    /**
     * Returns the name of the session cookie set when a user logs in and clear when they log out.
     * The cookie value is a session id value from the user db sessions table. By default, a sling
     * standalone mode cookie name is returned. Override to share authentication state with
     * another webapp.
     */
    protected String getSessionCookieName ()
    {
        return "slingtok";
    }

    /**
     * Returns the equivalent of the given name but without any deleted game names.
     * @see GameInfoProvider#isDeleted(String)
     */
    protected AccountName withoutDeletedGameNames (AccountName name)
    {
        if (Iterables.all(name.gameNames, _notDeleted)) {
            return name;
        }

        return new AccountName(name.accountName,
                Lists.newArrayList(Iterables.filter(name.gameNames, _notDeleted)));
    }

    /**
     * Converts an account name to a name to be used for displaying replies to user petitions.
     * By default, omits the account name and keeps all the undeleted game names.
     */
    protected AccountName toHandle (AccountName name)
    {
        return withoutDeletedGameNames(new AccountName("", name.gameNames));
    }

    /**
     * Returns a URL to redirect the user to if a page is requested that requires a session and
     * the user is not logged in. By default, returns null, which causes the sling client to
     * display a standalone sling login page. Override to redirect to a main login page.
     */
    protected String getLoginRedirectUrl ()
    {
        return null;
    }

    /**
     * Returns a URL to redirect the user to when a logout is requested. By default, returns
     * null, which causes the sling client to display a standalone sling login page. Override to
     * redirect to a main logout page.
     */
    protected String getLogoutRedirectUrl ()
    {
        return null;
    }

    /**
     * Returns the default language to use for events created by the servlet, if none could be
     * determined from other information. Note that for sling, languages are two letter codes.
     * By default, returns the code for English, "en".
     */
    protected String getDefaultLanguage ()
    {
        return "en";
    }

    /**
     * Returns the languages that may be used by the servlet when creating events. If a user's
     * language is set to a value not in this set, the default will be used. The set must always
     * contain the value returned by {@link #getDefaultLanguage()}. Note that for sling, languages
     * are two letter codes. By default, returns the singleton containing the default language.
     */
    protected Set<String> getSupportedLanguages ()
    {
        return Collections.singleton(getDefaultLanguage());
    }

    protected String getAuthTok ()
    {
        return CookieUtil.getCookieValue(getThreadLocalRequest(), getSessionCookieName());
    }

    /**
     * Whether or not to allow users to update their email address when submitting an issue.
     */
    protected boolean allowEmailUpdate ()
    {
        return true;
    }

    /**
     * Returns the id of the site for which we're managing support (determined by looking at the
     * URL through which sling is being accessed.
     */
    protected int getSiteId ()
    {
        int siteId = _siteIdentifier.identifySite(getThreadLocalRequest());
        if (siteId == -1) {
            siteId = OOOUser.BANGHOWDY_SITE_ID; // use Bang when testing
        }
        return siteId;
    }

    protected AuthInfo createAuthInfo (Caller caller)
        throws SlingException
    {
        _userLogic.resolveName(caller.username);
        AuthInfo ainfo = new AuthInfo();
        ainfo.name = withoutDeletedGameNames(_userLogic.resolveName(caller.username));
        ainfo.email = caller.email;
        ainfo.isJrSupport = caller.isJrSupport;
        ainfo.isSupport = caller.isSupport;
        ainfo.isAdmin = caller.isAdmin;
        ainfo.isMaintainer = caller.isMaintainer;
        if (ainfo.isSupport) {
            String site = _siteIdentifier.getSiteString(getSiteId());
            String url = _oooconf.getValue(site + ".game_info", "");
            if (!("".equals(url))) {
                ainfo.setUrl(AuthUrl.GAME, url);
            }

            url = _oooconf.getValue(site + ".billing_info", "");
            if (!("".equals(url))) {
                ainfo.setUrl(AuthUrl.BILLING, url);
            }
        }

        return ainfo;
    }

    protected Map<String, AccountName> resolveNames (Collection<MessageRecord> msgrecs)
    {
        HashSet<String> accounts = new HashSet<String>();
        for (MessageRecord msgrec : msgrecs) {
            accounts.add(msgrec.author);
        }
        return _userLogic.resolveNames(accounts);
    }

    protected Caller requireAuthedUser ()
        throws SlingException
    {
        // get the cookie value
        String authTok = getAuthTok();

        // sanity check (our client should always init the cookie using validateSession)
        if (authTok == null) {
            throw new SlingException("m.internal_error");
        }

        return _callers.get(authTok);
    }

    protected Caller requireAuthedSupport ()
        throws SlingException
    {
        Caller user = requireAuthedUser();
        // make sure they have proper privileges
        if (!user.isSupport) {
            throw new AuthenticationException("m.access_denied");
        }
        return user;
    }

    protected Caller requireAuthedJrSupport ()
        throws SlingException
    {
        Caller user = requireAuthedUser();
        // make sure they have proper privileges
        if (!user.isJrSupport) {
            throw new AuthenticationException("m.access_denied");
        }
        return user;
    }

    protected Caller requireAuthedAdmin ()
        throws SlingException
    {
        Caller user = requireAuthedUser();
        // make sure they have proper privileges
        if (!user.isAdmin) {
            throw new AuthenticationException("m.access_denied");
        }
        return user;
    }

    protected EventRecord requireEvent (int eventId)
        throws SlingException
    {
        EventRecord event = _slingRepo.loadEvent(eventId);
        if (event == null) {
            throw new SlingException("m.no_such_event");
        }
        return event;
    }

    protected int registerPetitionRecord (
            String source, String handle, UserPetition petition, String message)
        throws SlingException
    {
        // create a new support record for this petition
        EventRecord evrec = new EventRecord();
        evrec.type = Event.Type.PETITION;
        evrec.source = source;
        evrec.sourceHandle = handle;
        evrec.subject = petition.subject;
        evrec.status = Event.Status.OPEN;
        evrec.chatHistory = "";
        fillSessionInfo(evrec);

        MessageRecord msgrec = new MessageRecord();
        msgrec.author = source;
        msgrec.text = message;
        msgrec.access = Message.Access.NORMAL;

        // add the event record
        _slingRepo.insertEvent(evrec);

        // and the initial message record
        msgrec.eventId = evrec.eventId;
        _slingRepo.insertMessage(msgrec, false);

        return evrec.eventId;
    }

    protected int recordEvent (Event.Type type, String source, String target, String subject)
        throws SlingException
    {
        return recordEvent(type, source, target, subject, null);
    }

    protected int recordEvent (Event.Type type, String source, String target, String subject,
        String text)
        throws SlingException
    {
        // create a new support record for this petition
        EventRecord evrec = new EventRecord();
        evrec.type = type;
        evrec.source = source;
        evrec.target = target;
        evrec.subject = subject;
        evrec.status = Event.Status.RESOLVED_CLOSED;
        evrec.chatHistory = (text == null ? "" : text);
        fillSessionInfo(evrec);

        // add the event record
        _slingRepo.insertEvent(evrec);

        return evrec.eventId;
    }

    protected PagedResult<Event> toResult(
        SlingRepository.PagedQuery<EventRecord> query, PagedRequest request)
    {
        PagedResult<Event> result = new PagedResult<Event>();
        if (request.needCount) {
            result.total = query.count();
        }

        List<EventRecord> evrecs = query.load(request.offset, request.count);
        // resolve all accounts associated with these events
        HashSet<String> accounts = new HashSet<String>();
        for (EventRecord event : evrecs) {
            accounts.add(event.source);
            accounts.add(event.target);
            accounts.add(event.owner);
        }
        Map<String, AccountName> names = _userLogic.resolveNames(accounts);

        result.page = Lists.newArrayListWithExpectedSize(evrecs.size());
        for (EventRecord event : evrecs) {
            result.page.add(event.toEvent(names));
        }
        return result;
    }

    protected void updateRelatedAccounts (String machIdent, Boolean tainted, Boolean banned)
    {
        // update the cache
        for (List<MachineIdentity> idents : _relatedAccounts.values()) {
            for (MachineIdentity ident : idents) {
                if (ident.machIdent.equals(machIdent)) {
                    ident.banned = banned != null ? banned : ident.banned;
                    ident.tainted = tainted != null ? tainted : ident.tainted;
                    break;
                }
            }
        }
    }

    protected void updateRelatedAccounts (Account account, boolean banned)
    {
        // update the cache
        for (List<MachineIdentity> idents : _relatedAccounts.values()) {
            for (MachineIdentity ident : idents) {
                for (AccountInfo info : ident.accounts) {
                    if (info.name.equals(account.name)) {
                        info.banned = banned;
                    }
                }
            }
        }
    }

    protected void fillSessionInfo (EventRecord evrec)
    {
        HttpServletRequest req = getThreadLocalRequest();
        evrec.language = getSessionLanguage(req);
        evrec.sourceIpAddress = req.getRemoteAddr();
    }

    protected String getSessionLanguage (HttpServletRequest req)
    {
        Locale locale = (Locale)req.getSession().getAttribute(
            DefaultLocaleFilter.LOCALE_ATTRIBUTE);
        if (locale == null) {
            return getDefaultLanguage();
        }
        String lang = locale.getLanguage();
        if (lang == null) {
            return getDefaultLanguage();
        }
        if (getSupportedLanguages().contains(lang)) {
            return lang;
        }
        int underscore = lang.indexOf('_');
        if (underscore == 2) {
            lang = lang.substring(0, underscore);
            if (getSupportedLanguages().contains(lang)) {
                return lang;
            }
        }
        return getDefaultLanguage();
    }

    /**
     * Notes that a member of support staff is doing something that looks like they are on duty.
     */
    protected void noteAgentActivity (String accountName)
    {
        Long time = _activityWriteCache.get(accountName);
        if (time != null && System.currentTimeMillis() - time < 5 * 60 * 1000L) {
            return;
        }
        long now = _slingRepo.noteAgentActivity(accountName);
        _activityWriteCache.put(accountName, now);
    }

    /** Provides access to all of our dependencies. */
    @Inject protected UserLogic _userLogic;
    @Inject protected GameActionHandler _actionHandler;
    @Inject protected GameInfoProvider _infoProvider;
    @Inject protected SlingRepository _slingRepo;
    @Inject protected SiteIdentifier _siteIdentifier;

    protected Properties loadOOOProps () {
        Properties props = new Properties();
        try {
            props.load(getClass().getClassLoader().getResourceAsStream("threerings.properties"));
        } catch (Exception e) {
            log.warning("Failed to load threerings.properties.", e);
        }
        return props;
    }
    protected final Config _oooconf = new Config(loadOOOProps());

    protected final Predicate<String> _notDeleted = new Predicate<String> () {
        public boolean apply (String name) {
            return !_infoProvider.isDeleted(name);
        }
    };

    /** A cache of authenticated users. */
    protected SimpleCache<String, Caller> _callers =
            new SimpleCache<String, Caller>(GENERAL_REFRESH_INTERVAL) {
        @Override protected Caller compute (String authtok) throws SlingException {
            Caller caller = _userLogic.loadCaller(authtok);
            if (caller == null) {
                throw new AuthenticationException("m.session_expired");
            }
            return caller;
        }
    };

    /** A mapping from account id to a list of accounts that have played from a machine identifier
     * from which the original account has played. */
    protected SimpleCache<Integer, List<MachineIdentity>> _relatedAccounts =
            new SimpleCache<Integer, List<MachineIdentity>>(GENERAL_REFRESH_INTERVAL) {
        @Override protected List<MachineIdentity> compute (Integer accountId)
            throws SlingException {
            return _userLogic.getRelatedAccounts(getSiteId(), accountId);
        }
    };

    /** A cached copy of the FAQs. */
    protected SimpleCache<Integer, List<Category>> _faqs =
            new SimpleCache<Integer, List<Category>>(FAQ_REFRESH_INTERVAL) {
        @Override protected List<Category> compute (Integer key) throws SlingException {
            Preconditions.checkArgument(key == 0);

            // first load our categories
            HashIntMap<Category> cats = new HashIntMap<Category>();
            for (CategoryRecord cat : _slingRepo.loadCategories()) {
                cats.put(cat.categoryId, cat.toCategory());
            }

            // then map our questions to the categories
            for (QuestionRecord quest : _slingRepo.loadQuestions()) {
                Category cat = cats.get(quest.categoryId);
                if (cat == null) {
                    log.warning("Question mapped to non-existent category! " + quest + ".");
                    continue;
                }
                cat.questions.add(quest.toQuestion());
            }

            // finally store them as an array of categories
            List<Category> faqs = Lists.newArrayList(cats.values());
            Collections.sort(faqs, new Comparator<Category>() {
                public int compare (Category one, Category two) {
                    return Ints.compare(two.categoryId, one.categoryId); // reversed
                }
            });

            return faqs;
        }
    };

    @Inject protected MessageManager _msgmgr;

    protected MessageBundle _msgs;

    /** Caches agent last activity times. This is different from a SimpleCache because values
     * are written frequently and read rarely. */
    protected ConcurrentMap<String, Long> _activityWriteCache = Maps.newConcurrentMap();

//    protected static final DateFormat DBG_FMT =
//        DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

    /** Expire sessions after five days. */
    protected static final int SESSION_EXPIRY_DAYS = 5;

    /** Refresh our cached FAQs every five minutes. */
    protected static final long FAQ_REFRESH_INTERVAL = 5 * 60 * 1000L; // 5 * 60 * 1000L;

    /** Refresh our cached user languages every hour. */
    protected static final long USER_LANGUAGES_REFRESH_INTERVAL = 60 * 60 * 1000L;

    /** Refresh our cached namess every five minutes. */
    protected static final long GENERAL_REFRESH_INTERVAL = 5 * 60 * 1000L;
}
