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
import com.intellij.openapi.vcs.FilePathImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Maps a depot file to a local client file.  These are shared across the state.
 * <p/>
 * Usage: these are expected to be unique per client; as such, the only thing
 * this cares about for equality and hashcode is the depot path.  If the client
 * workspace is updated to change the local file mapping, then this object is
 * still valid.
 * <p/>
 * This is not a {@link CachedState} object, because it cannot reflect a server
 * state; it instead just reflects a depot file and how it relates to the local
 * file system.
 */
public final class P4ClientFileMapping {
    @NotNull
    private final String depotPath;

    @Nullable
    private String localPath;

    public P4ClientFileMapping(@NotNull String depotPath, @Nullable String localPath) {
        this.depotPath = depotPath;
        this.localPath = localPath;
    }

    @NotNull
    public String getDepotPath() {
        return depotPath;
    }

    @Nullable
    public String getLocalPath() {
        return localPath;
    }

    @Nullable
    public FilePath getLocalFilePath() {
        return getFilePath(localPath);
    }

    // called by FileMappingRepo
    void updateLocalPath(@Nullable final String localPath) {
        this.localPath = localPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (! obj.getClass().equals(getClass())) {
            return false;
        }
        P4ClientFileMapping that = (P4ClientFileMapping) obj;
        return that.depotPath.equals(depotPath);
    }

    @Override
    public int hashCode() {
        return depotPath.hashCode();
    }

    protected void serialize(@NotNull Element wrapper) {
        wrapper.setAttribute("d", depotPath);
        wrapper.setAttribute("l", localPath == null ? "" : localPath);
    }

    @Nullable
    protected static P4ClientFileMapping deserialize(@NotNull Element wrapper) {
        String depot = CachedState.getAttribute(wrapper, "d");
        if (depot == null) {
            return null;
        }
        return new P4ClientFileMapping(depot, CachedState.getAttribute(wrapper, "l"));
    }


    private static FilePath getFilePath(@Nullable String path) {
        if (path == null) {
            return null;
        }
        File f = new File(path);
        return new FilePathImpl(f, f.isDirectory());
    }
}
