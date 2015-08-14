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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PathRevsSet {
    private static final Logger LOG = Logger.getInstance(PathRevsSet.class);


    // TODO make configurable
    private static final int REVISION_PAGE_SIZE = 1000;


    private final List<PathRevs> revs;
    private final String error;

    public static PathRevsSet create(final @NotNull P4Vcs vcs, final @NotNull VirtualFile file) {
        final Client client = vcs.getClientFor(file);
        if (client == null || client.isWorkingOffline()) {
            return new PathRevsSet(P4Bundle.getString("revision.list.notconnected"));
        } else {
            try {
                final List<P4FileInfo> p4infoList = client.getServer().getVirtualFileInfo(Collections.singleton(file));
                if (p4infoList.isEmpty()) {
                    // can't find file
                    return new PathRevsSet(P4Bundle.getString("revision.list.nosuchfile"));
                } else {
                    final P4FileInfo fileInfo = p4infoList.get(0);
                    LOG.info("diff file depot: " + fileInfo.getDepotPath());

                    final List<PathRevs> revisions =
                            PathRevs.getPathRevs(client.getServer().getRevisionHistory(fileInfo, REVISION_PAGE_SIZE));

                    if (revisions.isEmpty()) {
                        return new PathRevsSet(P4Bundle.message("revision.list.no-revs", file));
                    }

                    return new PathRevsSet(revisions);
                }
            } catch (VcsException e) {
                LOG.warn(e);
                return new PathRevsSet(e.getMessage());
            }
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
