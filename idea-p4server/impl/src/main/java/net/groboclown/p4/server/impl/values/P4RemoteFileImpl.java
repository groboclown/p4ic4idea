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

import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import net.groboclown.p4.server.impl.util.HandleFileSpecUtil;
import org.jetbrains.annotations.NotNull;

public class P4RemoteFileImpl implements P4RemoteFile {
    private final String displayName;
    private final String path;

    public P4RemoteFileImpl(@NotNull IFileSpec spec) {
        this.path = spec.getDepotPath().getPathString();
        this.displayName = HandleFileSpecUtil.getDepotDisplayName(spec);
    }

    public P4RemoteFileImpl(@NotNull String path) {
        this.path = path;
        this.displayName = path;
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
}
