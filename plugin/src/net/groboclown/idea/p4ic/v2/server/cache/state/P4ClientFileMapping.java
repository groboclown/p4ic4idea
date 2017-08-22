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
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Maps a depot file to a local client file.  These are shared across the state.
 * <p/>
 * Usage: these are expected to be unique per client; as such, the only thing
 * this cares about for equality and hashcode is the depot path.  If the client
 * workspace is updated to change the local file mapping, then this object is
 * still valid.
 * <p/>
 * The local path can only be null if the depot path references a
 * remote location; the depot path can be null if an operation was performed
 * while off-line, and the depot path was not known at the time.
 * <p/>
 * This is not a {@link CachedState} object, because it cannot reflect a server
 * state; it instead just reflects a depot file and how it relates to the local
 * file system.
 */
public final class P4ClientFileMapping {
    private static final Logger LOG = Logger.getInstance(P4ClientFileMapping.class);

    // NOTE: this does not implement hashCode or equals.  Due to the changing
    // nature of the class, it leads to too many problems when this is used
    // in a hash set or as a hash map key.  Instead, use an invariant
    // as the key.

    @Nullable
    private String depotPath;

    @Nullable
    private FilePath localFilePath;

    // called by FileMappingRepo
    P4ClientFileMapping(@NotNull String depotPath) {
        assert depotPath.length() > 0;
        this.depotPath = depotPath;
        this.localFilePath = null;
    }

    // called by FileMappingRepo
    P4ClientFileMapping(@Nullable String depotPath, @NotNull FilePath localFilePath) {
        if (depotPath != null && depotPath.length() <= 0) {
            depotPath = null;
        }
        this.depotPath = depotPath;
        this.localFilePath = localFilePath;
    }

    @Nullable
    public String getDepotPath() {
        return depotPath;
    }

    @Nullable
    public String getLocalPath() {
        return localFilePath == null ? null : localFilePath.getIOFile().getAbsolutePath();
    }

    @Nullable
    public FilePath getLocalFilePath() {
        return localFilePath;
    }

    @NotNull
    public IFileSpec getFileSpec() throws P4Exception {
        if (depotPath != null) {
            return FileSpecUtil.getFromDepotPath(depotPath, IFileSpec.NO_FILE_REVISION);
        }
        if (localFilePath != null) {
            return FileSpecUtil.getFromFilePath(localFilePath);
        }
        throw new IllegalStateException("no valid path for " + this);
    }

    // called by FileMappingRepo; requires local path maps to be updated
    void updateLocalPath(@Nullable FilePath localFilePath) {
        this.localFilePath = localFilePath;
    }

    // called by FileMappingRepo; requires depot maps to be updated
    void updateDepot(@NotNull final String depot) {
        this.depotPath = depot;
    }

    @Override
    public String toString() {
        if (depotPath == null && localFilePath == null) {
            return "null";
        }
        if (depotPath == null) {
            return localFilePath.getIOFile().getAbsolutePath();
        }
        if (localFilePath == null) {
            return depotPath;
        }
        return depotPath + " -> " + localFilePath.getIOFile().getAbsolutePath();
    }


    protected void serialize(@NotNull Element wrapper) {
        if (depotPath != null) {
            wrapper.setAttribute("d", depotPath);
        }
        wrapper.setAttribute("l", getLocalPath() == null ? "" : getLocalPath());
    }

    @Nullable
    protected static P4ClientFileMapping deserialize(@NotNull Element wrapper) {
        String depot = CachedState.getAttribute(wrapper, "d");
        if (depot != null && depot.length() <= 0) {
            depot = null;
        }
        String shelvedChangeIdS = CachedState.getAttribute(wrapper, "s");
        int shelvedChangeId = IChangelist.UNKNOWN;
        if (shelvedChangeIdS != null) {
            try {
                shelvedChangeId = Integer.parseInt(shelvedChangeIdS);
            } catch (NumberFormatException e) {
                shelvedChangeId = IChangelist.UNKNOWN;
            }
        }
        String localPath = CachedState.getAttribute(wrapper, "l");

        FilePath localFilePath = FilePathUtil.getFilePath(localPath);
        if (depot == null && localFilePath == null) {
            return null;
        }
        if (localFilePath == null) {
            return new P4ClientFileMapping(depot);
        }
        return new P4ClientFileMapping(depot, localFilePath);
    }
}
