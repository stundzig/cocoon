/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.components.store.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;

import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.util.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.excalibur.store.Store;
import org.apache.excalibur.store.StoreJanitor;

/**
 * Store implementation based on EHCache.
 * (http://ehcache.sourceforge.net/)
 * Configure the store. 
 * <br/>
 * The following options can be used:
 * <ul>
 *  <li><code>maxobjects</code> (10000) - The maximum number of in-memory objects.</li>
 *  <li><code>overflow-to-disk</code> (true) - Whether to spool elements to disk after
 *   maxobjects has been exceeded.</li>
 * <li><code>eternal</code> (true) - whether or not entries expire. When set to
 * <code>false</code> the <code>timeToLiveSeconds</code> and
 * <code>timeToIdleSeconds</code> parameters are used to determine when an
 * item expires.</li>
 * <li><code>timeToLiveSeconds</code> (0) - how long an entry may live in the cache
 * before it is removed. The entry will be removed no matter how frequently it is retrieved.</li>
 * <li><code>timeToIdleSeconds</code> (0) - the maximum time between retrievals
 * of an entry. If the entry is not retrieved for this period, it is removed from the
 * cache.</li>
 *  <li><code>use-cache-directory</code> (false) - If true the <i>cache-directory</i>
 *   context entry will be used as the location of the disk store. 
 *   Within the servlet environment this is set in web.xml.</li>
 *  <li><code>use-work-directory</code> (false) - If true the <i>work-directory</i>
 *   context entry will be used as the location of the disk store.
 *   Within the servlet environment this is set in web.xml.</li>
 *  <li><code>directory</code> - Specify an alternative location of the disk store.
 * </ul>
 * 
 * <p>
 * Setting <code>eternal</code> to <code>false</code> but not setting
 * <code>timeToLiveSeconds</code> and/or <code>timeToIdleSeconds</code>, has the
 * same effect as setting <code>eternal</code> to <code>true</code>.
 * </p>
 * 
 * <p>
 * Here is an example to clarify the purpose of the <code>timeToLiveSeconds</code> and
 * <code>timeToIdleSeconds</code> parameters:
 * </p>
 * <ul>
 *   <li>timeToLiveSeconds = 86400 (1 day)</li>
 *   <li>timeToIdleSeconds = 10800 (3 hours)</li>
 * </ul>
 * 
 * <p>
 * With these settings the entry will be removed from the cache after 24 hours. If within
 * that 24-hour period the entry is not retrieved within 3 hours after the last retrieval, it will
 * also be removed from the cache.
 * </p>
 * 
 * <p>
 * By setting <code>timeToLiveSeconds</code> to <code>0</code>, an item can stay in
 * the cache as long as it is retrieved within <code>timeToIdleSeconds</code> after the
 * last retrieval.
 * </p>
 *
 * <p>
 * By setting <code>timeToIdleSeconds</code> to <code>0</code>, an item will stay in
 * the cache for exactly <code>timeToLiveSeconds</code>.
 * </p>
 *
 * <p>
 * <code>disk-persistent</code> Whether the disk store persists between restarts of
 * the Virtual Machine. The default value is true.
 *
 * @version $Id$
 */
public class EHDefaultStore implements Store {

    // ---------------------------------------------------- Constants

    private static final int MAXOBJECTS = 10000;
    private static final boolean OVERFLOW_TO_DISK = true;
    private static final boolean DISK_PERSISTENT = true;
    private static final boolean ETERNAL = true;
    private static final long TIME_TO_LIVE_SECONDS = 0L;
    private static final long TIME_TO_IDLE_SECONDS = 0L;
    private static final boolean USE_WORK_DIRECTORY = false;
    private static final boolean USE_CACHE_DIRECTORY = false;

    private static final String CONFIG_FILE = "org/apache/cocoon/components/store/impl/ehcache.xml";

    private static int instanceCount = 0;

