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
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.ui.DummyProgressIndicator;
import net.groboclown.p4plugin.ui.sync.SyncOptionConfigurable;
import net.groboclown.p4plugin.ui.sync.SyncOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class P4SyncUpdateEnvironment
        implements UpdateEnvironment {
    private final Project project;
    private SyncOptions options = SyncOptions.createDefaultSyncOptions();

    P4SyncUpdateEnvironment(Project project) {
        this.project = project;
    }

    @Override
    public void fillGroups(UpdatedFiles updatedFiles) {
        // The plugin doesn't have any non-standard update file groups, so this call does nothing.
    }

    @NotNull
    @Override
    public UpdateSession updateDirectories(@NotNull FilePath[] filePaths, UpdatedFiles updatedFiles,
            ProgressIndicator pi, @NotNull Ref<SequentialUpdatesContext> ref)
            throws ProcessCanceledException {
        ProgressIndicator progressIndicator = DummyProgressIndicator.nullSafe(pi);
        progressIndicator.setFraction(0.0);
        final SyncUpdateSession ret = new SyncUpdateSession();
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            ret.exceptions.add(new VcsException("Plugin disposed"));
            return ret;
        }

        final Map<String, FileGroup> groups = collateByFileGroupId(updatedFiles.getTopLevelGroups(), null);
        final Map<ClientConfigRoot, List<FilePath>> filesByRoot = new HashMap<>();
        for (FilePath filePath : filePaths) {
            filesByRoot.computeIfAbsent(registry.getClientFor(filePath), k -> new ArrayList<>()).add(filePath);
        }
        final double inc = filesByRoot.isEmpty() ? 1.0 : (1.0 / filesByRoot.size());
        final double[] pos = { 0.0 };

        filesByRoot.forEach((key, value) -> {
            try {
                FetchFilesResult res =
                        P4ServerComponent
                                .perform(project, key.getClientConfig(),
                                        new FetchFilesAction(value, options.getSpecAnnotation(),
                                                options.isForce()))
                                .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project),
                                        TimeUnit.MILLISECONDS);
                updateForResult(res, groups);
                pos[0] += inc;
                progressIndicator.setFraction(pos[0]);
            } catch (P4CommandRunner.ServerResultException e) {
                ret.exceptions.add(e);
            } catch (InterruptedException e) {
                // TODO better exception?
                ret.exceptions.add(new VcsException(e));
            }
        });
        return ret;
    }

    private void updateForResult(@Nullable FetchFilesResult res, Map<String, FileGroup> groups) {
        if (res == null) {
            return;
        }
        for (P4LocalFile file : res.getFiles()) {

            // hardRefresh and refresh are deprecated now.  We don't need to do that anymore.
            // path.hardRefresh();

            String groupId = getGroupIdFor(file);
            FileGroup group = groups.get(groupId);
            if (group != null) {
                group.add(file.getFilePath().getIOFile().getAbsolutePath(),
                        P4Vcs.getKey(), file.getHaveRevision());
            }

        }
    }

    private String getGroupIdFor(@NotNull final P4LocalFile file) {
        switch (file.getFileAction()) {
            case ADD:
            case ADD_EDIT:
                return FileGroup.LOCALLY_ADDED_ID;

            case REOPEN:
            case EDIT:
                return FileGroup.MODIFIED_ID;

            case MOVE_ADD_EDIT:
            case MOVE_ADD:
                return FileGroup.LOCALLY_ADDED_ID;

            case EDIT_RESOLVED:
                return FileGroup.MERGED_ID;

            case INTEGRATE:
                return FileGroup.MERGED_ID;

            case DELETE:
            case MOVE_DELETE:
                return FileGroup.LOCALLY_REMOVED_ID;

            case REVERTED:
                return FileGroup.RESTORED_ID;

            case MOVE_EDIT:
                return FileGroup.MERGED_ID;

            case NONE:
                return FileGroup.UPDATED_ID;

            case UNKNOWN:
            default:
                return FileGroup.UNKNOWN_ID;
        }
    }


    @Nullable
    @Override
    public Configurable createConfigurable(Collection<FilePath> collection) {
        // Note the way this is currently implemented: synchronize options will always be reset.
        options = SyncOptions.createDefaultSyncOptions();

        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return new SyncOptionConfigurable(project, options, Collections.emptyList());
        }
        Map<P4ServerName, ServerConfig> configMap = new HashMap<>();
        collection.forEach((fp) -> {
            ClientConfigRoot clientRoot = registry.getClientFor(fp);
            if (clientRoot != null) {
                configMap.put(clientRoot.getServerConfig().getServerName(), clientRoot.getServerConfig());
            }
        });
        return new SyncOptionConfigurable(project, options, configMap.values());
    }

    @Override
    public boolean validateOptions(Collection<FilePath> collection) {
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return false;
        }
        for (FilePath filePath : collection) {
            ClientConfigRoot root = registry.getClientFor(filePath);
            if (root != null) {
                return true;
            }
        }
        return false;
    }

    private Map<String, FileGroup> collateByFileGroupId(final List<FileGroup> groups, Map<String, FileGroup> sorted) {
        if (sorted == null) {
            sorted = new HashMap<>();
        }

        for (FileGroup group : groups) {
            sorted.put(group.getId(), group);
            sorted = collateByFileGroupId(group.getChildren(), sorted);
        }

        return sorted;
    }

    static class SyncUpdateSession implements UpdateSession {
        private boolean cancelled = false;
        private List<VcsException> exceptions = new ArrayList<>();

        @NotNull
        @Override
        public List<VcsException> getExceptions() {
            return exceptions;
        }

        @Override
        public void onRefreshFilesCompleted() {
            // TODO if any cache needs update, call it from here.
        }

        @Override
        public boolean isCanceled() {
            return cancelled;
        }
    }
}
