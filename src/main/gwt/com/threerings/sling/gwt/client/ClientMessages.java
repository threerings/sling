//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.client;

public interface ClientMessages extends com.google.gwt.i18n.client.Messages
{
    @Key("petitionsAndComplaints")
    String petitionsAndComplaints ();

    @Key("ipAddresses")
    String ipAddresses ();

    @Key("owner")
    String owner ();

    @Key("petRefresh")
    String petRefresh ();

    @Key("statusEscalatedLead")
    String statusEscalatedLead ();

    @Key("petPost")
    String petPost ();

    @Key("eventTypeHeader")
    String eventTypeHeader ();

    @Key("qualifiedEvents")
    String qualifiedEvents ();

    @Key("accountsRelatedToAccountId")
    String accountsRelatedToAccountId (String arg0);

    @Key("admin")
    String admin ();

    @Key("firstResponseLabel")
    String firstResponseLabel ();

    @Key("idents")
    String idents ();

    @Key("searching")
    String searching ();

    @Key("idHeader")
    String idHeader ();

    @Key("loginFailed")
    String loginFailed ();

    @Key("messageLabel")
    String messageLabel ();

    @Key("fmtFirstResponse")
    String fmtFirstResponse (String arg0, String arg1, String arg2);

    @Key("recentEventVolumeTitle")
    String recentEventVolumeTitle ();

    @Key("isBanned")
    String isBanned (String arg0);

    @Key("petNoOpenPetitionsWereFound")
    String petNoOpenPetitionsWereFound ();

    @Key("petitionDefaultRecipient")
    String petitionDefaultRecipient ();

    @Key("noAccountsFoundMatchingEmail")
    String noAccountsFoundMatchingEmail (String arg0);

    @Key("postReplyTitle")
    String postReplyTitle (String arg0, String arg1);

    @Key("statusOpen")
    String statusOpen ();

    @Key("redirecting")
    String redirecting ();

    @Key("submitPetition")
    String submitPetition ();

    @Key("machineIdent")
    String machineIdent ();

    @Key("enterAReasonForTheBan")
    String enterAReasonForTheBan ();

    @Key("relAccAccountHdr")
    String relAccAccountHdr ();

    @Key("isTempBanned")
    String isTempBanned (String arg0);

    @Key("showInactiveAccts")
    String showInactiveAccts ();

    @Key("untaintMachineIdent")
    String untaintMachineIdent ();

    @Key("selectCharacter")
    String selectCharacter ();

    @Key("status")
    String status ();

    @Key("subject")
    String subject ();

    @Key("accountBanned")
    String accountBanned ();

    @Key("cancel")
    String cancel ();

    @Key("accountsMatchingEmail")
    String accountsMatchingEmail (String arg0);

    @Key("messagePostedButNotSent")
    String messagePostedButNotSent (String arg0);

    @Key("totalEventsReported")
    String totalEventsReported ();

    @Key("waitingForPlayer")
    String waitingForPlayer ();

    @Key("registered")
    String registered ();

    @Key("statusLabel")
    String statusLabel ();

    @Key("emailUpdated")
    String emailUpdated ();

    @Key("hasBoughtCoins")
    String hasBoughtCoins ();

    @Key("support")
    String support ();

    @Key("edit")
    String edit ();

    @Key("assignEventTo")
    String assignEventTo ();

    @Key("taintMachineIdent")
    String taintMachineIdent ();

    @Key("matchesTerms")
    String matchesTerms ();

    @Key("unbanMachineIdent")
    String unbanMachineIdent ();

    @Key("showEventsWithFailedResponses")
    String showEventsWithFailedResponses ();

    @Key("created")
    String created ();

    @Key("menuTitleOpened")
    String menuTitleOpened ();

    @Key("days")
    String days ();

    @Key("tempBan")
    String tempBan ();

    @Key("noEventsFound")
    String noEventsFound ();

    @Key("errNoFilters")
    String errNoFilters ();

    @Key("filedAgainst")
    String filedAgainst ();

    @Key("faqEditPart1")
    String faqEditPart1 ();

    @Key("update")
    String update ();

    @Key("result")
    String result ();

    @Key("subjectLabel")
    String subjectLabel ();

    @Key("enterQuestion")
    String enterQuestion ();

    @Key("tempBanDaysMustBePositiveNumber")
    String tempBanDaysMustBePositiveNumber ();

    @Key("errMustSelectCharacter")
    String errMustSelectCharacter ();

