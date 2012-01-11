//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ForwardingConcurrentMap;
import com.google.common.collect.Maps;

/**
 * A concurrent map of keys to CacheEntry instances. All operations are forwarding to an underlying
 * concurrent map. Some basic functionality for removing old entries is provided.
 *
 * @param <K> the key type of the map
 * @param <V> the value type of the cache entries in the map
 */
public class CacheEntryMap<K, V> extends ForwardingConcurrentMap<K, CacheEntry<V>>
{
    /**
     * Convenience method to create a new cache entry map.
     */
    public static <K, V> CacheEntryMap<K, V> makeNew ()
    {
        return new CacheEntryMap<K, V>();
    }

    /**
     * Gets the value of the cache entry of the given key, which may be null, or null if the key
     * is not present.
     */
    public V getUnwrappedValue (K key)
    {
        CacheEntry<V> entry = get(key);
        return entry == null ? null : entry.value;
    }

    /**
     * Returns a collection of cache entry values, some of which may be null if any null values
     * were inserted.
     */
    public Collection<V> unwrappedValues ()
    {
        return Collections2.transform(values(), _unwrapper);
    }

    /**
     * Removes all entries aged more than the given limit.
     * @param now the time to use as the current time, see {@link CacheEntry#age(long)}.
     * @param ageLimit the maximum age item to retain
     * @return the number of entries removed
     */
    public int removeOld (long now, long ageLimit)
    {
        int removed = 0;
        for (Iterator<CacheEntry<V>> it = values().iterator(); it.hasNext(); ) {
            CacheEntry<V> entry = it.next();
            if (entry.age(now) > ageLimit) {
                it.remove();
                ++removed;
            }
        }
        return removed;
    }

    /**
     * Removes all entries aged more than the given limit. This is the same as
     * {@link #removeOld(long, long)}, with the current system time as the now parameter.
     */
    public int removeOld (long ageLimit)
    {
        return removeOld(System.currentTimeMillis(), ageLimit);
    }

    @Override protected ConcurrentMap<K, CacheEntry<V>> delegate ()
    {
        return _delegate;
    }

    protected ConcurrentMap<K, CacheEntry<V>> _delegate = Maps.newConcurrentMap();
    protected Function<CacheEntry<V>, V> _unwrapper = CacheEntry.unwrapper();
}
