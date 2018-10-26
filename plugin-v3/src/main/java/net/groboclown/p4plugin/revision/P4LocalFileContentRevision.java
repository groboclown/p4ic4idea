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

import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.values.P4LocalFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.Comparing.equal;

public class P4LocalFileContentRevision extends AbstractP4FileContentRevision {
    private final P4LocalFile file;

    public P4LocalFileContentRevision(@Nullable ClientConfig clientConfig, @NotNull P4LocalFile file,
            @Nullable HistoryContentLoader loader) {
        super(clientConfig, file.getFilePath(), file.getFilePath().getPath(), file.getHaveRevision(), loader,
                // TODO use the correct charset
                null);
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof P4LocalFileContentRevision) {
            P4LocalFileContentRevision that = (P4LocalFileContentRevision) o;
            return equal(that.file.getDepotPath(), file.getDepotPath())
                    && equal(that.getRevisionNumber(), getRevisionNumber());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (file.getDepotPath() == null ? 0 : file.getDepotPath().hashCode()) +
                getRevisionNumber().hashCode();
    }

    @Override
    public String toString() {
        return file.toString();
    }
}