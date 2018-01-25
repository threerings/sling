//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedRequest;
import com.threerings.gwt.util.PagedResult;
import com.threerings.sling.web.client.SlingService.AssignEventResult;
import com.threerings.sling.web.client.SlingService.AuthInfo;
import com.threerings.sling.web.client.SlingService.AuthUrl;
import com.threerings.sling.web.client.SlingService.AverageEventVolumeByHour;
import com.threerings.sling.web.client.SlingService.EventResponses;
import com.threerings.sling.web.client.SlingService.EventVolume;
import com.threerings.sling.web.client.SlingService.PostMessageResult;
import com.threerings.sling.web.client.SlingService.TimeUnit;
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
 * Defines the asynchronous version of {@link SlingService}.
 */
public interface SlingServiceAsync
{
    /**
     * The asynchronous version of {@link SlingService#login}.
     */
    public void login (String username, String password,
                       AsyncCallback<AuthInfo> callback);

    /**
     * The asynchronous version of {@link SlingService#validateSession}.
     */
    public void validateSession (AsyncCallback<AuthInfo> callback);

    /**
     * The asynchronous version of {@link SlingService#logout}.
     */
    public void logout (AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link SlingService#updateEmail}.
     */
    public void updateEmail (String email, AsyncCallback<Void> callback);

    /**
     * Create a new account.
     */
    public void createSupportAccount (
            String name, String password, String email, AsyncCallback<Account> callback);

    /**
     * The asynchronous version of {@link SlingService#getAccount}.
     */
    public void getAccount (String name, AsyncCallback<Account> callback);

    /**
     * The asynchronous version of {@link SlingService#getAccountByName}.
     */
    public void getAccountByName (
        String accountName, AsyncCallback<Account> callback);

    /**
     * The asynchronous version of {@link SlingService#getRelatedAccounts}.
     */
    public void getRelatedAccounts (
        int accountId, AsyncCallback<List<MachineIdentity>> callback);

    /**
     * The asynchronous version of {@link SlingService#updateAccount}.
     */
    public void updateAccount (
        int accountId, String email, String password, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SlingService#updateBanned}.
     */
    public void updateBanned (
        int accountId, boolean banned, String reason, boolean untaintIdents,
        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SlingService#updateTempBan}.
     */
    public void updateTempBan (
        String accountName, int days, String reason, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SlingService#updateWarning}.
     */
    public void updateWarning (String accountName, String reason,
                               AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SlingService#findAccountsByEmail}.
     */
    public void findAccountsByEmail (
        String query, AsyncCallback<AccountName[]> callback);

    /**
     * The asynchronous version of {@link SlingService#findAccountsByGameName}.
     */
    public void findAccountsByGameName (
        String query, AsyncCallback<AccountName[]> callback);

    /**
     * The asynchronous version of {@link SlingService#loadPetitions}.
     */
    public void loadPetitions (AsyncCallback<List<UserPetition>> callback);

    /**
     * The asynchronous version of {@link SlingService#registerPetition}.
     */
    public void registerPetition (UserPetition petition, String gameName,
        String message, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link SlingService#registerAnonymousPetition}.
     */
    public void registerAnonymousPetition (
        String email, UserPetition petition, String message, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link SlingService#loadEvents}.
     */
    public void loadEvents (
        SlingService.Events criterion, String query, PagedRequest request,
        AsyncCallback<PagedResult<Event>> callback);

    /**
     * The asynchronous version of {@link SlingService#searchEvents}.
     */
    public void searchEvents (
        EventSearch search, PagedRequest request,
        AsyncCallback<PagedResult<Event>> callback);

    /**
     * The asynchronous version of {@link SlingService#loadEvents}.
     */
    public void loadEvent (
        int eventId, AsyncCallback<Event> callback);

    /**
     * The asynchronous version of {@link SlingService#checkResponseTimes}.
     */
    public void checkResponseTimes (Event.Type type, TimeRange range,
        long thresholdMillis, AsyncCallback<EventResponses> callback);

    /**
     * The asynchronous version of {@link SlingService#getVolume}.
     */
    public void getVolume (long now, TimeUnit timeUnit, int count,
        AsyncCallback<EventVolume> callback);

    /**
     * The asynchronous version of {@link SlingService#getAverageVolume}.
     */
    public void getAverageVolume (TimeRange range, Set<Event.Type> types,
        AsyncCallback<AverageEventVolumeByHour> callback);

    /**
     * The asynchronous version of {@link SlingService#getAgentActivity}.
     */
    public void getAgentActivity (AsyncCallback<Map<String, Long>> callback);

    /**
     * The asynchronous version of {@link SlingService#loadMessages}.
     */
    public void loadMessages (int eventId, AsyncCallback<List<Message>> callback);

    /**
     * The asynchronous version of {@link SlingService#updateEvent}.
     */
    public void updateEvent (
        int eventId, Event.Status status, AsyncCallback<Message> callback);

    /**
     * The asynchronous version of {@link SlingService#setWaitingForPlayer}.
     */
    public void setWaitingForPlayer (
        int eventId, boolean waitingForPlayer, AsyncCallback<Message> callback);

    /**
     * The asynchronous version of {@link SlingService#setLanguage}.
     */
    public void setLanguage (int eventId, String language, AsyncCallback<Message> callback);

    /**
     * The asynchronous version of {@link SlingService#updateEvents}.
     */
    public void updateEvents (
        int[] eventIds, Event.Status status, AsyncCallback<Void> callback);

    /**
     * Assigned the specified support event to the user. Admin only.
     */
    public void assignEvent (int eventId, Event.Status status, String gameName,
        AsyncCallback<AssignEventResult> callback);

    /**
     * The asynchronous version of {@link SlingService#addNote}.
     */
    public void addNote (
        String accountName, String subject, String note,
        AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link SlingService#postMessage}.
     */
    public void postMessage (int eventId, Message.Access access, String message,
        boolean updateWaitFlag, AsyncCallback<PostMessageResult> callback);

    /**
     * The asynchronous version of {@link SlingService#getFAQs}.
     */
    public void getFAQs (AsyncCallback<List<Category>> callback);

    /**
     * The asynchronous version of {@link SlingService#storeCategory}.
     */
    public void storeCategory (Category category, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link SlingService#storeQuestion}.
     */
    public void storeQuestion (Question question, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link SlingService#updateIdentBanned}.
     */
    public void updateIdentBanned (String machIdent, boolean banned,
                                   AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SlingService#updateIdentTaint}.
     */
    public void updateIdentTaint (String machIdent, boolean tainted,
                                    AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link SlingService#updateFlags}.
     */
    public void updateFlags (String accountName, int setFlags, int clearFlags,
            AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link SlingService#updateFlags}.
     */
    public void dummy1 (AsyncCallback<AuthUrl> callback);
}
