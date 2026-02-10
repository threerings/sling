/*
 * Copyright 2007 Three Rings Design, Inc.
 * Copyright 1999,2004 The Apache Software Foundation.
 */

package com.threerings.jsp.taglib.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utilities for getting and setting cookies.
 */
public class CookieUtils {

    /**
     * Create a cookie and add it to the response object.
     * @param req Request object that holds the host name
     * @param rsp Response to add the cookie to
     * @param cookieName Name of the cookie
     * @param cookieValue String value for the cookie
     * @param ageDays Number of days for the cookies to live.  Set to 0 to remove a cookie.
     * @param stripDomain If set to true, cookie domain will be the server host with subdomain
     *        stripped and a leading period (eg .domain.com).  For hosts with no periods
     *        like "localhost", the cookie domain will not be set.
     */
    public static void setCookie (
        HttpServletRequest req, HttpServletResponse rsp, String cookieName, String cookieValue,
        int ageDays, boolean stripDomain)
    {
        final Cookie cookie = new Cookie(cookieName, cookieValue);

        /* Cookie lives X days */
        cookie.setMaxAge(ageDays * 24 * 60 * 60);

        /* Cookie is available from all pages */
        cookie.setPath("/");

        /* Strip the web server's hostname, if requested */
        if (stripDomain) {

            String host = req.getServerName();
            int idx = host.indexOf(".");

            /* Check that a '.' was found, and that we're not stripping the
             * host to the TLD (eg .com or .local). If all is well, drop the first component of the FQDN */
            if (idx != -1 && host.indexOf(".", idx+1) != -1)
                cookie.setDomain(host.substring(idx));
            /* if a single period was found (eg domain.com) add a leading period */
            else if (idx != -1)
                cookie.setDomain("." + host);
        }

        rsp.addCookie(cookie);
    }

    /**
     * If the cookie exists in the request, add a cookie to the response that expires
     * immediately, thus clearing any existing cookie.
     */
    public static void deleteCookie (
        HttpServletRequest req, HttpServletResponse rsp, String cookieName, boolean stripDomain)
    {
        /* If there are no request cookies, there's nothing to do. */
        if (req.getCookies() == null) {
            return;
        }

        /* Find and clear our cookie. */
        for (Cookie cookie : req.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                setCookie(req, rsp, cookieName, null, 0, stripDomain);
            }
        }
    }

    /**
     * Retrieve a single cookie by name from the servlet request.  If there is more than one
     * cookie under that name, return the first one.
     */
    public static String getCookie (HttpServletRequest req, String cookieName)
    {
        try {
            /* If there are no request cookies, there's nothing to do. */
            if (req.getCookies() == null) {
                return null;
            }

            /* Find and parse our cookie, or fall through and return null. */
            for (Cookie cookie : req.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }

            return null;
        }
        /* Jetty's req.getCookies throws occasional StringIndexOutOfBoundsException - bots? */
        catch (StringIndexOutOfBoundsException e) {
            return null;
        }
    }
}