    // ---------------------------------------------------- Instance variables

    private Cache cache;
    private CacheManager cacheManager;

    private final String cacheName;

    // configuration options
    private int maxObjects = MAXOBJECTS;
    private boolean overflowToDisk = OVERFLOW_TO_DISK;
    private boolean diskPersistent = DISK_PERSISTENT;
    private boolean eternal = ETERNAL;
    private long timeToLiveSeconds = TIME_TO_LIVE_SECONDS;
    private long timeToIdleSeconds = TIME_TO_IDLE_SECONDS;
    private boolean useCacheDirectory = USE_CACHE_DIRECTORY;
    private boolean useWorkDirectory = USE_WORK_DIRECTORY;
    private String directory;
    /** The service manager */

    /** By default we use the logger for this class. */
    private Log logger = LogFactory.getLog(getClass());

    /** The store janitor */
    private StoreJanitor storeJanitor;
    
    /** The settings object */
    private Settings settings;

    private File workDir;
    private File cacheDir;

    // ---------------------------------------------------- Lifecycle

    public EHDefaultStore() {
        instanceCount++;
        this.cacheName = "cocoon-ehcache-" + instanceCount;
    }

    /**
     *  <li><code>directory</code> - Specify an alternative location of the disk store.
     * @param directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * <code>disk-persistent</code> Whether the disk store persists between restarts of
     * the Virtual Machine. The default value is true.
     * @param diskPersistent
     */
    public void setDiskPersistent(boolean diskPersistent) {
        this.diskPersistent = diskPersistent;
    }

    /**
     * <li><code>eternal</code> (true) - whether or not entries expire. When set to
     * <code>false</code> the <code>timeToLiveSeconds</code> and
     * <code>timeToIdleSeconds</code> parameters are used to determine when an
     * item expires.</li>
     * @param eternal
     */
    public void setEternal(boolean eternal) {
        this.eternal = eternal;
    }

    /**
     * <code>maxobjects</code> (10000) - The maximum number of in-memory objects.
     * @param maxObjects
     */
    public void setMaxObjects(int maxObjects) {
        this.maxObjects = maxObjects;
    }

    /**
     *  <li><code>overflow-to-disk</code> (true) - Whether to spool elements to disk after
     *   maxobjects has been exceeded.</li>
     * @param overflowToDisk
     */
    public void setOverflowToDisk(boolean overflowToDisk) {
        this.overflowToDisk = overflowToDisk;
    }

    /**
     * <li><code>timeToIdleSeconds</code> (0) - the maximum time between retrievals
     * of an entry. If the entry is not retrieved for this period, it is removed from the
     * cache.</li>
     * @param timeToIdleSeconds
     */
    public void setTimeToIdleSeconds(long timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    /**
     * <li><code>timeToLiveSeconds</code> (0) - how long an entry may live in the cache
     * before it is removed. The entry will be removed no matter how frequently it is retrieved.</li>
     * @param timeToLiveSeconds
     */
    public void setTimeToLiveSeconds(long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    /**
     *  <li><code>use-cache-directory</code> (false) - If true the <i>cache-directory</i>
     *   context entry will be used as the location of the disk store. 
     *   Within the servlet environment this is set in web.xml.</li>
     * @param useCacheDirectory
     */
    public void setUseCacheDirectory(boolean useCacheDirectory) {
        this.useCacheDirectory = useCacheDirectory;
    }

    /**
     *  <li><code>use-work-directory</code> (false) - If true the <i>work-directory</i>
     *   context entry will be used as the location of the disk store.
     *   Within the servlet environment this is set in web.xml.</li>
     * @param useWorkDirectory
     */
    public void setUseWorkDirectory(boolean useWorkDirectory) {
        this.useWorkDirectory = useWorkDirectory;
    }

    public Log getLogger() {
        return this.logger;
    }

    public void setLogger(Log l) {
        this.logger = l;
    }

    /**
     * @param settings the settings to set
     */
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * @param storeJanitor the storeJanitor to set
     */
    public void setStoreJanitor(StoreJanitor storeJanitor) {
        this.storeJanitor = storeJanitor;
    }

    /**
     * Sets the cache directory
     */
    private void setDirectory(final File directory) throws IOException  {
        // Save directory path prefix
        String directoryPath = getFullFilename(directory);
        directoryPath += File.separator;

        // If directory doesn't exist, create it anew
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Error creating store directory '" + directoryPath + "': ");
            }
        }

        // Is given file actually a directory?
        if (!directory.isDirectory()) {
            throw new IOException("'" + directoryPath + "' is not a directory");
        }

        // Is directory readable and writable?
        if (!(directory.canRead() && directory.canWrite())) {
            throw new IOException("Directory '" + directoryPath + "' is not readable/writable");
        }
        System.setProperty("java.io.tmpdir", directoryPath);
    }

