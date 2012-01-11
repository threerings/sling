//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.samskivert.servlet.SiteIdentifier;

import com.threerings.sling.server.persist.SlingRepository;
import com.threerings.sling.web.data.AccountName;

/**
 * Contains references to bits needed by server components and some configuration.
 */
public class SlingEnvironment
{
    public final SiteIdentifier site;
    public final GameInfoProvider info;
    public final GameActionHandler action;
    public final UserLogic user;
    public final SlingRepository urepo;

    public final Predicate<String> notDeleted = new Predicate<String> () {
        public boolean apply (String name) {
            return !info.isDeleted(name);
        }
    };

    public SlingEnvironment (SiteIdentifier site, GameInfoProvider info, GameActionHandler action,
                         UserLogic user, SlingRepository urepo)
    {
        this.info = info;
        this.action = action;
        this.site = site;
        this.user = user;
        this.urepo = urepo;
    }

    /**
     * Converts an account name to a name to be used for displaying replies to user petitions.
     * By default, omits the account name and keeps all the undeleted game names.
     */
    public AccountName toHandle (AccountName name)
    {
        return withoutDeletedGameNames(new AccountName("", name.gameNames));
    }

    /**
     * Returns the default language to use for events created by the servlet, if none could be
     * determined from other information. Note that for sling, languages are two letter codes.
     * By default, returns the code for English, "en".
     */
    public String getDefaultLanguage ()
    {
        return "en";
    }

    /**
     * Returns the languages that may be used by the servlet when creating events. If a user's
     * language is set to a value not in this set, the default will be used. The set must always
     * contain the value returned by {@link #getDefaultLanguage()}. Note that for sling, languages
     * are two letter codes. By default, returns the singleton containing the default language.
     */
    public Set<String> getSupportedLanguages ()
    {
        return Collections.singleton(getDefaultLanguage());
    }

    /**
     * Returns the equivalent of the given name but without any deleted game names.
     * @see GameInfoProvider#isDeleted(String)
     */
    public AccountName withoutDeletedGameNames (AccountName name)
    {
        if (Iterables.all(name.gameNames, notDeleted)) {
            return name;
        }

        return new AccountName(name.accountName,
                Lists.newArrayList(Iterables.filter(name.gameNames, notDeleted)));
    }

    /**
     * Returns the name of the session cookie set when a user logs in and clear when they log out.
     * The cookie value is a session id value from the user db sessions table. By default, a sling
     * standalone mode cookie name is returned. Override to share authentication state with
     * another webapp.
     */
    public String getSessionCookieName ()
    {
        return "slingtok";
    }

    /**
     * Returns a URL to redirect the user to if a page is requested that requires a session and
     * the user is not logged in. By default, returns null, which causes the sling client to
     * display a standalone sling login page. Override to redirect to a main login page.
     */
    public String getLoginRedirectUrl ()
    {
        return null;
    }

    /**
     * Returns a URL to redirect the user to when a logout is requested. By default, returns
     * null, which causes the sling client to display a standalone sling login page. Override to
     * redirect to a main logout page.
     */
    public String getLogoutRedirectUrl ()
    {
        return null;
    }
}
