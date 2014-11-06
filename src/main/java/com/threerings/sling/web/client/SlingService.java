//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedRequest;
import com.threerings.gwt.util.PagedResult;
import com.threerings.sling.web.data.Account;
import com.threerings.sling.web.data.AccountName;
import com.threerings.sling.web.data.Category;
import com.threerings.sling.web.data.Event;
import com.threerings.sling.web.data.EventSearch;
import com.threerings.sling.web.data.MachineIdentity;
import com.threerings.sling.web.data.Message;
import com.threerings.sling.web.data.Question;
import com.threerings.sling.web.data.TimeRange;
import com.threerings.sling.web.data.UserPetition;

/**
 * Provides services needed by the Sling GWT client.
 *
 * <p>NOTE: {@code Date} and {@link TimeRange} values passed into methods here are expected to be
 * converted to UTC time on the client. See {@link com.threerings.gwt.util.DateUtil#toUTC()} and
 * {@link com.threerings.sling.gwt.util.TimeRanges#toUTC()}.</p>
 */
public interface SlingService extends RemoteService
{
    /** Query types for {@link #loadEvents}. */
    public enum Events { OPEN, MY, ALL, ACCOUNT }

    /** The kinds of urls we may want to sent back with the AuthInfo. */
    public enum AuthUrl { GAME, BILLING, REDIRECT }

    /**
     * Information returned during the logon process.
     */
    public static class AuthInfo implements IsSerializable
    {
        public AccountName name;
        public String email;
        public boolean isSupport;
        public boolean isJrSupport;
        public boolean isAdmin;
        public boolean isMaintainer;

        public AuthInfo() {}
        public String getBillingUrl ()
        {
            return getUrl(AuthUrl.BILLING);
        }

        public String getGameUrl ()
        {
            return getUrl(AuthUrl.GAME);
        }

        public String getRedirectUrl ()
        {
            return getUrl(AuthUrl.REDIRECT);
        }

        public String getUrl (AuthUrl which)
        {
            return _externalUrls == null ? null : _externalUrls.get(which);
        }

        public void setUrl (AuthUrl which, String value)
        {
            if (_externalUrls == null) {
                _externalUrls = Maps.newHashMap();
            }
            _externalUrls.put(which, value);
        }

        public void removeUrl (AuthUrl which)
        {
            if (_externalUrls != null) {
                _externalUrls.remove(which);
            }
        }

        // usually relate to the site configuration, may be null if not needed
        protected Map<AuthUrl, String> _externalUrls;
    }

    /**
     * Information returned from {@link #assignEvent()}.
     */
    public static class AssignEventResult implements IsSerializable
    {
        /** The support account that now owns the event. */
        public AccountName newOwner;

        /** The message added to log the assignment. */
        public Message logMessage;
    }

    /**
     * Result for {@link #postMessage()}.
     */
    public static class PostMessageResult implements IsSerializable
    {
        /** The message posted. */
        public Message message;

        /** Indicates the new value of the corresponding event field. */
        public boolean waitingForPlayer;

        /** The error message returned by the game when attempting to send the message to the
         * player, or null if the message was successfully sent to the player. */
        public String sendError;
    }

    /**
     * Total number and events in a time range and the number with a qualifying response time.
     */
    public static class EventResponses
        implements IsSerializable
    {
        public int total;
        public int qualified;
    }

    public enum TimeUnit
    {
        /** A one hour span. */
        HOUR(60 * 60 * 1000L),

        /** A 24 hours span, commonly referred to as a day. */
        DAY(HOUR.millis * 24);

        public final long millis;

        TimeUnit (long millis)
        {
            this.millis = millis;
        }
    }

    /**
     * Total number of events reported in a series of intervals.
     */
    public static class EventVolume
        implements IsSerializable
    {
        public long begin;
        public int eventCounts[];
    }

    /**
     * Average number of events reported on each hour for each day of the week.
     */
    public static class AverageEventVolumeByHour implements IsSerializable
    {
        public AverageEventVolumeByHour ()
        {
        }

        public AverageEventVolumeByHour (float[][] eventCounts)
        {
            _eventCounts = eventCounts;
        }

        /**
         * Returns the event count for the given day of the week (0=Sunday) and given hour (0-23).
         * A value of -1 means that the requested hour did not occur in the range requested in
         * {@link SlingService#getAverageVolume()}.
         */
        public float getEventCount (int dayOfTheWeek, int hour)
        {
            return _eventCounts[dayOfTheWeek][hour];
        }

        protected float[][] _eventCounts;
    }

    /**
     * Logs the client in and returns some authentication info about the user.
     */
    public AuthInfo login (String username, String passwordHash)
        throws SlingException;

    /**
     * Validates that the session using the request headers. If the session is still active
     * refreshes its expiration time.
     */
    public AuthInfo validateSession ()
        throws SlingException;

    /**
     * Logs out the current user, if any is given by the session token, then clears the cookie.
     * If the server is configured to redirect the user to the main site on logout, returns the URL
     * to redirect to. Otherwise, returns null.
     */
    public String logout ()
        throws SlingException;

    /**
     * Requests to update the calling user's email address.
     */
    public void updateEmail (String email)
        throws SlingException;

    /**
     * Create a new account.
     */
    public Account createSupportAccount (String name, String password, String email)
        throws SlingException;

    /**
     * Loads and returns the account with the specified name. Admin only.
     */
    public Account getAccount (String name)
        throws SlingException;

    /**
     * Loads and returns the account with the specified account name. Admin only.
     */
    public Account getAccountByName (String accountName)
        throws SlingException;

