/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages the cache of file mappings.  These need to be owned by this object, as it keeps
 * the system of record of what is known.  If the underlying file path changes, it must
 * be updated via this class.  The primary concern of this class is to keep track
 * of the {@link P4ClientFileMapping} instances so that they can be correctly updated if
 * a client mapping changes.
 */
public class FileMappingRepo {
    private static final int CACHE_MISS_THRESHOLD = 100;

    private final boolean serverIsCaseInsensitive;

    private final Lock lock = new ReentrantLock();

    private final ReferenceQueue<P4ClientFileMapping> queue;
    private final Set<WeakReference<P4ClientFileMapping>> files = new HashSet<WeakReference<P4ClientFileMapping>>();
    private final Map<FilePath, WeakReference<P4ClientFileMapping>> filesByLocal = new HashMap<FilePath, WeakReference<P4ClientFileMapping>>();

    // Because of the case sensitivity of the server, access to this map should
    // go through the specialized functions below.
    private final Map<String, WeakReference<P4ClientFileMapping>> filesByDepot = new HashMap<String, WeakReference<P4ClientFileMapping>>();

    private int cacheMissCount = 0;

    public FileMappingRepo(final boolean serverIsCaseInsensitive) {
        this(serverIsCaseInsensitive, new ReferenceQueue<P4ClientFileMapping>());
    }

    FileMappingRepo(final boolean serverIsCaseInsensitive, @NotNull ReferenceQueue<P4ClientFileMapping> queue) {
        this.serverIsCaseInsensitive = serverIsCaseInsensitive;
        this.queue = queue;
    }

    @NotNull
    public Iterable<P4ClientFileMapping> getAllFiles() {
        lock.lock();
        try {
            return new WeakIterable(files);
        } finally {
            lock.unlock();
        }
    }

    /*
    @Nullable
    public P4ClientFileMapping getByDepot(@NotNull String depot) {
        // go directly through the filesByDepot, so we must explicitly get the internal
        // path name
        return cacheGet(filesByDepot, internalDepotPath(depot));

    }

    @Nullable
    public P4ClientFileMapping getByLocalFilePath(@NotNull FilePath path) {
        return cacheGet(filesByLocal, path);
    }
    */

    /**
     * Called when the callee cannot map the location to the depot.  If the
     * location is already registered, that registered version is returned.
     * It's possible that the depot mapping is already registered.
     * <p/>
     * Because the callee doesn't know about the depot, this is not considered
     * an "update" command.  Updates must only be done if both parts are known.
     *
     * @param location location on the current computer for the file.
     * @return the mapping
     */
    @NotNull
    public P4ClientFileMapping getByLocation(@NotNull FilePath location) {
        // Discover if the location is already registered.  This includes possible cache missing
        // computations.
        P4ClientFileMapping map = null;

        lock.lock();
        try {
            WeakReference<P4ClientFileMapping> ref = filesByLocal.get(location);
            if (ref != null) {
                map = ref.get();
                if (map == null) {
                    // cache miss logic and cleanup
                    filesByLocal.remove(location);
                    onCacheMiss();
                }
                // else: the mapping still exists for this location.
                // Because we don't know the new state of the depot path,
                // we can ignore any possible updates that it requires.
            }
            if (map == null) {
                map = new P4ClientFileMapping(null, location);
                ref = createRef(map);
                files.add(ref);
                filesByLocal.put(location, ref);
                // no depot associated with this mapping.
            }
        } finally {
            lock.unlock();
        }
        return map;
    }