    /**
     * Get the complete filename corresponding to a (typically relative)
     * <code>File</code>.
     * This method accounts for the possibility of an error in getting
     * the filename's <i>canonical</i> path, returning the io/error-safe
     * <i>absolute</i> form instead
     *
     * @param file The file
     * @return The file's absolute filename
     */
    private static String getFullFilename(File file) {
        try {
            return file.getCanonicalPath();
        }
        catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

    /**
     * Initialize the CacheManager and created the Cache.
     */
    public void init() throws Exception {
        this.workDir = new File(settings.getWorkDirectory());
        this.cacheDir = new File(settings.getCacheDirectory());

        try {
            if (this.useCacheDirectory) {
                if (this.getLogger().isDebugEnabled()) {
                    getLogger().debug("Using cache directory: " + cacheDir);
                }
                setDirectory(cacheDir);
            } else if (this.useWorkDirectory) {
                if (this.getLogger().isDebugEnabled()) {
                    getLogger().debug("Using work directory: " + workDir);
                }
                setDirectory(workDir);
            } else if (this.directory != null) {
                this.directory = IOUtils.getContextFilePath(workDir.getPath(), this.directory);
                if (this.getLogger().isDebugEnabled()) {
                    getLogger().debug("Using directory: " + this.directory);
                }
                setDirectory(new File(this.directory));
            } else {
                try {
                    // Legacy: use working directory by default
                    setDirectory(this.workDir);
                } catch (IOException e) {
                    // EMPTY
                }
            }
        } catch (IOException e) {
            throw new Exception("Unable to set directory", e);
        }

        URL configFileURL = Thread.currentThread().getContextClassLoader().getResource(CONFIG_FILE);
        this.cacheManager = CacheManager.create(configFileURL);
        this.cache = new Cache(this.cacheName, this.maxObjects, this.overflowToDisk, this.eternal,
                this.timeToLiveSeconds, this.timeToIdleSeconds, this.diskPersistent, 120);
        this.cacheManager.addCache(this.cache);
        this.storeJanitor.register(this);
        getLogger().info("EHCache cache \"" + this.cacheName + "\" initialized");
    }

    /**
     * Shutdown the CacheManager.
     */
    public void destroy() {
        if (this.storeJanitor != null)
            this.storeJanitor.unregister(this);

        /*
         * EHCache can be a bitch when shutting down. Basically every cache registers
         * a hook in the Runtime for every persistent cache, that will be executed when
         * the JVM exit. It might happen (though) that we are shutting down Cocoon
         * because of the same event (someone sending a TERM signal to the VM).
         * So what we need to do here is to check if the cache itself is still alive,
         * then we're going to shutdown EHCache entirely (if there are other caches open
         * they will be shut down as well), if the cache is not alive, either another
         * instance of this called the shutdown method on the CacheManager (thanks) or
         * otherwise the hook had time to run before we got here.
         */
        synchronized (this.cache) {
            if (Status.STATUS_ALIVE == this.cache.getStatus()) {
                try {
                    getLogger().info("Disposing EHCache cache \"" + this.cacheName + "\".");
                    this.cacheManager.shutdown();
                } catch (IllegalStateException e) {
                    getLogger().error("Error disposing EHCache cache \"" + this.cacheName + "\".", e);
                }
            } else {
                getLogger().info("EHCache cache \"" + this.cacheName + "\" already disposed.");
            }
        }
    }

    // ---------------------------------------------------- Store implementation

    /**
     * @see org.apache.excalibur.store.Store#free()
     */
    public Object get(Object key) {
        Object value = null;
        try {
            final Element element = this.cache.get((Serializable) key);
            if (element != null) {
                value = element.getValue();
            }
        } catch (CacheException e) {
            getLogger().error("Failure retrieving object from store", e);
        }
        if (getLogger().isDebugEnabled()) {
            if (value != null) {
                getLogger().debug("Found key: " + key);
            } else {
                getLogger().debug("NOT Found key: " + key);
            }
        }
        return value;
    }

    /**
     * @see org.apache.excalibur.store.Store#free()
     */
    public void store(Object key, Object value) throws IOException {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Store object " + value + " with key "+ key);
        }

        // without these checks we get cryptic "ClassCastException" messages
        if (!(key instanceof Serializable)) {
            throw new IOException("Key of class " + key.getClass().getName() + " is not Serializable");
        }
        if (!(value instanceof Serializable)) {
            throw new IOException("Value of class " + value.getClass().getName() + " is not Serializable");            
        }

        final Element element = new Element((Serializable) key, (Serializable) value);
        this.cache.put(element);
    }