    @Key("category")
    String category ();

    @Key("eventId")
    String eventId ();

    @Key("petitionInstructionsEmail")
    String petitionInstructionsEmail (String arg0);

    @Key("languageHeader")
    String languageHeader ();

    @Key("addNew")
    String addNew ();

    @Key("characterName")
    String characterName ();

    @Key("generate")
    String generate ();

    @Key("eventPanelTitle")
    String eventPanelTitle (String arg0, String arg1);

    @Key("isEqualTo")
    String isEqualTo ();

    @Key("supportHistory")
    String supportHistory ();

    @Key("eventTypes")
    String eventTypes ();

    @Key("selectOrCreateNewCategory")
    String selectOrCreateNewCategory ();

    @Key("petYourRecentPetitions")
    String petYourRecentPetitions ();

    @Key("cancelTempBan")
    String cancelTempBan ();

    @Key("deadBeat")
    String deadBeat ();

    @Key("familySubscriber")
    String familySubscriber ();

    @Key("claim")
    String claim ();

    @Key("recentEventVolume")
    String recentEventVolume ();

    @Key("affiliate")
    String affiliate ();

    @Key("accountEnabled")
    String accountEnabled ();

    @Key("target")
    String target ();

    @Key("findingRelatedAccounts")
    String findingRelatedAccounts ();

    @Key("averageEventVolume")
    String averageEventVolume ();

    @Key("accountId")
    String accountId ();

    @Key("enterCategoryName")
    String enterCategoryName ();

    @Key("petitionSubmitted")
    String petitionSubmitted ();

    @Key("noWarning")
    String noWarning ();

    @Key("languageLabel")
    String languageLabel ();

    @Key("maintainer")
    String maintainer ();

    @Key("loadingFaqs")
    String loadingFaqs ();

    @Key("question")
    String question ();

    @Key("statusEscalatedAdmin")
    String statusEscalatedAdmin ();

    @Key("volumePrefix")
    String volumePrefix ();

    @Key("loggingIn")
    String loggingIn ();

    @Key("postNoteToAccount")
    String postNoteToAccount (String arg0);

    @Key("name")
    String name ();

    @Key("accountNotFoundWithName")
    String accountNotFoundWithName (String arg0);

    @Key("altName")
    String altName ();

    @Key("isWarned")
    String isWarned (String arg0);

    @Key("login")
    String login ();

    @Key("source")
    String source ();

    @Key("statusInProgress")
    String statusInProgress ();

    @Key("warning")
    String warning ();

    @Key("setWaitForPlayer")
    String setWaitForPlayer ();

    @Key("statusHeader")
    String statusHeader ();

    @Key("statusIgnoredClosed")
    String statusIgnoredClosed ();

    @Key("accountName")
    String accountName ();

    @Key("billingStatus")
    String billingStatus ();

    @Key("assign")
    String assign ();

    @Key("hasBoughtTime")
    String hasBoughtTime ();

    @Key("privateReplyCheckBox")
    String privateReplyCheckBox ();

    @Key("postNoteLink")
    String postNoteLink ();

    @Key("billingExSubscriber")
    String billingExSubscriber ();

    @Key("generating")
    String generating ();

    @Key("noAccountsFoundMatchingGameName")
    String noAccountsFoundMatchingGameName (String arg0);

    @Key("returnToPetitions")
    String returnToPetitions ();

    @Key("faqEditAddQuestion")
    String faqEditAddQuestion ();

    @Key("anErrorOccurred")
    String anErrorOccurred (String arg0);

    @Key("noLanguage")
    String noLanguage ();

    @Key("passwordLabel")
    String passwordLabel ();

    @Key("notePosted")
    String notePosted ();

    @Key("failedEvents")
    String failedEvents ();

    @Key("emailItem")
    String emailItem ();

    @Key("logoutFailed")
    String logoutFailed ();

    @Key("waitingForPlayerLabel")
    String waitingForPlayerLabel ();

    @Key("agentTip")
    String agentTip ();

    @Key("cancelledTempBan")
    String cancelledTempBan ();

    @Key("petPostNewReply")
    String petPostNewReply ();

    @Key("lastPlayed")
    String lastPlayed ();

    @Key("noMachineIdent")
    String noMachineIdent ();

    @Key("notes")
    String notes ();

    @Key("loadedRelatedAccounts")
    String loadedRelatedAccounts ();

    @Key("privateBy")
    String privateBy ();

    @Key("relAccIdentHdr")
    String relAccIdentHdr ();

