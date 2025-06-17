//
// $Id: $

package com.threerings.jsp.taglib.i18n;

import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Re-usable locale-related functions.
 * This class is unsupported and should not be considered stable, public API.
 */
public class LocaleUtils {
    /**
     * Parse a locale string in the 'underbar' format, eg, 'en_US@WIN' to a Locale
     * object. Returns null if the string can not be parsed.
     *
     * @param localeString A locale string in the format of 'en_US@WIN'
     * @return A {@link Locale} instance, or null if the locale string could not be parsed.
     */
    public static Locale parseLocale (String localeString)
    {
        StringTokenizer st;
        String language = null;
        String country = "";
        String variant = "";

        st = new StringTokenizer(localeString, "_@");

        /* Fetch the language. */
        if (st.hasMoreElements())
            language = st.nextToken();

        /* Country */
        if (st.hasMoreElements())
            country = st.nextToken();

        /* Variant */
        if (st.hasMoreElements())
            variant = st.nextToken();

        /* Language is required, other elements are optional. */
        if (language == null)
            return null;

        return new Locale(language, country, variant);
    }
}
