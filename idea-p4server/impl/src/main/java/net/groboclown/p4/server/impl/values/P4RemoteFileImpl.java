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

package net.groboclown.p4.server.impl.values;

import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.util.HandleFileSpecUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class P4RemoteFileImpl implements P4RemoteFile {
    private final String displayName;
    private final String path;
    private final String localPath;

    public static List<P4RemoteFile> createFor(@NotNull Collection<IFileSpec> specList) {
        return specList.stream()
                .filter((spec) -> spec.getStatusMessage() == null && spec.getDepotPath() != null)
                .map(P4RemoteFileImpl::new)
                .collect(Collectors.toList());
    }

    public static List<P4RemoteFile> createForExtended(@NotNull Collection<IExtendedFileSpec> specList) {
        return specList.stream()
                .filter((spec) -> spec.getStatusMessage() == null && spec.getDepotPath() != null)
                .map(P4RemoteFileImpl::new)
                .collect(Collectors.toList());
    }

    public P4RemoteFileImpl(@NotNull IFileSpec spec) {
        if (spec.getDepotPath() != null) {
            this.path = spec.getDepotPath().getPathString();
            this.displayName = HandleFileSpecUtil.getDepotDisplayName(spec);
        } else if (spec.getClientPath() != null) {
            this.path = spec.getClientPath().getPathString();
            this.displayName = spec.getClientPath().getPathString();
        } else if (spec.getLocalPath() != null) {
            this.path = spec.getLocalPath().getPathString();
            this.displayName = spec.getLocalPath().getPathString();
        } else if (spec.getOriginalPath() != null) {
            this.path = spec.getOriginalPath().getPathString();
            this.displayName = spec.getOriginalPath().getPathString();
        } else {
            throw new NullPointerException("Invalid spec path " + spec);
        }

        this.localPath = spec.getLocalPathString();
    }

    P4RemoteFileImpl(@NotNull String path) {
        this.path = path;
        this.displayName = path;
        this.localPath = null;
    }

    P4RemoteFileImpl(@NotNull IFileAnnotation ann) {
        // Note: depotPath may be null in very rare circumstances.
        this.path = ann.getDepotPath() == null ? "<unknown>" : ann.getDepotPath();
        this.displayName = HandleFileSpecUtil.getDepotDisplayName(ann);
        this.localPath = null;
    }

    public P4RemoteFileImpl(@NotNull String path, @NotNull String displayName, @Nullable String localPath) {
        this.path = path;
        this.displayName = displayName;
        this.localPath = localPath;
    }

    @NotNull
    @Override
    public String getDepotPath() {
        return path;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @NotNull
    @Override
    public Optional<String> getLocalPath() {
        return Optional.ofNullable(localPath);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof P4RemoteFile)) {
            return false;
        }
        P4RemoteFile that = (P4RemoteFile) o;
        return that.getDepotPath().equals(getDepotPath());
    }

    @Override
    public int hashCode() {
        return path.hashCode() + 1;
    }

    @Override
    public String toString() {
        return path;
    }
}
