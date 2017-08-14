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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class FileUpdateStateList implements Iterable<P4FileUpdateState> {
    private static final Logger LOG = Logger.getInstance(FileUpdateStateList.class);

    // The file update state can change its contents, and thus be changing its equality and its hash code.
    // When its hash code changes, and it already belongs to the set, then it can no longer be removed
    // through a simple "remove" call (the hash codes no longer match).

    // Therefore, we only save a map of the local file system file to the update state.
    // We know we have a local file system in the state, because this stores a local update,
    // which can only happen with a file.

    private final Set<P4FileUpdateState> allFiles;
    private final Map<File, P4FileUpdateState> updatedFiles;
    private final Map<String, P4FileUpdateState> shelvedUpdatedFiles;
    private final Object sync = new Object();

    FileUpdateStateList() {
        this.allFiles = new HashSet<P4FileUpdateState>();
        this.updatedFiles = new HashMap<File, P4FileUpdateState>();
        this.shelvedUpdatedFiles = new HashMap<String, P4FileUpdateState>();
    }

    @NotNull
    @Override
    public Iterator<P4FileUpdateState> iterator() {
        return copy().iterator();
    }


    /**
     * Empty out the cached state.
     */
    void flush() {
        synchronized (sync) {
            allFiles.clear();
            updatedFiles.clear();
            shelvedUpdatedFiles.clear();
        }
    }


    @NotNull
    public Set<P4FileUpdateState> copy() {
        synchronized (sync) {
            return new HashSet<P4FileUpdateState>(allFiles);
        }
    }


    public void replaceWith(@NotNull Collection<P4FileUpdateState> newValues) {
        synchronized (sync) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Replacing update state files with " + newValues + "; was " + updatedFiles);
            }
            flush();
            for (P4FileUpdateState newValue : newValues) {
                allFiles.add(newValue);

                updatedFiles.put(getKey(newValue), newValue);
            }
        }
    }


    public void add(@NotNull P4FileUpdateState state) {
        final File key = getKey(state);
        synchronized (sync) {
            updatedFiles.put(key, state);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding state file with " + state + "; now " + updatedFiles);
            }
        }
    }


    public boolean remove(@NotNull P4FileUpdateState state) {
        synchronized (sync) {
            final boolean ret = updatedFiles.remove(getKey(state)) != null;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removing state file " + state + "; now " + updatedFiles);
            }
            return ret;
        }
    }


    @Nullable
    public P4FileUpdateState getUpdateStateFor(@NotNull final FilePath file) {
        synchronized (sync) {
            return updatedFiles.get(file.getIOFile());
        }
    }


    @Override
    public String toString() {
        return updatedFiles.toString();
    }

    @NotNull
    private File getKey(@NotNull P4FileUpdateState state) {
        final FilePath path = state.getLocalFilePath();
        if (path == null) {
            throw new IllegalArgumentException("No local path in state (" + state + ")");
        }
        return path.getIOFile();
   }
}