    /**
     * Returns an array containing info on all accounts associated with the specifed account by
     * machine identifier. Admin only.
     */
    public List<MachineIdentity> getRelatedAccounts (int accountId)
        throws SlingException;

    /**
     * Updates the email address, password or both for the supplied account. If either is null it
     * will not be updated. Admin only.
     */
    public void updateAccount (int accountId, String email, String password)
        throws SlingException;

    /**
     * Updates the banned status of the specified account. Admins only.
     *
     * @param untaintIdents if banned is false (we're clearing a ban) and clearIdents is true, we
     * will also untaint any machine idents associated with the banned account.
     */
    public void updateBanned (int accountId, boolean banned, String reason,
                              boolean untaintIdents)
        throws SlingException;

    /**
     * Updates the temp banned status of the specified account. Admins only.
     */
    public void updateTempBan (String accountName, int days, String warning)
        throws SlingException;

    /**
     * Updates the warning message of the specified account. Admins only.
     */
    public void updateWarning (String accountName, String warning)
        throws SlingException;

    /**
     * Searches for accounts that have the supplied email address. Admin only.
     */
    public AccountName[] findAccountsByEmail (String query)
        throws SlingException;

    /**
     * Searches for accounts that have the supplied display name. Admin only.
     */
    public AccountName[] findAccountsByGameName (String query)
        throws SlingException;

    /**
     * Loads all petitions created by the calling user.
     */
    public List<UserPetition> loadPetitions ()
        throws SlingException;

    /**
     * Requests to register a petition with the system.
     */
    public int registerPetition (
        UserPetition petition, String gameName, String message)
        throws SlingException;

    /**
     * Requests to register an anonymous petition with the system.
     */
    public void registerAnonymousPetition (String email, UserPetition petition, String message)
        throws SlingException;

    /**
     * Loads up all events matching the supplied criterion. Admin only.
     */
    public PagedResult<Event> loadEvents (Events criterion, String query,
        PagedRequest request)
        throws SlingException;

    /**
     * Loads up all events matching the supplied criterion. Admin only.
     */
    public PagedResult<Event> searchEvents (EventSearch search,
        PagedRequest request)
        throws SlingException;

    /**
     * Loads up an event by id. Admin only.
     */
    public Event loadEvent (int eventId)
        throws SlingException;

    /**
     * Finds all petitions created between two given dates that have a first response within the
     * given number of milliseconds.
     */
    public EventResponses checkResponseTimes (
        Event.Type type, TimeRange range, long thresholdMillis)
        throws SlingException;

    /**
     * Retrieves the volume of event reporting for each of the most recent time units, up to the
     * given number.
     * @param now the current time
     * @param timeUnit interval size. intervals are generated by subtracting from {@code now}
     * @param count number of intervals to report
     */
    public EventVolume getVolume (long now, TimeUnit timeUnit, int count)
        throws SlingException;

    /**
     * Retrieves the average volume of events reported between the given dates.
     */
    public AverageEventVolumeByHour getAverageVolume (
        TimeRange range, Set<Event.Type> types)
        throws SlingException;

    /**
     * Gets the most recent activity times for agents. This will be the last time an agent
     * performed a search for open tickets, changed the status of a ticket or posted a message or
     * reply to a ticket.
     */
    public Map<String, Long> getAgentActivity ()
        throws SlingException;

    /**
     * Loads up all messages for the supplied event. Admin only.
     */
    public Message[] loadMessages (int eventId)
        throws SlingException;

    /**
     * Updates the specified support event's status, possibly claiming or relinquishing it as
     * appropriate. Admin only.
     */
    public Message updateEvent (int eventId, Event.Status status)
        throws SlingException;

    /**
     * Update whether the given event id is waiting for a player response or not. Admin only.
     */
    public Message setWaitingForPlayer (int eventId, boolean waiting)
        throws SlingException;

    /**
     * Updates the language of an event. Admin only.
     */
    public Message setLanguage (int eventId, String language)
        throws SlingException;

    /**
     * Updates the status of a list of events, possibly claiming or relinquishing it as
     * appropriate. Admin only.
     */
    public void updateEvents (int[] eventIds, Event.Status status)
        throws SlingException;

    /**
     * Assigned the specified support event to the user. Admin only.
     */
    public AssignEventResult assignEvent (int eventId, Event.Status status,
        String gameName)
        throws SlingException;

    /**
     * Adds a support note to an account. Admin only.
     */
    public int addNote (String accountName, String subject, String note)
        throws SlingException;

    /**
     * Adds the supplied message to the specified support event.
     *
     * @return a newly created {@link Message} instance for the message.
     */
    public PostMessageResult postMessage (int eventId, Message.Access access,
        String message, boolean updateWaitingFlag) throws SlingException;

    /**
     * Returns a list of all FAQ Category objects.
     */
    public Category[] getFAQs ()
        throws SlingException;

    /**
     * Updates or creates a FAQ category. Admin only.
     *
     * @return the assigned category id for newly created categories.
     */
    public int storeCategory (Category category)
        throws SlingException;

    /**
     * Updates or creates a FAQ question with the supplied details. Admin only.
     *
     * @return the assigned question id for newly created questions.
     */
    public int storeQuestion (Question question)
        throws SlingException;

    /**
     * Updates the banned status of the specified machine ident. Admins only.
     */
    public void updateIdentBanned (String machIdent, boolean banned)
        throws SlingException;

    /**
     * Updates the tainted status of the specified machine ident. Admins only.
     */
    public void updateIdentTaint (String machIdent, boolean tainted)
        throws SlingException;

    /**
     * Updates the flags for an account. Admins only.
     */
    public int updateFlags (String accountName, int setFlags, int clearFlags)
        throws SlingException;
}
