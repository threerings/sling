//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

/**
 * Encapsulates the address of a web page in a sling application.
 */
public class PageAddress
{
    /** The section the page lies in. */
    public SectionId sectionId;

    /** The arguments used to generate the page within the section. */
    public Arguments arguments;

    /**
     * Creates a new address from a GWT token. (The token is the part after the "#" character,
     * not including it.)
     */
    public static PageAddress fromToken (String token)
    {
        SectionId section = null;
        Arguments args = new Arguments();
        for (int pos = 0, len = token.length(), sepPos; pos < len; pos = sepPos + 1) {
            sepPos = wrap(token.indexOf('_', pos), len);
            String sub = token.substring(pos, sepPos);
            if (section == null) {
                section = new SectionId(sub);
                continue;
            }
            args.values.add(unescape(sub));
        }
        return new PageAddress(section == null ? EMPTY_SECTION : section, args);
    }

    /**
     * Creates an address referring to the given section only, with no arguments. Typically this
     * results in the display of a "default" page for the section.
     */
    public PageAddress (SectionId sectionId)
    {
        this(sectionId, new Arguments());
    }

    /**
     * Creates an address referring to the given section and arguments.
     */
    public PageAddress (SectionId sectionId, Arguments arguments)
    {
        this.sectionId = sectionId;
        this.arguments = arguments;
    }

    /**
     * Converts this address to a token.
     */
    public String toToken ()
    {
        StringBuilder tok = new StringBuilder(sectionId.toString());
        for (String arg : arguments.values) {
            tok.append("_").append(escape(arg));
        }
        return tok.toString();
    }

    @Override // from Object
    public String toString ()
    {
        return toToken();
    }

    protected static String escape (String arg)
    {
        if (arg.equals("")) {
            return ".(nil)";
        }
        StringBuilder fragment = new StringBuilder();
        for (int pos = 0, len = arg.length(), gremlin; ; pos = gremlin + 1) {
            gremlin = Math.min(
                wrap(arg.indexOf('.', pos), len),
                wrap(arg.indexOf('_', pos), len));
            fragment.append(arg, pos, gremlin);
            if (gremlin >= len) {
                break;
            }
            switch (arg.charAt(gremlin)) {
            case '_': fragment.append(".(und)"); break;
            case '.': fragment.append(".(dot)"); break;
            }
        }
        return fragment.toString();
    }

    protected static String unescape (String fragment)
    {
        StringBuilder arg = new StringBuilder();
        for (int pos = 0, len = fragment.length(), gremlin; ; pos = gremlin + 6) {
            gremlin = wrap(fragment.indexOf('.', pos), len);
            arg.append(fragment, pos, gremlin);
            if (gremlin >= len) {
                break;
            }
            if (gremlin + 1 >= len || fragment.charAt(gremlin + 1) != '(' ||
                gremlin + 5 >= len || fragment.charAt(gremlin + 5) != ')') {
                throw new IllegalArgumentException("Illegal fragment: " + fragment);
            }
            String esc = fragment.substring(gremlin + 2, gremlin + 5);
            esc = esc.equals("und") ? "_" :
                  esc.equals("dot") ? "." :
                  esc.equals("nil") ? "" : null;
            if (esc == null) {
                throw new IllegalArgumentException("Unrecognized escape in fragment " + fragment);
            }
            arg.append(esc);
        }
        return arg.toString();
    }

    protected static int wrap (int pos, int len)
    {
        return pos == -1 ? len : pos;
    }

    protected static final SectionId EMPTY_SECTION = new SectionId("");
}
