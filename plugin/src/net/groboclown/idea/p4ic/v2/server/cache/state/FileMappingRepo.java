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

/**
 * Manages the cache of file mappings.  These need to be owned by this object, as it keeps
 * the system of record of what is known.  If the underlying file path changes, it must
 * be updated via this class.  The primary concern of this class is to keep track
 * of the {@link P4ClientFileMapping} instances so that they can be correctly updated if
 * a client mapping changes.
 * <p/>
 * This class is not thread safe.  Need to investigate whether this needs to be
 * thread safe (probably will be).
 */
public class FileMappingRepo {
    private static final int CACHE_MISS_THRESHOLD = 100;

    private final boolean serverIsCaseInsensitive;

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
        return new WeakIterable(files);
    }

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


    public void addLocation(@NotNull String depot, @NotNull FilePath location) {
        // Discover if the location is already registered.  This includes possible cache missing
        // computations.
        final String internalDepotPath = internalDepotPath(depot);
        final WeakReference<P4ClientFileMapping> ref = filesByDepot.get(internalDepotPath);
        P4ClientFileMapping map = null;
        if (ref != null) {
            map = ref.get();
            if (map == null) {
                // cache miss logic and cleanup
                filesByDepot.remove(internalDepotPath);
                onCacheMiss();
            }
        }
        if (map == null) {
            map = new P4ClientFileMapping(depot, location.getIOFile().getAbsolutePath());
        }
        updateLocation(map, location);
    }

    public void updateLocation(@NotNull P4ClientFileMapping mapping, @Nullable FilePath newLocation) {
        final FilePath oldLocation = mapping.getLocalFilePath();
        {
            String path = null;
            if (newLocation != null) {
                path = newLocation.getIOFile().getAbsolutePath();
            }
            mapping.updateLocalPath(path);
        }

        WeakReference<P4ClientFileMapping> ref = getWeakRefByDepot(mapping.getDepotPath());
        if (newLocation != null && ! newLocation.equals(mapping.getLocalFilePath())) {
            // Update to the file location
            if (ref == null) {
                // Add a new file location
                ref = createRef(mapping);
                files.add(ref);
                addByDepot(mapping.getDepotPath(), ref);
            }
            filesByLocal.remove(oldLocation);
            filesByLocal.put(newLocation, ref);
        } else if (newLocation == null) {
            // remove the file location; note that we want to keep the depot
            // reference around, because it's potentially useful.
            filesByLocal.remove(oldLocation);
        } else if (ref == null) {
            // brand new location
            ref = createRef(mapping);
            files.add(ref);
            addByDepot(mapping.getDepotPath(), ref);
            filesByLocal.put(newLocation, ref);
        } // else it's an existing location that's the same location
    }


    public void updateLocations(@NotNull Map<P4ClientFileMapping, FilePath> newMap) {
        // Because we're doing a potentially large operation, clean up the
        // existing maps for old stuff.  Do this before the new updates,
        // because
        flush();

        // FIXME
    }


    /**
     * Replace the entire collection of file mappings with these new objects.
     *
     * @param mappings new, fully configured mappings
     */
    void refreshFiles(@NotNull Collection<P4ClientFileMapping> mappings) {
        files.clear();
        filesByDepot.clear();
        filesByLocal.clear();
        for (P4ClientFileMapping mapping : mappings) {
            addMapping(mapping);
        }
    }

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


    private void flush() {
        Set<Reference<? extends P4ClientFileMapping>> refs = new HashSet<Reference<? extends P4ClientFileMapping>>();
        Reference<? extends P4ClientFileMapping> ref;
        while ((ref = queue.poll()) != null) {
            refs.add(ref);
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

    private void addByDepot(@NotNull String depot, @NotNull WeakReference<P4ClientFileMapping> map) {
        filesByDepot.put(internalDepotPath(depot), map);
    }

    private void removeByDepot(@NotNull String depot) {
        filesByDepot.remove(internalDepotPath(depot));
    }

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

        WeakIterable(final Iterable<WeakReference<P4ClientFileMapping>> proxy) {
            this.proxy = proxy;
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
