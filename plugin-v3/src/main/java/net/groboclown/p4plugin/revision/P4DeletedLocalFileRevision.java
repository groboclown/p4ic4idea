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

package net.groboclown.p4plugin.revision;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4DeletedLocalFileRevision implements ContentRevision {
    private final P4LocalFile localFile;

    public P4DeletedLocalFileRevision(@NotNull P4LocalFile localFile) {
        this.localFile = localFile;
    }

    @Nullable
    @Override
    public String getContent() {
        return null;
    }

    @NotNull
    @Override
    public FilePath getFile() {
        return localFile.getFilePath();
    }

    @NotNull
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        // Revision 0 == deleted
        return new VcsRevisionNumber.Int(0);
    }
}
