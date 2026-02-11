//
// Sling - Copyright 2012 Three Rings Design, Inc.

package com.threerings.sling.web.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.threerings.sling.web.client.SlingException;

/**
 * A simple cache for keeping data from sling repos close to hand. Each item in the cache is
 * allowed to live for a number of milliseconds given on construction. Items are automatically
 * removed after they have been around for that long. This uses a Cache internally so is
 * threadsafe, but values are wrapped so they may be null.
 *
 * @param <K> the unique id for a cache datum
 * @param <V> the type of cache data
 */
public abstract class SimpleCache<K, V>
{
    /**
     * Creatse a new simple cache with the given time to live.
     * @param ttlMillis milliseconds after which an entry is considered old and should be removed
     */
    public SimpleCache (long ttlMillis)
    {
        _cache = CacheBuilder.newBuilder()
            .expireAfterWrite(ttlMillis, TimeUnit.MILLISECONDS)
            .build(new CacheLoader<K, Optional<V>>() {
                public Optional<V> load (K key)
                    throws SlingException
                {
                    return Optional.fromNullable(compute(key));
                }
            });
    }

    /**
     * Gets the cache value corresponding to the given key. If the entry does not yet exist, a
     * new entry is put in using {@link #compute}.
     */
    public V get (K key)
        throws SlingException
    {
        try {
            return _cache.get(key).orNull();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SlingException) {
                throw (SlingException)cause;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * Manually adds the given entry to the cache. This sidesteps the {@link #compute} mechanism
     * for situations in which the caller has the cache value as a side effect of some other
     * operation.
     */
    public void put (K key, V value)
    {
        _cache.put(key, Optional.fromNullable(value));
    }

    /**
     * Gets an iterable containing all non-null values in the cache.
     */
    public Iterable<V> values ()
    {
        return Optional.presentInstances(_cache.asMap().values());
    }

    /**
     * Removes all items from the cache.
     */
    public void clear ()
    {
        _cache.invalidateAll();
    }

    /**
     * Removes the cache entry corresponding to the given key.
     */
    public void remove (K key)
    {
        _cache.invalidate(key);
    }

    /**
     * Computes the value for the given key, normally by accessing the database via a sling
     * repository.
     */
    protected abstract V compute (K key)
        throws SlingException;

    private final LoadingCache<K, Optional<V>> _cache;
}
