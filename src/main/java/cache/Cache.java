package cpen221.mp3.cache;

import java.util.concurrent.ConcurrentHashMap;

public class Cache <T extends Cacheable> {

    /* the default cache size is 32 objects */
    private static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    private static final int DTIMEOUT = 3600;

    private final int capacity;
    private final int timeout;

    private ConcurrentHashMap<CacheObject<T>, Boolean> data;
    private final long t0 = System.nanoTime();

    /**
     * Cache Rep Invariants
     *
     * capacity >= 0
     * timeout >= 0
     * data is not null and does not contain null elements
     * t0 >= 0
     *
     * CacheObject Rep Invariants
     *
     * t is not null
     * lastAccessed >= 0
     * lastUpdated >= 0
     *
     * ---------------------------------------------------------------------------
     *
     * Cache Abstraction Functions
     *
     * data -> a set of the elements stored in the cache
     * capacity -> the maximum number of total elements allowed in the cache at
     *             any given instant
     * timeout  -> the maximum amount of time an element is allowed to stay in
     *             the cache without being accessed before being removed
     *
     * CacheObject Abstraction Functions
     *
     * t -> a value/element stored in the cache
     * lastUpdated -> the time the value was most recently updated
     * lastAccessed -> the time the value was most recently accessed
     */

    /**
     * Create a cache with a fixed capacity and a timeout value.
     * Objects in the cache that have not been refreshed within the timeout period
     * are removed from the cache.
     *
     * @param capacity the number of objects the cache can hold
     * @param timeout  the duration (in seconds) an object should be in the cache
     *                 before it times out
     */
    public Cache(int capacity, int timeout) {
        if (capacity < 0 || timeout < 0) {
            throw new IllegalArgumentException("Negative capacity or timeout");
        }

        this.capacity = capacity;
        this.timeout = timeout;
        this.data = new ConcurrentHashMap<>();
    }

    /**
     * Create a cache with default capacity and timeout values.
     */
    public Cache() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the cache.
     * If the cache is full then remove the least recently accessed object to
     * make room for the new object. Update the value if it is already in
     * the cache.
     *
     * @param t the value to store in the cache
     * @return true if the value was successfully stored and not previously in
     *         the cache, false otherwise
     */
    public synchronized boolean put(T t) {
        CacheObject<T> val = new CacheObject<>(t);

        expire();
        if (this.data.containsKey(val)) {
            return update(t);
        } else if (this.data.size() >= this.capacity) {
            removeLeastRecentlyRequested();
            if (this.capacity == 0) {
                return false;
            } else {
                this.data.put(val, true);
                return true;
            }
        } else {
            this.data.put(val, true);
            return true;
        }
    }

    /**
     * Retrieve a value from the cache.
     *
     * @param id the identifier of the object to be retrieved
     * @return the value that matches the identifier from the cache
     * @throws NoSuchCacheElementException if the cache does not contain
     *         the value with identifier id
     */
    public synchronized T get(String id) throws NoSuchCacheElementException {
        expire();
        for (CacheObject<T> c : this.data.keySet()) {
            if (c.t.id().equals(id)) {
                c.lastAccessed = currentTime();
                return c.t;
            }
        }

        throw new NoSuchCacheElementException();
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * Does not count as accessing the object.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public synchronized boolean touch(String id) {
        expire();
        for (CacheObject c : this.data.keySet()) {
            if (c.t.id().equals(id)) {
                c.lastUpdated = currentTime();
                return true;
            }
        }

        return false;
    }

    /**
     * Update the data held by and creation time of the specified object
     * in the cache. Does not count as accessing the object.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    public synchronized boolean update(T t) {
        ConcurrentHashMap<CacheObject<T>, Boolean> values = new ConcurrentHashMap<>(this.data);
        CacheObject<T> updatedItem = new CacheObject<>(t);

        boolean updated = false;
        for (CacheObject<T> c : values.keySet()) {
            if (c.equals(updatedItem)) {
                updatedItem.lastAccessed = c.lastAccessed;
                this.data.remove(c);
                this.data.put(updatedItem, true);
                updated = true;
                break;
            }
        }

        return updated;
    }

    /**
     * Remove the least recently requested value in the cache, if
     * one exists.
     */
    private synchronized void removeLeastRecentlyRequested() {
        if (this.data.isEmpty()) {
            return;
        }

        CacheObject<T> least = null;
        long leastRecentlyRequested = Long.MAX_VALUE;
        for (CacheObject<T> c : this.data.keySet()) {
            if (c.lastAccessed < leastRecentlyRequested) {
                least = c;
                leastRecentlyRequested = c.lastAccessed;
            }
        }

        this.data.remove(least);
    }

    /**
     * Remove all values in the cache that have not been refreshed within
     * the timeout period
     */
    private synchronized void expire() {
        ConcurrentHashMap<CacheObject<T>, Boolean> values = new ConcurrentHashMap<>(this.data);
        for (CacheObject<T> c : values.keySet()) {
            if (c.lastUpdated + this.timeout * 1000000000 < currentTime()) {
                this.data.remove(c);
            }
        }
    }

    /**
     * Get the time since the cache was created.
     *
     * @return the number of nanoseconds since the cache was created.
     */
    private synchronized long currentTime() {
        return System.nanoTime() - t0;
    }

    /**
     * An object that holds the values in the cache along with their
     * associated metadata (time of last access and number of requests)
     */
    private class CacheObject<S extends T> {
        private T t;
        private long lastUpdated;
        private long lastAccessed;

        /**
         * Create a CacheObject
         *
         * @param t the value to store
         */
        private CacheObject(T t) {
            this.t = t;
            this.lastUpdated = currentTime();
            this.lastAccessed = currentTime();
        }

        /**
         * Make a hashcode for the CacheObject
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            return t.id().hashCode();
        }

        /**
         * Determine if this and another CacheObject are equal
         *
         * @param o other CacheObject to compare to
         * @return true if this and o are equal, false otherwise
         */
        @Override
        public synchronized boolean equals(Object o) {
            if (o instanceof CacheObject) {
                CacheObject c = (CacheObject<?>) o;
                if (this.t.id().equals(c.t.id())) {
                    return true;
                }
            }
            return false;
        }
    }

}
