//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.util;

import com.google.common.base.Function;

/**
 * Represents a value in a map and an associated creation time.
 */
public class CacheEntry<T>
{
    /**
     * Convenience method to create a new cache entry for the given value.
     */
    public static <V> CacheEntry<V> wrap (V val)
    {
        return new CacheEntry<V>(val);
    }

    /**
     * Gets a function that returns the value of a cache entry.
     */
    public static <V> Function<CacheEntry<V>, V> unwrapper ()
    {
        return new Function<CacheEntry<V>, V>() {
            public V apply(CacheEntry<V> value) {
                return value.value;
            }
        };
    }

    /** The value in this entry. */
    public final T value;

    /** The system milliseconds when this entry was constructed. */
    public final long created;

    /**
     * Creates a new cache entry with the given value.
     */
    public CacheEntry (T value)
    {
        this.value = value;
        this.created = System.currentTimeMillis();
    }

    /**
     * Gets the current age of the cache entry, using the current system time.
     */
    public long age ()
    {
        return age(System.currentTimeMillis());
    }

    /**
     * Gets the age of the cache entry, using the given value for the current time.
     */
    public long age (long now)
    {
        return now - created;
    }
}
