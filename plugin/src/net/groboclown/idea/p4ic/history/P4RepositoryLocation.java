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
package net.groboclown.idea.p4ic.history;

import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.server.P4FileInfo;

public class P4RepositoryLocation implements RepositoryLocation {
    private final P4FileInfo file;

    public P4RepositoryLocation(P4FileInfo file) {
        this.file = file;
    }

    public P4FileInfo getP4FileInfo() {
        return file;
    }

    @Override
    public String toPresentableString() {
        return file.getDepotPath();
    }

    @Override
    public String getKey() {
        return file.getDepotPath();
    }

    @Override
    public void onBeforeBatch() throws VcsException {
        // do nothing
    }

    @Override
    public void onAfterBatch() {
        // do nothing
    }
}
