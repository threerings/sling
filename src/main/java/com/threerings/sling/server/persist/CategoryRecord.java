//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.util.ArrayList;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.sling.web.data.Category;
import com.threerings.sling.web.data.Question;

/**
 * Defines a category of FAQ questions.
 */
@Entity
public class CategoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CategoryRecord> _R = CategoryRecord.class;
    public static final ColumnExp<Integer> CATEGORY_ID = colexp(_R, "categoryId");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you make a change to this class that must be propagated to its
     * database representation. */
    public static final int SCHEMA_VERSION = 1;

    /** A unique identifier for this category. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int categoryId;

    /** The name of the category. */
    public String name;

    /**
     * A zero argument constructor for use by Depot.
     */
    public CategoryRecord ()
    {
    }

    /**
     * Configures a category record with the contents of the supplied object.
     */
    public CategoryRecord (Category category)
    {
        categoryId = category.categoryId;
        name = category.name;
    }

    /**
     * Converts this category record to an over the wire object.
     */
    public Category toCategory ()
    {
        Category record = new Category();
        record.categoryId = categoryId;
        record.name = name;
        record.questions = new ArrayList<Question>();
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CategoryRecord}
     * with the supplied key values.
     */
    public static Key<CategoryRecord> getKey (int categoryId)
    {
        return newKey(_R, categoryId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(CATEGORY_ID); }
    // AUTO-GENERATED: METHODS END
}
