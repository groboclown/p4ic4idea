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
package net.groboclown.idea.p4ic.v2.extension;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.update.*;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.server.exceptions.VcsInterruptedException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import net.groboclown.idea.p4ic.v2.history.P4RevisionNumber;
import net.groboclown.idea.p4ic.v2.history.P4RevisionNumber.RevType;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Checks the latest
 */
public class P4StatusUpdateEnvironment implements UpdateEnvironment {
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
        progressIndicator.setFraction(0.0);
        StatusUpdateSession session = new StatusUpdateSession();
        if (project.isDisposed()) {
            session.exceptions.add(new P4InvalidConfigException("project disposed"));
            return session;
        }
        P4Vcs vcs = P4Vcs.getInstance(project);
        Map<P4Server, List<FilePath>> mappedRoots;
        try {
            mappedRoots = vcs.mapFilePathsToP4Server(Arrays.asList(contentRoots));
        } catch (InterruptedException e) {
            session.exceptions.add(new VcsInterruptedException(e));
            return session;
        }

        double serverConfigPos = 0.0;
        for (Map.Entry<P4Server, List<FilePath>> en: mappedRoots.entrySet()) {
            SubProgressIndicator procIndConfig = new SubProgressIndicator(progressIndicator,
                    0.1 + (0.6 * (serverConfigPos / (double) mappedRoots.size())),
                    0.1 + (0.6 * ((serverConfigPos + 1.0) / (double) mappedRoots.size())));
            serverConfigPos += 1.0;
            try {
                P4Server server = en.getKey();
                final Map<FilePath, IExtendedFileSpec> infos =
                        server.getFileStatus(en.getValue());
                if (infos != null) {
                    double filePos = 0.0;
                    for (Map.Entry<FilePath, IExtendedFileSpec> infoEntry: infos.entrySet()) {
                        procIndConfig.setFraction(filePos / (double) infos.size());
                        filePos += 1.0;
                        final IExtendedFileSpec spec = infoEntry.getValue();
                        FileGroup group = updatedFiles.getGroupById(getGroupId(spec));
                        group.add(infoEntry.getKey().getIOFile().getAbsolutePath(), P4Vcs.getKey(),
                                new P4RevisionNumber(infoEntry.getKey(),
                                        spec.getDepotPathString(), spec,
                                        // Have revision - we're seeing what the user has synced.
                                        RevType.HAVE));
                    }
                } else {
                    FileGroup group = updatedFiles.getGroupById(OFFLINE_GROUP_ID);
                    for (FilePath filePath: en.getValue()) {
                        // FIXME get cached known state
                        group.add(filePath.getIOFile().getAbsolutePath(),
                                P4Vcs.getKey(),
                                null);
                    }
                }
                // FIXME else mark them w/ offline status
            } catch (InterruptedException ex) {
                session.exceptions.add(new VcsInterruptedException(ex));
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

    @NotNull
    static String getGroupId(@NotNull IExtendedFileSpec spec) {
        final FileAction mainAction = spec.getAction();
        final FileAction openAction = spec.getOpenAction();
        final FileAction headAction = spec.getHeadAction();

        if (openAction == null && mainAction == null) {
            if (headAction == null) {
                return FileGroup.LOCALLY_ADDED_ID;
            }
            return getGroupId(headAction);
        }
        if (mainAction == null) {
            return getGroupId(openAction);
        }
        return getGroupId(mainAction);
    }


    @NotNull
    static String getGroupId(@NotNull FileAction action) {
        // TODO test out these statuses, to make sure they map right.

        switch (action) {
            case ADD:
            case ADD_EDIT:
                return FileGroup.LOCALLY_ADDED_ID;

            case ADDED:
            case BRANCH:
                return FileGroup.CREATED_ID;

            case EDIT:
            case INTEGRATE:
            case REPLACED:
            case UPDATED:
            case EDIT_FROM:
                return FileGroup.CHANGED_ON_SERVER_ID;

            case DELETE:
                return FileGroup.LOCALLY_REMOVED_ID;

            case DELETED:
            case MOVE_DELETE:
                return FileGroup.REMOVED_FROM_REPOSITORY_ID;

            case SYNC:
            case REFRESHED:
                return FileGroup.CHANGED_ON_SERVER_ID;

            case IGNORED:
            case ABANDONED:
            case EDIT_IGNORED:
                return FileGroup.SKIPPED_ID;

            case MOVE:
            case MOVE_ADD:
            case COPY_FROM:
            case MERGE_FROM:
                return FileGroup.MERGED_ID;

            case RESOLVED:
                return FileGroup.MERGED_ID;

            case UNRESOLVED:
                return FileGroup.MERGED_WITH_CONFLICT_ID;

            case PURGE:
            case IMPORT:
            case ARCHIVE:
            case UNKNOWN:
            default:
                return FileGroup.UNKNOWN_ID;

        }
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
