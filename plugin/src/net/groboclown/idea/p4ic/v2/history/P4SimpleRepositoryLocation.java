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
package net.groboclown.idea.p4ic.v2.history;

import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.exceptions.P4FileException;

public class P4SimpleRepositoryLocation implements RepositoryLocation {
    private final String depotPath;

    public P4SimpleRepositoryLocation(String depotPath) {
        this.depotPath = depotPath;
    }

    public IFileSpec getP4FileInfo() throws P4FileException {
        return FileSpecUtil.getFromDepotPath(depotPath, IFileSpec.NO_FILE_REVISION);
    }

    @Override
    public String toPresentableString() {
        return depotPath;
    }

    @Override
    public String getKey() {
        return depotPath;
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
