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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class FileUpdateStateList implements Iterable<P4FileUpdateState> {
    private static final Logger LOG = Logger.getInstance(FileUpdateStateList.class);

    private final Set<P4FileUpdateState> updatedFiles;
    private final Object sync = new Object();

    public FileUpdateStateList() {
        this.updatedFiles = new HashSet<P4FileUpdateState>();
    }

    @NotNull
    @Override
    public Iterator<P4FileUpdateState> iterator() {
        return copy().iterator();
    }


    @NotNull
    public Set<P4FileUpdateState> copy() {
        synchronized (sync) {
            return new HashSet<P4FileUpdateState>(updatedFiles);
        }
    }


    public void replaceWith(@NotNull Collection<P4FileUpdateState> newValues) {
        synchronized (sync) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Replacing update state files with " + newValues + "; was " + updatedFiles);
            }
            updatedFiles.clear();
            updatedFiles.addAll(newValues);
        }
    }


    public void add(@NotNull P4FileUpdateState state) {
        synchronized (sync) {
            updatedFiles.add(state);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding state file with " + state + "; now " + updatedFiles);
            }
        }
    }


    public boolean remove(@NotNull P4FileUpdateState state) {
        synchronized (sync) {
            final boolean ret = updatedFiles.remove(state);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Removing state file " + state + "; now " + updatedFiles);
            }
            return ret;
        }
    }


    @Nullable
    public P4FileUpdateState getUpdateStateFor(@NotNull final FilePath file) {
        for (P4FileUpdateState updatedFile : copy()) {
            if (file.equals(updatedFile.getLocalFilePath())) {
                return updatedFile;
            }
        }
        return null;
    }


    @Override
    public String toString() {
        return updatedFiles.toString();
    }
}

