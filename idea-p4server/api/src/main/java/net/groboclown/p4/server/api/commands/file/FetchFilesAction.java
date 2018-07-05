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

package net.groboclown.p4.server.api.commands.file;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.commands.ActionUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FetchFilesAction implements P4CommandRunner.ClientAction<FetchFilesResult> {
    private final String actionId = ActionUtil.createActionId(FetchFilesAction.class);
    private final List<String> syncPath;
    private final boolean force;

    public FetchFilesAction(String syncPath) {
        this.syncPath = Collections.singletonList(syncPath);
        this.force = false;
    }

    public FetchFilesAction(File syncPath) {
        if (syncPath.isDirectory()) {
            this.syncPath = Collections.singletonList(syncPath.getAbsolutePath() + "/...");
        } else {
            this.syncPath = Collections.singletonList(syncPath.getAbsolutePath());
        }
        this.force = false;
    }

    public FetchFilesAction(Collection<FilePath> syncPaths, boolean force) {
        List<String> paths = new ArrayList<>(syncPaths.size());
        for (FilePath path : syncPaths) {
            if (path.isDirectory()) {
                paths.add(path.getPath() + "/...");
            } else {
                paths.add(path.getPath());
            }
        }
        this.syncPath = paths;
        this.force = force;
    }

    @NotNull
    @Override
    public Class<? extends FetchFilesResult> getResultType() {
        return FetchFilesResult.class;
    }

    @Override
    public P4CommandRunner.ClientActionCmd getCmd() {
        return P4CommandRunner.ClientActionCmd.FETCH_FILES;
    }

    @NotNull
    @Override
    public String getActionId() {
        return actionId;
    }

    public List<String> getSyncPath() {
        return syncPath;
    }

    public boolean isForce() {
        return force;
    }
}