    @Key("enterWarningMessageForThisAccount")
    String enterWarningMessageForThisAccount ();

    @Key("updateTempBan")
    String updateTempBan ();

    @Key("chatHistoryLabel")
    String chatHistoryLabel ();

    @Key("trueValue")
    String trueValue ();

    @Key("returnToView")
    String returnToView ();

    @Key("noEventFoundWithId")
    String noEventFoundWithId (String arg0);

    @Key("ticketLanguage")
    String ticketLanguage ();

    @Key("accountNotFound")
    String accountNotFound (String arg0);

    @Key("pageNotFound")
    String pageNotFound ();

    @Key("chatHistory")
    String chatHistory ();

    @Key("banMachineIdent")
    String banMachineIdent ();

    @Key("isMoreThan")
    String isMoreThan ();

    @Key("logout")
    String logout ();

    @Key("billingBanned")
    String billingBanned ();

    @Key("showGameNames")
    String showGameNames ();

    @Key("creationDate")
    String creationDate ();

    @Key("filed")
    String filed ();

    @Key("open")
    String open ();

    @Key("firstPlayed")
    String firstPlayed ();

    @Key("statusResolvedClosed")
    String statusResolvedClosed ();

    @Key("petStatusLabel")
    String petStatusLabel ();

    @Key("dayHdr")
    String dayHdr ();

    @Key("submit")
    String submit ();

    @Key("all")
    String all ();

    @Key("messagePosted")
    String messagePosted ();

    @Key("change")
    String change ();

    @Key("editFaqQuestion")
    String editFaqQuestion ();

    @Key("volumeHdr")
    String volumeHdr ();

    @Key("petitions")
    String petitions ();

    @Key("loggedInAs")
    String loggedInAs ();

    @Key("agentAccount")
    String agentAccount ();

    @Key("flags")
    String flags ();

    @Key("view")
    String view ();

    @Key("filedBy")
    String filedBy ();

    @Key("hours")
    String hours ();

    @Key("lastUpdated")
    String lastUpdated ();

    @Key("searchLabel")
    String searchLabel ();

    @Key("gameInfo")
    String gameInfo ();

    @Key("frequentlyAskedQuestions")
    String frequentlyAskedQuestions ();

    @Key("cancelledWarning")
    String cancelledWarning ();

    @Key("shiftPeriod")
    String shiftPeriod ();

    @Key("viewAccount")
    String viewAccount ();

    @Key("firstResponse")
    String firstResponse ();

    @Key("setWaitForPlayerTip")
    String setWaitForPlayerTip ();

    @Key("lastUpdatedHeader")
    String lastUpdatedHeader ();

    @Key("clearWarning")
    String clearWarning ();

    @Key("reportsTitle")
    String reportsTitle ();

    @Key("clearIdents")
    String clearIdents ();

    @Key("eventTypeSupportAction")
    String eventTypeSupportAction ();

    @Key("averageEventVolumeTitle")
    String averageEventVolumeTitle ();

    @Key("eventTypePetition")
    String eventTypePetition ();

    @Key("accountsMatchingGameName")
    String accountsMatchingGameName (String arg0);

    @Key("responseTime")
    String responseTime ();

    @Key("text")
    String text ();

    @Key("petitionInstructionsInGame")
    String petitionInstructionsInGame (String arg0);

    @Key("ipAddress")
    String ipAddress ();

    @Key("updateWarning")
    String updateWarning ();

    @Key("questionSubmitted")
    String questionSubmitted ();

    @Key("brokenLink")
    String brokenLink ();

    @Key("falseValue")
    String falseValue ();

    @Key("billingFailure")
    String billingFailure ();

    @Key("tempBanned")
    String tempBanned ();

    @Key("passwordUpdated")
    String passwordUpdated ();

    @Key("returnToEditFaqs")
    String returnToEditFaqs ();

    @Key("postNoteTitle")
    String postNoteTitle (String arg0, String arg1);

    @Key("foundOneMatchOpening")
    String foundOneMatchOpening ();

    @Key("accountEmail")
    String accountEmail ();

    @Key("by")
    String by ();

    @Key("goToSearch")
    String goToSearch ();

    @Key("statusPlayerClosed")
    String statusPlayerClosed ();

    @Key("advancedEventSearch")
    String advancedEventSearch ();

    @Key("postNoteBtn")
    String postNoteBtn ();

    @Key("link")
    String link ();

    @Key("eventTypeNote")
    String eventTypeNote ();

