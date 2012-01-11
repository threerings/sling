//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.gwt.util;

/**
 * Holds a section identifier string.
 */
public class SectionId
{
    /**
     * Creates a new section identifier with the given string.
     */
    public SectionId (String sectionId)
    {
        _sectionId = sectionId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return other instanceof SectionId && ((SectionId)other)._sectionId.equals(_sectionId);
    }

    @Override // from Object
    public int hashCode ()
    {
        return _sectionId.hashCode();
    }

    @Override // from Object
    public String toString ()
    {
        return _sectionId;
    }

    private String _sectionId;
}
