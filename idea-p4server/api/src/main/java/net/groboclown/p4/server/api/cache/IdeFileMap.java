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

package net.groboclown.p4.server.api.cache;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.api.values.P4RemoteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Maintains a mapping between the local files as the projects see them, and the Perforce server view of the
 * files.  This includes updating the IDE file status to indicate how the file is open (deleted, edit, add, etc).
 * This does not manage mapping files to changelists.  This should only maintain files which the user has modified
 * in the client.
 * <p>
 * This does not query the server.  Rather, other actions may query the server which would update this mapping.
 * <p>
 * This file should maintain files for a single project, rather than server-wide.  This should reduce potential
 * threading issues.
 */
public interface IdeFileMap {
    @Nullable
    P4LocalFile forIdeFile(VirtualFile file);

    @Nullable
    P4LocalFile forIdeFile(FilePath file);

    @Nullable
    P4LocalFile forDepotPath(P4RemoteFile file);

    /**
     *
     * @return all the known links between server files and local files.
     */
    @NotNull
    Stream<P4LocalFile> getLinkedFiles();

    /**
     * Updates the internal links between the remote files and the local files.  If an
     * existing linked file is not contained in the argument, then it is assumed to no longer
     * be opened by any of the project's clients, and will be marked accordingly.
     * <p>
     * This will not update the association of the files to changelists.  That can only
     * be done through {@link IdeChangelistMap#updateForOpenChanges(Stream)}.
     *
     * @param files all the known files open in the current project.
     */
    void updateAllLinkedFiles(@NotNull Stream<P4LocalFile> files);
}
