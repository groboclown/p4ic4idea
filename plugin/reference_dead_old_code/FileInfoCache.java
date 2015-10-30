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

package net.groboclown.idea.p4ic.server;


import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches the Perforce information of a file, in order to
 * reduce the number of redundant server calls.
 * <p>
 * The {@link #get} method
 * is used only by the {@link P4FileInfo} factory methods, while the
 * {@link #invalidateCache()} is used by the client when necessary.
 * </p>
 * <p>
 * There should be only one instance of this per client.
 * </p>
 */
public class FileInfoCache {
    private final Object sync = new Object();
    private final Map<String, P4FileInfo> depotCache;


    public interface Loader {
        @NotNull
        P4FileInfo create(@NotNull String path, @NotNull IFileSpec spec);
    }


    public FileInfoCache() {
        depotCache = new HashMap<String, P4FileInfo>();
    }

    public void invalidateCache() {
        synchronized (sync) {
            depotCache.clear();
        }
    }


    /**
     * The path names *should* be escaped here.
     *
     * @param primaryPath the Perforce path as determined by the {@link P4FileInfo} class.
     * @return the cached version of the {@link P4FileInfo} object.
     */
    @NotNull
    public P4FileInfo get(@NotNull String primaryPath, @NotNull IFileSpec spec, @NotNull Loader loader) {
        boolean isExtended = spec instanceof IExtendedFileSpec;

        List<String> specPaths = getPathsForSpec(primaryPath, spec);
        synchronized (sync) {
            for (String path: specPaths) {
                P4FileInfo fileInfo = depotCache.get(path);
                if (fileInfo != null) {
                    // If we are given an extended spec, then we want to use
                    // that.  If the cached version is not extended, then
                    // it will be replaced.  If they are both extended, then
                    // we'll replace the cache with the up-to-date version.
                    if (! isExtended && fileInfo.getExtendedSpec() != null) {
                        return fileInfo;
                    }
                }
            }

            // Instead of reloading the cached object, we'll waste some memory
            // but keep our cache up-to-date.

            P4FileInfo fileInfo = loader.create(primaryPath, spec);
            for (String path: specPaths) {
                depotCache.put(path, fileInfo);
            }
            return fileInfo;
        }
    }


    /**
     * The primary reason for the cache - rather than resubmitting another
     * request to the server to gather information about the file, load what
     * may have already been loaded about it.
     *
     * @param spec Perforce specification object
     * @return the P4FileInfo cached for this spec.
     */
    @Nullable
    public P4FileInfo get(@NotNull IFileSpec spec) {
        List<String> specPaths = getPathsForSpec(null, spec);
        synchronized (sync) {
            for (String path: specPaths) {
                P4FileInfo fileInfo = depotCache.get(path);
                if (fileInfo != null) {
                    return fileInfo;
                }
            }
        }
        return null;
    }


    @NotNull
    private List<String> getPathsForSpec(@Nullable final String primaryPath, @NotNull final IFileSpec spec) {
        List<String> ret = new ArrayList<String>();
        if (primaryPath != null) {
            ret.add(stripPath(primaryPath));
        }
        String path;
        path = spec.getDepotPathString();
        if (path != null) {
            ret.add(stripPath(path));
        }
        path = spec.getLocalPathString();
        if (path != null) {
            ret.add(stripPath(path));
        }
        path = spec.getClientPathString();
        if (path != null) {
            ret.add(stripPath(path));
        }
        path = spec.getOriginalPathString();
        if (path != null) {
            ret.add(stripPath(path));
        }
        path = spec.getPreferredPathString();
        if (path != null) {
            ret.add(stripPath(path));
        }
        return ret;
    }


    private String stripPath(@NotNull String path) {
        int hash = path.indexOf('#');
        int at = path.indexOf('@');
        if (hash >= 0 && (hash < at || at < 0)) {
            return path.substring(0, hash);
        }
        if (at >= 0 && (at < hash || hash < 0)) {
            return path.substring(0, at);
        }
        return path;
    }
}