    /**
     * Create or update an existing depot object with a new location.  If the
     * new location is {@code null}, then the original location is used.
     *
     * @param depot the known depot location of the file.
     * @param location the local computer location where the file is located;
     *                 pass {@code null} if it isn't known.
     * @return the file mapping for the depot location; it might be created by this
     *   call.
     */
    @NotNull
    public P4ClientFileMapping getByDepotLocation(@NotNull String depot, @Nullable FilePath location) {
        // Discover if the location is already registered.  This includes possible cache missing
        // computations.
        P4ClientFileMapping map = null;

        lock.lock();
        try {
            final String internalDepotPath = internalDepotPath(depot);
            WeakReference<P4ClientFileMapping> ref = filesByDepot.get(internalDepotPath);
            if (ref == null && location != null) {
                ref = filesByLocal.get(location);
            }
            if (ref != null) {
                map = ref.get();
                if (map == null) {
                    // cache miss logic and cleanup
                    filesByDepot.remove(internalDepotPath);
                    filesByLocal.remove(location);
                    onCacheMiss();
                } else if (location != null && !location.equals(map.getLocalFilePath())) {
                    // This is a location update.
                    filesByLocal.remove(map.getLocalFilePath());
                    filesByLocal.put(location, ref);
                    map.updateLocalPath(location);
                } else if (!depot.equals(map.getDepotPath())) {
                    // This is a depot update
                    // depot is always not-null
                    if (map.getDepotPath() != null) {
                        filesByDepot.remove(internalDepotPath(map.getDepotPath()));
                    }
                    filesByDepot.put(depot, ref);
                    map.updateDepot(depot);
                }
                // else, either the location is not known by the callee (it might be known
                // by the cached object), or both the callee and the cache version have
                // the same location object and depot location.  Either way, there's no need to
                // touch the map's location or the lookups.
            }
            if (map == null) {
                if (location != null) {
                    map = new P4ClientFileMapping(depot, location);
                    ref = createRef(map);
                    filesByLocal.put(location, ref);
                } else {
                    map = new P4ClientFileMapping(depot);
                    ref = createRef(map);
                    // no local assignment
                }
                filesByDepot.put(internalDepotPath, ref);
                files.add(ref);
            }
        } finally {
            lock.unlock();
        }
        return map;
    }

    /**
     *
     * @param depotToLocation new mappings.  This does not strip out the existing mappings
     *               that aren't in the passed-in list.
     * @deprecated because it's not implemented yet
     */
    public void updateLocations(@NotNull Map<String, FilePath> depotToLocation) {
        lock.lock();
        try {
            // Because we're doing a potentially large operation, clean up the
            // existing maps for old stuff.
            flush();

            // FIXME implement or delete this method if not used
            throw new IllegalStateException("not implemented");
        } finally {
            lock.unlock();
        }
    }


