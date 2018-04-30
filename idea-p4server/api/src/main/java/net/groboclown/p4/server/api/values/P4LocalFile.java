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

package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A local filesystem file, used for representing its state as the
 * server knows it, or as we want the sever to know it.
 */
@Immutable
public interface P4LocalFile {
    /**
     *
     * @return null if the file isn't in the depot.
     */
    @Nullable
    P4RemoteFile getDepotPath();

    // Can't be null, because this is a local file.  However, it can be non-existent if locally deleted.
    @NotNull
    FilePath getFilePath();

    /**
     *
     * @return {@link P4Revision#NOT_ON_SERVER} if the file does not exist on the server.
     */
    @NotNull
    P4Revision getHaveRevision();

    /**
     *
     * @return null if the file does not exist on the server.
     */
    @Nullable
    P4FileRevision getHeadFileRevision();

    /**
     *
     * @return null if not checked out, otherwise the changelist number associated to the local edit action.
     */
    @Nullable
    P4ChangelistId getChangelistId();

    @NotNull
    P4FileAction getFileAction();

    @NotNull
    P4ResolveType getResolveType();

    @NotNull
    P4FileType getFileType();
}
