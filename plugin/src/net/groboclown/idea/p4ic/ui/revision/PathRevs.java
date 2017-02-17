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
import net.groboclown.idea.p4ic.v2.history.P4FileRevision;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PathRevs {
    private static final Logger LOG = Logger.getInstance(PathRevs.class);

    private final String depotPath;
    private final List<P4FileRevision> revs;

    @NotNull
    static List<PathRevs> getPathRevs(@NotNull final List<P4FileRevision> revs) {
        List<PathRevs> ret = new ArrayList<PathRevs>();
        PathRevs current = null;
        for (P4FileRevision rev : revs) {
            String depotPath = rev.getRevisionDepotPath();
            if (depotPath != null) {
                if (current == null) {
                    // LOG.info(":: " + depotPath);
                    current = new PathRevs(depotPath);
                    ret.add(current);
                } else if (! depotPath.equals(current.depotPath)) {
                    // LOG.info(":: " + depotPath);
                    current = new PathRevs(depotPath);
                    ret.add(current);
                }
                // LOG.info(":: -> " + rev.getRev());
                current.revs.add(rev);
            }
        }

        return ret;
    }



    private PathRevs(final String depotPath) {
        this.depotPath = depotPath;
        this.revs = new ArrayList<P4FileRevision>();
    }

    public String getDepotPath() {
        return depotPath;
    }

    public List<P4FileRevision> getRevisions() {
        return Collections.unmodifiableList(revs);
    }
}
