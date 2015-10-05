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

package net.groboclown.idea.p4ic.ui.revision;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.util.FilePathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PathRevsSet {
    private static final Logger LOG = Logger.getInstance(PathRevsSet.class);

    // TODO make configurable
    private static final int REVISION_PAGE_SIZE = 1000;

    private final List<PathRevs> revs;
    private final String error;

    public static PathRevsSet create(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        FilePath fp = FilePathUtil.getFilePath(file);
        final P4Server server;
        try {
            server = vcs.getP4ServerFor(fp);
            if (server == null) {
                return new PathRevsSet(P4Bundle.getString("revision.list.notconnected"));
            } else {
                final Map<FilePath, IExtendedFileSpec> status =
                        server.getFileStatus(Collections.singletonList(fp));
                if (status == null) {
                    return new PathRevsSet(P4Bundle.getString("revision.list.notconnected"));
                }
                if (status.get(fp) == null) {
                    return new PathRevsSet(P4Bundle.getString("revision.list.nosuchfile"));
                }
                final IExtendedFileSpec spec = status.get(fp);
                LOG.info("diff file depot: " + spec.getDepotPathString());
                final List<P4FileRevision> history =
                        server.getRevisionHistory(spec, REVISION_PAGE_SIZE);
                if (history == null) {
                    return new PathRevsSet(P4Bundle.getString("revision.list.nosuchfile"));
                }
                final List<PathRevs> revisions = PathRevs.getPathRevs(history);
                if (revisions.isEmpty()) {
                    return new PathRevsSet(P4Bundle.message("revision.list.no-revs", file));
                }
                return new PathRevsSet(revisions);
            }
        } catch (InterruptedException e) {
            return new PathRevsSet(P4Bundle.getString("revision.list.timeout"));
        }
    }

    PathRevsSet(@NotNull final List<PathRevs> revs) {
        this.revs = Collections.unmodifiableList(revs);
        this.error = null;
    }

    PathRevsSet(@NotNull final String errs) {
        this.revs = Collections.emptyList();
        this.error = errs;
    }


    public boolean isError() {
        return error != null;
    }

    public List<PathRevs> getPathRevs() {
        return revs;
    }

    public String getError() {
        return error;
    }
}
