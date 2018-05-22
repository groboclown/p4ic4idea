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

package net.groboclown.p4.server.impl.cache;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.cache.CacheQueryHandler;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 *
 */
public class IdeFileMapImpl implements IdeFileMap {
    private final CacheQueryHandler cache;

    public IdeFileMapImpl(CacheQueryHandler queryHandler) {
        this.cache = queryHandler;
    }

    @Nullable
    @Override
    public P4LocalFile forIdeFile(VirtualFile file) {
        return null;
    }

    @Nullable
    @Override
    public P4LocalFile forIdeFile(FilePath file) {
        return null;
    }

    @Nullable
    @Override
    public P4LocalFile forDepotPath(P4RemoteFile file) {
        return null;
    }

    @NotNull
    @Override
    public Stream<P4LocalFile> getLinkedFiles() {
        return null;
    }
}
