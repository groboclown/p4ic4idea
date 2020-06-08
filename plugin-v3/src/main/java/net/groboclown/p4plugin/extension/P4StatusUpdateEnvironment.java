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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.FileGroup;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Checks the latest status of the files on the server
 */
public class P4StatusUpdateEnvironment
        implements UpdateEnvironment {
    private final Project project;

    public static final String OFFLINE_GROUP_ID = "p4.offline";

    public P4StatusUpdateEnvironment(@NotNull Project project) {
        this.project = project;
    }


    @Override
    public void fillGroups(UpdatedFiles updatedFiles) {
        updatedFiles.registerGroup(new FileGroup(
                P4Bundle.message("update.status.offline"),
                P4Bundle.message("update.status.offline"),
                false,
                OFFLINE_GROUP_ID,
                false));
    }

    /**
     * Performs the update/integrate/status operation.
     *
     * @param contentRoots      the content roots for which update/integrate/status was requested by the user.
     * @param updatedFiles      the holder for the results of the update/integrate/status operation.
     * @param progressIndicator the indicator that can be used to report the progress of the operation.
     * @param context           in-out parameter: a link between several sequential update operations (that can be triggered by one update action)
     * @return the update session instance, which can be used to get information about errors that have occurred
     * during the operation and to perform additional post-update processing.
     * @throws ProcessCanceledException if the update operation has been cancelled by the user. Alternatively,
     *                                  cancellation can be reported by returning true from
     *                                  {@link UpdateSession#isCanceled}.
     */
    @NotNull
    @Override
    public UpdateSession updateDirectories(@NotNull FilePath[] contentRoots, UpdatedFiles updatedFiles,
            ProgressIndicator progressIndicator, @NotNull Ref<SequentialUpdatesContext> context)
            throws ProcessCanceledException {
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public Configurable createConfigurable(Collection<FilePath> files) {
        // No UI for checking status
        return null;
    }

    @Override
    public boolean validateOptions(Collection<FilePath> roots) {
        return false;
    }

}
