/*
 * Copyright 2007 Three Rings Design, Inc.
 * Copyright 1999,2004 The Apache Software Foundation.
 */

package com.threerings.jsp.taglib.i18n;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;

import com.threerings.jsp.taglib.utils.CookieUtils;

/**
 * A re-usable locale management filter. Handles determining the user's
 * locale based on request parameters, cookies, and the user's session.
 *
 * The class supports two init parameters:
 *   strip-cookie-domain: If true, the first hostname element (eg www.) will be removed from the locale cookie domain.
 *   default-locale: Set the default locale, when no other is set (defaults to en_US).
 */
public class DefaultLocaleFilter implements Filter {
    /**
     * Default locale request attribute name. This attribute is set to the request's preferred
     * locale.
     */
    public static final String LOCALE_ATTRIBUTE = DefaultLocaleFilter.class.getName() + ".Locale";

    /** User's locale cookie name. */
    protected static final String LOCALE_COOKIE = DigestUtils.md5Hex(DefaultLocaleFilter.class.getName()
        + ".Cookie");

    /**
     * Filter initialization.
     */
    public void init (FilterConfig config)
        throws ServletException
    {
        String param;

        /* Should we strip the hostname off our domain? Default to true. */
        param = config.getInitParameter("strip-cookie-domain");
        _cookieStripDomain = param == null || param.equalsIgnoreCase("true");

        /* Set the default (fallback) locale. */
        param = config.getInitParameter("default-locale");
        if (param != null) {
            _defaultLocale = LocaleUtils.parseLocale(param);
            if (_defaultLocale == null) {
                throw new ServletException("Could not parse defaul-locale: " + param);
            }
        }
    }

    /**
     * Filter destruction.
     */
    public void destroy ()
    {
    // Nothing
    }

    /**
     * Locates and parses the locale cookie, if available, returning a Locale instance, or null if
     * the cookie can not be found (or parsed).
     */
    public void doFilter (ServletRequest servletRequest, ServletResponse servletResponse,
        FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest)servletRequest;
        HttpServletResponse resp = (HttpServletResponse)servletResponse;
        Locale newLocale = null;

        /* Retrieve the new locale (or null, for no change) */
        newLocale = getLocale(req);

        /* Cache the locale in the session, and add (or reset) the cookie */
        if (newLocale != null) {
            /* Session cache */
            req.getSession().setAttribute(LOCALE_ATTRIBUTE, newLocale);

            /* (Re)set the cookie. */
            setResponseCookie(req, resp, newLocale);
        }

        /* Pass the request on. */
        chain.doFilter(req, resp);
    }

    /**
     * Create and add the local cookie to the servlet response.
     *
     * @param req The HTTP servlet request.
     * @param resp The HTTP servlet response
     * @param locale The locale value to set.
     */
    private void setResponseCookie (HttpServletRequest req, HttpServletResponse resp,
        Locale locale)
    {
        CookieUtils.setCookie(req, resp, LOCALE_COOKIE, locale.toString(), 2 * 365,
            _cookieStripDomain);
    }

    /**
     * Retrieve the to-be-set locale, or null if the locale should not be changed. First match
     * returns 1) If the locale is specified in the request parameters, it overrides all other
     * settings, and is returned first. 2) If the locale is cached in the session, we use that and
     * change nothing. 3) If the locale is available via a cookie, we use the cookie-defined
     * locale and cache the Locale instance in the user's session. 4) Lastly, we fall back to the
     * default locale.
     *
     * @param req The HTTP servlet request.
     * @return A new {@link Locale}, or null if the locale should not be changed
     */
    private Locale getLocale (HttpServletRequest req)
    {
        Locale locale;

        /* Test for the locale parameter override. */
        if ((locale = getLocaleParameter(req)) != null) {
            return locale;
        }

        Locale sessionLocale = (Locale)req.getSession().getAttribute(LOCALE_ATTRIBUTE);
        Locale cookieLocale = getLocaleCookie(req);
        if (sessionLocale != null && sessionLocale.equals(cookieLocale)) {
            // they're not null and equal, what we have in the session is valid
            return null;
        } else if (cookieLocale != null) {
            // either the session locale is empty or it's not equal to our cookie, reset it
            return cookieLocale;
        }

        /* Use default language and country from the browser */
        if ((locale = getBrowserLocale(req)) != null) {
            return locale;
        }

        /* Fallback to the default locale (language and country) */
        return _defaultLocale;
    }

    /**
     * Parses the locale (and lang) parameters if available, returning a Locale instance, or null
     * if the parameters are not found.
     *
     * @param req The HTTP servlet request.
     * @return An instance of {@link java.util.Locale}.
     */
    private Locale getLocaleParameter (HttpServletRequest req)
    {
        String param;

        /* Try the standard 'locale' first, and then fall back to 'lang' */
        if ((param = req.getParameter("locale")) == null) {
            param = req.getParameter("lang");
        }

        /* No parameters set. */
        if (param == null) {
            return null;
        }

        /* Parse the locale string */
        return LocaleUtils.parseLocale(param);
    }

    /**
     * Locates and parses a locale cookie, if available, returning a corresponding Locale
     * instance, or null if the cookie is not found.
     *
     * @param req The HTTP servlet request.
     * @return An instance of {@link java.util.Locale}.
     */
    private Locale getLocaleCookie (HttpServletRequest req)
    {
        String cookieValue = CookieUtils.getCookie(req, LOCALE_COOKIE);
        if (cookieValue != null) {
            return LocaleUtils.parseLocale(cookieValue);
        }
        return null;
    }

    /**
     * Fetch the visitor's locale from their browser via the request object.
     */
    private Locale getBrowserLocale (HttpServletRequest req)
    {
        return req.getLocale();
    }

    /** The default fall-back locale. Defaults to US. */
    private Locale _defaultLocale = Locale.US;

    /** Should we remove the server hostname (eg www) from the locale cookie? */
    private Boolean _cookieStripDomain;
}
