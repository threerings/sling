//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.util;

import java.util.concurrent.ConcurrentMap;

import com.threerings.sling.web.client.SlingException;

/**
 * A simple cache for keeping data from sling repos close to hand. Each item in the cache is
 * allowed to live for a number of milliseconds given on construction. Items are automatically
 * removed after they have been around for that long. This uses a ConcurrentMap internally so is
 * threadsafe. Unlike concurrent map, however, values may be null.
 *
 * @param <K> the unique id for a cache datum
 * @param <V> the type of cache data
 * @see ConcurrentMap
 */
public abstract class SimpleCache<K, V>
{
    public final long ttlMillis;

    /**
     * Creatse a new simple cache with the given time to live.
     * @param ttlMillis milliseconds after which an entry is considered old and should be removed
     */
    public SimpleCache (long ttlMillis)
    {
        this.ttlMillis = ttlMillis;
    }

    /**
     * Gets the cache value corresponding to the given key. If the entry does not yet exist, a
     * new entry is put in using {@link #compute()}.
     */
    public V get (K key)
        throws SlingException
    {
        CacheEntry<V> entry = _cache.get(key);
        if (entry != null && entry.age() > ttlMillis) {
            _cache.remove(key, entry);
            return get(key);
        }

        if (entry == null) {
            _cache.putIfAbsent(key, CacheEntry.wrap(compute(key)));
            return get(key);
        }

        return entry.value;
    }

    /**
     * Manually adds the given entry to the cache. This sidesteps the {@link #compute()} mechanism
     * for situations in which the caller has the cache value as a side effect of some other
     * operation.
     */
    public void put (K key, V value)
    {
        _cache.put(key, CacheEntry.wrap(value));
    }

    /**
     * Gets an iterable containing all values in the cache. This operation first removes expired
     * entries.
     */
    public Iterable<V> values ()
    {
        _cache.removeOld(ttlMillis);
        return _cache.unwrappedValues();
    }

    /**
     * Removes all items from the cache.
     */
    public void clear ()
    {
        _cache.clear();
    }

    /**
     * Removes the cache entry corresponding to the given key.
     */
    public void remove (K key)
    {
        _cache.remove(key);
    }

    /**
     * Computes the value for the given key, normally by accessing the database via a sling
     * repository.
     */
    protected abstract V compute (K key)
        throws SlingException;

    private CacheEntryMap<K, V> _cache = CacheEntryMap.makeNew();
}
