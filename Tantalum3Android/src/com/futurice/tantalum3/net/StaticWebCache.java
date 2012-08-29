/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.futurice.tantalum3.net;

import com.futurice.tantalum3.Result;
import com.futurice.tantalum3.Workable;
import com.futurice.tantalum3.Worker;
import com.futurice.tantalum3.log.Log;
import com.futurice.tantalum3.rms.DataTypeHandler;
import com.futurice.tantalum3.rms.StaticCache;

/**
 * A cache of remote http contents backed by local flash memory storage
 *
 * @author tsaa
 */
public class StaticWebCache extends StaticCache {

    private static final int HTTP_GET_RETRIES = 3;

    public StaticWebCache(final char priority, final DataTypeHandler handler) {
        super(priority, handler);
    }

    /**
     * Retrieve the object from 1. RAM if available 2. RMS if available 3. WEB
     *
     * @param url
     * @param result
     */
    @Override
    public void get(final String url, final Result r, final int priority) {
        super.get(url, new Result() {

            /**
             * Local Cache get returned a result, no need to get it from the
             * network
             *
             */
            @Override
            public void set(final Object o) {
                if (r != null) {
                    r.set(o);
                }
            }

            /**
             * Local Cache get failed to return a result- not cached
             *
             */
            @Override
            public void onCancel() {
                Log.l.log("No result from cache get, shift to HTTP", url);
                final HttpGetter httpGetter = new HttpGetter(url, HTTP_GET_RETRIES, new Result() {

                    @Override
                    public void set(Object o) {
                        try {
                            o = put(url, (byte[]) o); // Convert to use form
                            if (r != null) {
                                if (o != null) {
                                    r.set(o);
                                    //#debug
                                    Log.l.log("END SAVE: After no result from cache get, shift to HTTP", url);
                                } else {
                                    r.cancel(false);
                                }
                            }
                        } catch (Exception e) {
                            Log.l.log("Can not set result", url, e);
                            cancel(false);
                        }
                    }

                    @Override
                    public void onCancel() {
                        if (r != null) {
                            r.cancel(false);
                        }
                    }
                });

                // Continue the HTTP GET attempt immediately on the same Worker thread
                // This avoids possible forkSerial delays
                //httpGetter.compute();
                
                Worker.fork(httpGetter);
            }
        }, priority);
        
    }

    /**
     * Delete any existing version, then retrieve the object from WEB.
     *
     * @param url
     * @param result
     */
    public void update(final String url, final Result result) {
        Worker.fork(new Workable() {

            @Override
            public Object compute() {
                try {
                    remove(url);
                    get(url, result, Worker.HIGH_PRIORITY);
                } catch (Exception e) {
                    Log.l.log("Can not update", url, e);
                }

                return null;
            }
        });
    }

    /**
     * Retrieve the object from WEB if it is not already cached locally.
     *
     * @param url
     * @param updateResult
     */
    public void prefetch(final String url) {
        if (synchronousRAMCacheGet(url) == null) {
            Worker.fork(new Workable() {

                @Override
                public Object compute() {
                    try {
                        get(url, null, Worker.LOW_PRIORITY);
                    } catch (Exception e) {
                        Log.l.log("Can not prefetch", url, e);
                    }

                    return null;
                }
            }, Worker.LOW_PRIORITY);
        }
    }
}