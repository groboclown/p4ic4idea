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
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A cached view of a file with pending actions.
 */
public interface CachedAlteredFile {
    @NotNull
    P4FileAction getAction();

    @NotNull
    List<P4CommandRunner.ClientAction<?>> getPendingActions();

    /**
     * The file that was altered, as on the local file system.
     *
     * @return the altered local file
     */
    @NotNull
    FilePath getAlteredFile();

    /**
     * The original file location of the altered file.  This will only be different than the
     * {@link #getAlteredFile()} if the user moved the file or integrated the file from another location.
     *
     * @return the path to the original file location
     */
    @NotNull
    FilePath getSourceFile();

    /**
     * Returns whether the file was moved from its source location.  If the source location is different
     * than the altered file location, and it was not moved, then the file was integrated from the source.
     * If the file was moved, then that means that the source file should be removed.
     *
     * @return true if the file was moved.
     */
    boolean isFileMoved();

    /**
     *
     * @return the contents of the file before the change occurred, so that reverts may successfully be performed.
     *      Returns null if the source contents were for a file that didn't exist.
     */
    @Nullable
    byte[] getSourceContents();

    /**
     * The file type for the altered file.  If unknown, or the altered file is marked for deletion, then returns null.
     *
     * @return the requested file type for the file action.
     */
    @Nullable
    P4FileType getFileType();
}