    @Key("petReplyToPetition")
    String petReplyToPetition (String arg0);

    @Key("eventsLabel")
    String eventsLabel ();

    @Key("postReplyHeader")
    String postReplyHeader ();

    @Key("loadedAccount")
    String loadedAccount ();

    @Key("noIpAddress")
    String noIpAddress ();

    @Key("postMessageSubtitle")
    String postMessageSubtitle (String arg0);

    @Key("tester")
    String tester ();

    @Key("loadingEvent")
    String loadingEvent (String arg0);

    @Key("noFlags")
    String noFlags ();

    @Key("unbanAccount")
    String unbanAccount ();

    @Key("gameName")
    String gameName ();

    @Key("firstResponseTitle")
    String firstResponseTitle ();

    @Key("complaints")
    String complaints ();

    @Key("ownerId")
    String ownerId ();

    @Key("postNoteHeader")
    String postNoteHeader ();

    @Key("loading")
    String loading ();

    @Key("answer")
    String answer ();

    @Key("postNote")
    String postNote ();

    @Key("editFaqs")
    String editFaqs ();

    @Key("menuTitleClosed")
    String menuTitleClosed ();

    @Key("billingInfo")
    String billingInfo ();

    @Key("bigSpender")
    String bigSpender ();

    @Key("messages")
    String messages ();

    @Key("petitionInstructions2")
    String petitionInstructions2 ();

    @Key("agentActivityTitle")
    String agentActivityTitle ();

    @Key("viewIdent")
    String viewIdent ();

    @Key("search")
    String search ();

    @Key("errNoMessage")
    String errNoMessage ();

    @Key("updatedDate")
    String updatedDate ();

    @Key("qualifiedScore")
    String qualifiedScore ();

    @Key("banAccount")
    String banAccount ();

    @Key("showMessagesToPlayerWarning")
    String showMessagesToPlayerWarning ();

    @Key("hoursItem")
    String hoursItem ();

    @Key("username")
    String username ();

    @Key("warningSet")
    String warningSet ();

    @Key("warn")
    String warn ();

    @Key("loggingOut")
    String loggingOut ();

    @Key("postReply")
    String postReply ();

    @Key("noRelatedAccountsFoundForAccountId")
    String noRelatedAccountsFoundForAccountId (String arg0);

    @Key("addNewFaqQuestion")
    String addNewFaqQuestion ();

    @Key("noPageFoundFor")
    String noPageFoundFor (String arg0);

    @Key("brokenLinkOrSomething")
    String brokenLinkOrSomething ();

    @Key("enterPetitionMessage")
    String enterPetitionMessage ();

    @Key("createCategory")
    String createCategory ();

    @Key("hourHdr")
    String hourHdr ();

    @Key("invalidSession")
    String invalidSession ();

    @Key("billingSubscriber")
    String billingSubscriber ();

    @Key("eventTypeComplaint")
    String eventTypeComplaint ();

    @Key("ignoreSelected")
    String ignoreSelected ();

    @Key("successful")
    String successful (String arg0);

    @Key("youMustBeLoggedIn")
    String youMustBeLoggedIn ();

    @Key("standaloneLink")
    String standaloneLink ();

    @Key("postMessageSubtitleAgainst")
    String postMessageSubtitleAgainst (String arg0, String arg1);

    @Key("eventTypeLegacy")
    String eventTypeLegacy ();

    @Key("hasNote")
    String hasNote ();

    @Key("insider")
    String insider ();

    @Key("neverLoggedOn")
    String neverLoggedOn ();

    @Key("relatedAccounts")
    String relatedAccounts ();

    @Key("enterPetitionSubject")
    String enterPetitionSubject ();

    @Key("petReplies")
    String petReplies (String arg0);

    @Key("agentActivity")
    String agentActivity ();

    @Key("enterAnswer")
    String enterAnswer ();

    @Key("warningUpdated")
    String warningUpdated ();

    @Key("eventTypeLabel")
    String eventTypeLabel ();

    @Key("daysItem")
    String daysItem ();

    @Key("billingTrial")
    String billingTrial ();

    @Key("notAvailable")
    String notAvailable ();

    @Key("eventType")
    String eventType ();

    @Key("advanced")
    String advanced ();

    @Key("noResponse")
    String noResponse ();

    @Key("errEnterQuery")
    String errEnterQuery ();

    @Key("mine")
    String mine ();

    @Key("youDontHavePermission")
    String youDontHavePermission ();
}
