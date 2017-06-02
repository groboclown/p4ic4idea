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

package net.groboclown.idea.p4ic.v2.changes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.idea.p4ic.v2.server.cache.ClientServerRef;
import net.groboclown.idea.p4ic.v2.server.cache.state.P4ShelvedFile;
import net.groboclown.idea.p4ic.v2.server.util.ShelvedFilePath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4ShelvedContentRevision implements ContentRevision {
    private static final P4CurrentContentRevision.CurrentRevisionNumber
            HAVE_REV = new P4CurrentContentRevision.CurrentRevisionNumber();

    private final ShelvedFilePath filePath;

    public P4ShelvedContentRevision(@Nullable Project project, @NotNull ClientServerRef clientServerRef,
            @NotNull P4ShelvedFile shelvedFile) {
        this.filePath = new ShelvedFilePath(shelvedFile);
    }

    @Nullable
    @Override
    public String getContent()
            throws VcsException {
        // TODO Not yet supported
        return null;
    }

    @NotNull
    @Override
    public FilePath getFile() {
        return this.filePath;
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return HAVE_REV;
    }
}
