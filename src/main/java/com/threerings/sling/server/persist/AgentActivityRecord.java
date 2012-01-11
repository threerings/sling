//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Record the last time at which an agent did something that looked like being on duty.
 */
@Entity
public class AgentActivityRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AgentActivityRecord> _R = AgentActivityRecord.class;
    public static final ColumnExp<String> ACCOUNT_NAME = colexp(_R, "accountName");
    public static final ColumnExp<Timestamp> TIME = colexp(_R, "time");
    // AUTO-GENERATED: FIELDS END

    @Id public String accountName;
    public Timestamp time;
    public static final int SCHEMA_VERSION = 1;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AgentActivityRecord}
     * with the supplied key values.
     */
    public static Key<AgentActivityRecord> getKey (String accountName)
    {
        return newKey(_R, accountName);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ACCOUNT_NAME); }
    // AUTO-GENERATED: METHODS END
}
