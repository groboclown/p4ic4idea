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
package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.update.*;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.ServerExecutor;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Checks the latest
 */
public class P4StatusUpdateEnvironment implements UpdateEnvironment {
    private final Project project;

    public P4StatusUpdateEnvironment(@NotNull Project project) {
        this.project = project;
    }


    @Override
    public void fillGroups(UpdatedFiles updatedFiles) {
        // No non-standard file status, so ignored.
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
        progressIndicator.setFraction(0.0);
        StatusUpdateSession session = new StatusUpdateSession();
        if (project.isDisposed()) {
            session.exceptions.add(new P4InvalidConfigException("project disposed"));
            return session;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);
        Map<Client, List<FilePath>> mappedRoots;
        try {
            mappedRoots = vcs.mapFilePathToClient(Arrays.asList(contentRoots));
        } catch (P4InvalidConfigException e) {
            session.exceptions.add(e);
            return session;
        }

        double serverConfigPos = 0.0;
        for (Map.Entry<Client, List<FilePath>> en: mappedRoots.entrySet()) {
            SubProgressIndicator procIndConfig = new SubProgressIndicator(progressIndicator,
                    0.1 + (0.6 * (serverConfigPos / (double) mappedRoots.size())),
                    0.1 + (0.6 * ((serverConfigPos + 1.0) / (double) mappedRoots.size())));
            serverConfigPos += 1.0;
            try {
                ServerExecutor exec = en.getKey().getServer();
                List<P4FileInfo> infos = exec.loadDeepFileInfo(en.getValue());
                double filePos = 0.0;
                for (P4FileInfo info : infos) {
                    procIndConfig.setFraction(filePos / (double) infos.size());
                    filePos += 1.0;
                    FileGroup group = updatedFiles.getGroupById(info.getClientAction().getFileGroupId());
                    group.add(info.getPath().getIOFile().getAbsolutePath(),
                            P4Vcs.getKey(), new VcsRevisionNumber.Int(info.getHeadRev()));
                }
            } catch (VcsException ex) {
                session.exceptions.add(ex);
            }
            procIndConfig.setFraction(1.0);
        }


        return session;
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


    static class StatusUpdateSession implements UpdateSession {
        private boolean cancelled = false;
        private List<VcsException> exceptions = new ArrayList<VcsException>();

        @NotNull
        @Override
        public List<VcsException> getExceptions() {
            return exceptions;
        }

        @Override
        public void onRefreshFilesCompleted() {

        }

        @Override
        public boolean isCanceled() {
            return cancelled;
        }
    }
}
