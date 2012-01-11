//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.sling.web.data.Question;

/**
 * Contains a single FAQ entry.
 */
@Entity
public class QuestionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<QuestionRecord> _R = QuestionRecord.class;
    public static final ColumnExp<Integer> QUESTION_ID = colexp(_R, "questionId");
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<String> QUESTION = colexp(_R, "question");
    public static final ColumnExp<String> ANSWER = colexp(_R, "answer");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you make a change to this class that must be propagated to its
     * database representation. */
    public static final int SCHEMA_VERSION = 1;

    /** A unique identifier for this question. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int questionId;

    /** The category in which this question resides. */
    public int categoryId;

    /** The text of the question. */
    public String question;

    /** The text of the question. */
    @Column(length=65535)
    public String answer;

    /**
     * A zero argument constructor for use by Depot.
     */
    public QuestionRecord ()
    {
    }

    /**
     * Configures a question record with the contents of the supplied object.
     */
    public QuestionRecord (Question quest)
    {
        questionId = quest.questionId;
        categoryId = quest.categoryId;
        question = quest.question;
        answer = quest.answer;
    }

    /**
     * Converts this question record to an over the wire object.
     */
    public Question toQuestion ()
    {
        Question record = new Question();
        record.questionId = questionId;
        record.categoryId = categoryId;
        record.question = question;
        record.answer = answer;
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link QuestionRecord}
     * with the supplied key values.
     */
    public static Key<QuestionRecord> getKey (int questionId)
    {
        return newKey(_R, questionId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(QUESTION_ID); }
    // AUTO-GENERATED: METHODS END
}