    /**
     * Replace the entire collection of file mappings with these new objects.
     *
     * @param mappings new, fully configured mappings
     */
    public void refreshFiles(@NotNull Collection<P4ClientFileMapping> mappings) {
        lock.lock();
        try {
            files.clear();
            filesByDepot.clear();
            filesByLocal.clear();
            // this implicitly did a flush, so the cache misses can return to 0.
            cacheMissCount = 0;

            for (P4ClientFileMapping mapping : mappings) {
                addMapping(mapping);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Called when the workspace view is changed, which invalidates all existing
     * depot mappings.
     */
    public void clearLocations() {
        lock.lock();
        try {
            filesByLocal.clear();
            Iterator<WeakReference<P4ClientFileMapping>> iter = files.iterator();
            while (iter.hasNext()) {
                WeakReference<P4ClientFileMapping> ref = iter.next();
                final P4ClientFileMapping map = ref.get();
                if (map == null) {
                    iter.remove();
                    // delay the cache miss check until the end.
                    cacheMissCount++;
                } else {
                    map.updateLocalPath(null);
                }
            }
            if (cacheMissCount > CACHE_MISS_THRESHOLD) {
                flush();
            }
        } finally {
            lock.unlock();
        }
    }

    // Must be run from within a write lock.
    private void addMapping(@NotNull final P4ClientFileMapping mapping) {
        final WeakReference<P4ClientFileMapping> originalRef = filesByLocal.remove(mapping.getLocalFilePath());
        if (originalRef != null) {
            final P4ClientFileMapping original = originalRef.get();
            if (original != null) {
                removeByDepot(original.getDepotPath());
                filesByLocal.remove(original.getLocalFilePath());
                files.remove(originalRef);
            }
        }
        final WeakReference<P4ClientFileMapping> mapRef = createRef(mapping);
        files.add(mapRef);
        filesByLocal.put(mapping.getLocalFilePath(), mapRef);
        addByDepot(mapping.getDepotPath(), mapRef);
    }


    // Must be run from within a write lock.
    private void flush() {
        Set<Reference<? extends P4ClientFileMapping>> refs = new HashSet<Reference<? extends P4ClientFileMapping>>();
        Reference<? extends P4ClientFileMapping> ref;
        while ((ref = queue.poll()) != null) {
            refs.add(ref);
            // The type is what was passed into the queue; so it may
            // not be a correct object, but it should be.
            //noinspection SuspiciousMethodCalls
            files.remove(ref);
        }


        {
            // Careful not to compare by key (depot path) here...
            Iterator<Entry<String, WeakReference<P4ClientFileMapping>>> iter =
                    filesByDepot.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<String, WeakReference<P4ClientFileMapping>> next = iter.next();
                if (refs.contains(next.getValue())) {
                    iter.remove();
                }
            }
        }
        {
            final Iterator<Entry<FilePath, WeakReference<P4ClientFileMapping>>> iter =
                    filesByLocal.entrySet().iterator();
            while (iter.hasNext()) {
                final Entry<FilePath, WeakReference<P4ClientFileMapping>> next = iter.next();
                if (refs.contains(next.getValue())) {
                    iter.remove();
                }
            }
        }

        cacheMissCount = 0;
    }


    @NotNull
    private String internalDepotPath(@NotNull String depot) {
        if (serverIsCaseInsensitive) {
            depot = depot.toLowerCase();
        }
        return depot;
    }


    private WeakReference<P4ClientFileMapping> getWeakRefByDepot(@NotNull String depot) {
        return filesByDepot.get(internalDepotPath(depot));
    }

    // Must be run from within a write lock.
    private void addByDepot(@Nullable String depot, @NotNull WeakReference<P4ClientFileMapping> map) {
        if (depot != null) {
            filesByDepot.put(internalDepotPath(depot), map);
        }
    }

    // Must be run from within a write lock.
    private void removeByDepot(@Nullable String depot) {
        if (depot != null) {
            filesByDepot.remove(internalDepotPath(depot));
        }
    }

    // Must be run from within a write lock.
    @Nullable
    private <T> P4ClientFileMapping cacheGet(@NotNull Map<T, WeakReference<P4ClientFileMapping>> mapper, @NotNull T key) {
        final WeakReference<P4ClientFileMapping> ref = mapper.get(key);
        if (ref != null) {
            final P4ClientFileMapping val = ref.get();
            if (val != null) {
                return val;
            }

            // Clean up our map; there's no more references to this thing.
            mapper.remove(key);

            onCacheMiss();
        }
        return null;
    }

    // Check if we need to flush our cache
    private void onCacheMiss() {
        if (cacheMissCount++ > CACHE_MISS_THRESHOLD) {
            cacheMissCount = 0;
            flush();
        }
    }

    @NotNull
    WeakReference<P4ClientFileMapping> createRef(@NotNull P4ClientFileMapping map) {
        return new WeakReference<P4ClientFileMapping>(map, queue);
    }


    static class WeakIterable implements Iterable<P4ClientFileMapping> {
        private final Iterable<WeakReference<P4ClientFileMapping>> proxy;

        WeakIterable(final Collection<WeakReference<P4ClientFileMapping>> proxy) {
            this.proxy = new ArrayList<WeakReference<P4ClientFileMapping>>(proxy);
        }

        @Override
        public Iterator<P4ClientFileMapping> iterator() {
            return new WeakIterator(proxy.iterator());
        }

    }

    static class WeakIterator implements Iterator<P4ClientFileMapping> {
        private final Iterator<WeakReference<P4ClientFileMapping>> iterator;
        private P4ClientFileMapping next;

        public WeakIterator(@NotNull final Iterator<WeakReference<P4ClientFileMapping>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            next = null;
            while (next == null && iterator.hasNext()) {
                final WeakReference<P4ClientFileMapping> ref = iterator.next();
                next = ref.get();
            }
            return next != null;
        }

        @Override
        public P4ClientFileMapping next() {
            if (next == null) {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
            }
            final P4ClientFileMapping ret = next;
            next = null;
            return ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
