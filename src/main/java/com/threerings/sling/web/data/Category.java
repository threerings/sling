//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a FAQ category.
 */
public class Category
    implements IsSerializable
{
    /** The unique identifier for this category. */
    public int categoryId;

    /** The human readable name of this category. */
    public String name;

    /** The questions in this FAQ category. */
    public List<Question> questions;

    /**
     * Returns a string representation of this instance.
     */
    @Override public String toString ()
    {
        int qcount = (questions == null) ? 0 : questions.size();
        return "[cid=" + categoryId + ", name=" + name + ", qcount=" + qcount + "]";
    }
}
