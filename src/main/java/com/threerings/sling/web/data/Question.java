//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a FAQ question.
 */
public class Question
    implements IsSerializable
{
    /** The unique identifier for this question. */
    public int questionId;

    /** The identifier of the category to which this question belongs. */
    public int categoryId;

    /** The question text. */
    public String question = "";

    /** The answer text (may contain markup). */
    public String answer = "";

    /**
     * Returns a string representation of this instance.
     */
    @Override public String toString ()
    {
        return "[qid=" + questionId + ", cid=" + categoryId +
            ", q=" + question + ", a=" + answer + "]";
    }
}
