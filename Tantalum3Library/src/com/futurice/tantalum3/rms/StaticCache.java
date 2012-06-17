package com.futurice.tantalum3.rms;

import com.futurice.tantalum3.Result;
import com.futurice.tantalum3.Workable;
import com.futurice.tantalum3.Worker;
import com.futurice.tantalum3.log.Log;
import com.futurice.tantalum3.util.LRUVector;
import com.futurice.tantalum3.util.SortedVector;
import com.futurice.tantalum3.util.WeakHashCache;
import java.util.Vector;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;

/**
 * A cache which returns Objects based on a String key asynchronously from RAM,
 * RMS, or network and synchronously from RAM and RMS.
 *
 * Objects in RAM are kept with WeakReferences so they may be garbage collected.
 *
 * Objects in RMS are managed in a "least recently accessed" form to make space.
 *
 * Each StaticCache uses a single RMS and may be referred to by name.
 *
 * You may provide alternative MODEs to change the default characteristics of a
 * given StaticCache.
 */
public class StaticCache {

    protected static final int DATA_TYPE_IMAGE = 1;
    protected static final int DATA_TYPE_XML = 2;
    private static final SortedVector caches = new SortedVector(new SortedVector.Comparator() {

        public boolean before(final Object o1, final Object o2) {
            return ((StaticCache) o1).priority < ((StaticCache) o2).priority;
        }
    });
    private static final Vector rmsWriteWorkables = new Vector();
    private static final Workable writeAllPending = new Workable() {

        public boolean work() {
            try {
                Workable work;
                while (!rmsWriteWorkables.isEmpty()) {
                    synchronized (rmsWriteWorkables) {
                        work = (Workable) rmsWriteWorkables.firstElement();
                        rmsWriteWorkables.removeElementAt(0);
                    }
                    work.work();
                    if (!rmsWriteWorkables.isEmpty()) {
                        try {
                            // DEBUG TEST- be kind to slow phones, avoid crashes
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            } catch (Exception e) {
                Log.l.log("Can not write all pending", "", e);
            }

            return false;
        }
    };
    protected final WeakHashCache cache = new WeakHashCache();
    protected final LRUVector accessOrder = new LRUVector();
    protected final char priority; // Must be unique, preferrably and integer, larger characters get more space when space is limited
    protected final DataTypeHandler handler;
    protected int sizeAsBytes = 0;

    /**
     * Create a named cache
     *
     * Caches with higher priority are more likely to keep their data when space
     * is limited.
     *
     * @param name
     * @param priority, a character from '0' to '9', higher numbers get a
     * preference for space
     * @param handler
     */
    public StaticCache(final char priority, final DataTypeHandler handler) {
        this.priority = priority;
        this.handler = handler;

        if (priority < '0') {
            throw new IllegalArgumentException("Priority=" + priority + " is invalid, must be '0' or higher");
        }
        synchronized (caches) {
            for (int i = 0; i < caches.size(); i++) {
                if (((StaticCache) caches.elementAt(i)).priority == priority) {
                    throw new IllegalArgumentException("A StaticCache with priority=" + priority + " already exists");
                }
            }
            caches.addElement(this);
        }
    }

    /**
     * Synchronously put the hash object to the RAM cache.
     *
     * If you also want the object stored in RMS, call put()
     *
     * @param key
     * @param o
     */
    protected synchronized Object convertAndPutToHeapCache(final String key, final byte[] bytes) {
        //#debug
        Log.l.log("Start to convert", key);
        final Object o = handler.convertToUseForm(bytes);
        accessOrder.addElement(key);
        cache.put(key, o);
        //#debug
        Log.l.log("End convert", key);

        return o;
    }

    /**
     * Synchronously return the hash object
     *
     * @param key
     * @return
     */
    public synchronized Object synchronousRAMCacheGet(final String key) {
        Object o = null;

        if (containsKey(key)) {
//            Log.l.log("StaticCache hit in RAM", key);
            this.accessOrder.addElement(key);
            o = cache.get(key);
        }

        return o;
    }

    /**
     * Retrieve an object from RAM or RMS storage.
     *
     *
     * @param key
     * @param result
     * @param highPriority - set true if you want the results quickly. Note that
     * your request for priority may be denied if you have recently changed the
     * value and the value happens to have expired from the RAM cache due to a
     * low memory condition.
     */
    public void get(final String key, final Result result, final boolean highPriority) {
        if (key == null || key.length() == 0) {
            Log.l.log("Trivial get", "");
            result.noResult();
            return;
        }
        final Object ho = synchronousRAMCacheGet(key);

        if (ho != null) {
            //#debug
            Log.l.log("RAM cache hit", "(" + priority + ") " + key);
            result.setResult(ho);
        } else {
            final Workable getWorkable = new Workable() {

                public boolean work() {
                    try {
                        final Object o = synchronousGet(key);

                        if (o != null) {
                            //#debug
                            Log.l.log("RMS cache hit", key);
                            result.setResult(o);
                        } else {
                            //#debug
                            Log.l.log("RMS cache miss", key);
                            result.noResult();
                        }
                    } catch (Exception e) {
                        //#debug
                        Log.l.log("Can not get", key, e);
                    }

                    return false;
                }
            };

            if (highPriority) {
                Worker.queuePriority(getWorkable);
            } else {
                Worker.queue(getWorkable);
            }
        }
    }

    private Object synchronousGet(final String key) {
        Object o = synchronousRAMCacheGet(key);

        if (o == null) {
            // Load from flash memory
            final byte[] bytes = RMSUtils.cacheRead(key);

            if (bytes != null) {
                //#debug
                Log.l.log("StaticCache hit in RMS", "(" + priority + ") " + key);

                o = convertAndPutToHeapCache(key, bytes);
            }
        }

        return o;
    }

    /**
     * Store a value to heap and flash memory.
     *
     * Note that the storage to RMS is done asynchronously in the background
     * which may lead to large binary objects being queued up on the Worker
     * thread. If you do this many times, you could run short on memory, and
     * should re-factor with use of synchronousPutToRMS() instead.
     *
     * Note that conversion to use form happens immediately and synchronously on
     * the calling thread before before this method returns. If conversion may
     * take a long time (XML parsing, etc) then consider not calling this from
     * the user event dispatch thread.
     *
     * @param key
     * @param bytes
     * @return the byte[] converted to use form by the cache's Handler
     */
    public synchronized Object put(final String key, final byte[] bytes) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Attempt to put trivial key to cache");
        }
        if (bytes == null) {
            throw new IllegalArgumentException("Attempt to put trivial bytes to cache: key=" + key);
        }
        rmsWriteWorkables.addElement(new Workable() {

            public boolean work() {
                try {
                    synchronousPutToRMS(key, bytes);
                } catch (Exception e) {
                    Log.l.log("Can not synch write to RMS", key, e);
                }

                return false;
            }
        });
        Worker.queuePriority(writeAllPending);

        return convertAndPutToHeapCache(key, bytes);
    }

    /**
     * Store the object to RMS, blocking the calling thread until the write is
     * complete.
     *
     * Generally you should use this method if you are on a Worker thread to
     * avoid adding large objects in the Worker queue waiting to be stored to
     * the RMS which could lead to a memory shortage. If you are on the EDT, use
     * the asynchronous put() method instead to avoid blocking the calling
     * thread.
     *
     * @param key
     * @param bytes
     * @param putToHeapCache - Set "true" unless an overriding method has
     * already done this
     * @return
     */
    private void synchronousPutToRMS(final String key, final byte[] bytes) {
        if (key == null) {
            throw new IllegalArgumentException("Null key put to cache");
        }
        try {
            do {
                try {
                    //#debug
                    Log.l.log("RMS cache write start", key + " (" + bytes.length + " bytes)");
                    RMSUtils.cacheWrite(key, bytes);
                    //#debug
                    Log.l.log("RMS cache write end", key + " (" + bytes.length + " bytes)");
                    break;
                } catch (RecordStoreFullException ex) {
                    //#debug
                    Log.l.log("Clearning space for data, ABORTING", key + " (" + bytes.length + " bytes)", ex);
                    if (!clearSpace(bytes.length)) {
                        //#debug
                        Log.l.log("Can not clear enough space for data, ABORTING", key);
                    }
                }
            } while (true);
        } catch (Exception e) {
            //#debug
            Log.l.log("Couldn't store object to RMS", key, e);
        }
    }

    /**
     * Remove unused and then currently used items from the RMS cache to make
     * room for new items.
     *
     * @param minSpaceToClear - in bytes
     * @return true if the requested amount of space has been cleared
     */
    private static boolean clearSpace(final int minSpaceToClear) {
        int spaceCleared = 0;
        final Vector rsv = RMSUtils.getCachedRecordStoreNames();

        //#debug
        Log.l.log("Clearing RMS space", minSpaceToClear + " bytes");

        // First: clear cached objects not currently appearing in any open cache
        for (int i = rsv.size() - 1; i >= 0; i--) {
            final String key = (String) rsv.elementAt(i);
            final StaticCache cache = getCacheContainingKey(key);

            if (cache != null) {
                spaceCleared += getByteSizeByKey(key);
                cache.remove(key);
            }
        }
        //#debug
        Log.l.log("End phase 1: clearing RMS space", spaceCleared + " bytes recovered");

        // Second: remove currently cached items, first from low priority caches
        while (spaceCleared < minSpaceToClear && rsv.size() > 0) {
            for (int i = 0; i < caches.size(); i++) {
                final StaticCache cache = (StaticCache) caches.elementAt(i);

                while (!cache.accessOrder.isEmpty() && spaceCleared < minSpaceToClear) {
                    final String key = (String) cache.accessOrder.removeLeastRecentlyUsed();
                    spaceCleared += getByteSizeByKey(key);
                    cache.remove(key);
                }
            }
        }
        //#debug
        Log.l.log("End phase 2: clearing RMS space", spaceCleared + " bytes recovered (total)");

        return spaceCleared >= minSpaceToClear;
    }

    private static int getByteSizeByKey(final String key) {
        int size = 0;

        try {
            final RecordStore rs = RMSUtils.getRecordStore(key, false);
            size = rs.getSize();
        } catch (RecordStoreException ex) {
            //#debug
            Log.l.log("Can not check size of record store to clear space", key, ex);
        }

        return size;
    }

    private static StaticCache getCacheContainingKey(String key) {
        StaticCache cache = null;

        synchronized (caches) {
            for (int i = 0; i < caches.size(); i++) {
                final StaticCache currentCache = (StaticCache) caches.elementAt(i);
                if (currentCache.containsKey(key)) {
                    cache = currentCache;
                    break;
                }
            }
        }

        return cache;
    }

    /**
     * Note that delete is synchronous, so while this operation does not take
     * long, other operations using the RMS may cause a slight stagger or pause
     * before this operation can complete.
     *
     * @param key
     */
    protected void remove(final String key) {
        try {
            if (containsKey(key)) {
                synchronized (StaticCache.this) {
                    accessOrder.removeElement(key);
                    cache.remove(key);
                }
                RMSUtils.cacheDelete(key);
                //#debug
                Log.l.log("Cache remove (from RAM and RMS)", key);
            }
        } catch (Exception e) {
            //#debug
            Log.l.log("Couldn't remove object from cache", key, e);
        }
    }

    /**
     * Remove all elements from this cache
     *
     */
    public void clear() {
        //#debug
        Log.l.log("Start Cache Clear", "ID=" + priority);
        final String[] keys;
        
        synchronized (this) {
            keys = new String[accessOrder.size()];
            accessOrder.copyInto(keys);
        }        
        for (int i = 0; i < keys.length; i++) {
            remove(keys[i]);
        }
        //#debug
        Log.l.log("Cache cleared", "ID=" + priority);
    }

    /**
     * Does this cache contain an object matching the key?
     *
     * @param key
     * @return
     */
    public synchronized boolean containsKey(final String key) {
        if (key != null) {
            return this.cache.containsKey(key);
        }

        return false;
    }

    /**
     * The number of items in the cache
     *
     * @return
     */
    public synchronized int getSize() {
        return this.cache.size();
    }

    /**
     * The relative priority used for allocating RMS space between multiple
     * caches. Higher priority caches synchronousRAMCacheGet more space.
     *
     * @return
     */
    public synchronized int getPriority() {
        return priority;
    }

    /**
     * Provide the handler which this caches uses for converting between
     * in-memory and binary formats
     *
     * @return
     */
    public DataTypeHandler getHandler() {
        return handler;
    }

    /**
     * For debugging use
     *
     * @return
     */
    public synchronized String toString() {
        String str = "StaticCache --- priority: " + priority + " size: " + getSize() + " size (bytes): " + sizeAsBytes + "\n";

        for (int i = 0; i < accessOrder.size(); i++) {
            str += accessOrder.elementAt(i) + "\n";
        }

        return str;
    }
}