    /**
     * @see org.apache.excalibur.store.Store#free()
     */
    public void free() {
        try {
            final List keys = this.cache.getKeysNoDuplicateCheck();
            if (!keys.isEmpty()) {
            	// TODO find a way to get to the LRU one.
                final Serializable key = (Serializable) keys.get(0);
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Freeing cache");
                    getLogger().debug("key: " + key);
                    getLogger().debug("value: " + this.cache.get(key));
                }
                if (!this.cache.remove(key)) {
                    if (getLogger().isInfoEnabled()) {
                        getLogger().info("Concurrency condition in free()");
                    }
                }
            }
        } catch (CacheException e) {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Error in free()", e);
            }
        }
    }

    /**
     * @see org.apache.excalibur.store.Store#remove(java.lang.Object)
     */
    public void remove(Object key) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Removing item " + key);
        }
        this.cache.remove((Serializable) key);
    }

    /**
     * @see org.apache.excalibur.store.Store#clear()
     */
    public void clear() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Clearing the store");
        }
        try {
            this.cache.removeAll();
        } catch (IOException e) {
            getLogger().error("Failure to clearing store", e);
        }
    }

    /**
     * @see org.apache.excalibur.store.Store#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        try {
            return this.cache.get((Serializable) key) != null;
        } catch (CacheException e) {
            getLogger().error("Failure retrieving object from store",e);
        }
        return false;
    }

    /**
     * @see org.apache.excalibur.store.Store#keys()
     */
    public Enumeration keys() {
        List keys = null;
        try {
            keys = this.cache.getKeys();
        } catch (CacheException e) {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Error while getting cache keys", e);
            }
            keys = Collections.EMPTY_LIST;
        }
        return Collections.enumeration(keys);
    }

    /**
     * @see org.apache.excalibur.store.Store#size()
     */
    public int size() {
        try {
            // cast to int due ehcache implementation returns a long instead of int.
            // See: http://ehcache.sourceforge.net/javadoc/net/sf/ehcache/Cache.html#getMemoryStoreSize()
            return (int)this.cache.getMemoryStoreSize();
        } catch (IllegalStateException e) {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Error while getting cache size", e);
            }
            return 0;
        }
    }
}
