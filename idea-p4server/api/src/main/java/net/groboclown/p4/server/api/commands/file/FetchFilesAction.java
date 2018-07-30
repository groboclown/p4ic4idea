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
import net.groboclown.p4.server.api.commands.AbstractNonCachedClientAction;
import net.groboclown.p4.server.api.commands.ActionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FetchFilesAction extends AbstractNonCachedClientAction<FetchFilesResult> {
    private final List<FilePath> syncPaths;
    private final String pathAnnotation;
    private final boolean force;

    public FetchFilesAction(@NotNull List<FilePath> syncPaths, @Nullable String pathAnnotation, boolean force) {
        this.syncPaths = syncPaths;
        this.pathAnnotation = pathAnnotation;
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

    public boolean isForce() {
        return force;
    }

    public List<FilePath> getSyncPaths() {
        return syncPaths;
    }

    public String getPathAnnotation() {
        return pathAnnotation;
    }
}
