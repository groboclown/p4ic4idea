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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.async.BlockingAnswer;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4.server.impl.util.ErrorCollectors;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class P4RollbackEnvironment implements RollbackEnvironment {
    private static final Logger LOG = Logger.getInstance(P4RollbackEnvironment.class);

    private final Project project;

    P4RollbackEnvironment(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
    }

    @Override
    public String getRollbackOperationName() {
        return P4Bundle.message("rollback.action.name");
    }

    @Override
    public void rollbackChanges(List<Change> changes, List<VcsException> vcsExceptions, @NotNull RollbackProgressListener listener) {
        if (changes == null || changes.isEmpty() || project.isDisposed()) {
            return;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return;
        }
        if (ApplicationManager.getApplication().isDispatchThread()) {
            vcsExceptions.add(new VcsException("Can only be run in a background thread"));
            return;
        }

        Set<FilePath> paths = new HashSet<>();
        for (Change change: changes) {
            if (change != null) {
                if (change.getAfterRevision() != null) {
                    paths.add(change.getAfterRevision().getFile());
                }
                if (change.getBeforeRevision() != null) {
                    paths.add(change.getBeforeRevision().getFile());
                }
            }
        }
        if (paths.isEmpty()) {
            return;
        }

        List<VirtualFile> needsRefresh = new ArrayList<>();
        try {
            BlockingAnswer.createBlockFor(paths.stream()
                    .map((f) -> Pair.create(registry.getClientFor(f), f))
                    .filter((p) -> p.first != null)
                    .map((p) -> P4ServerComponent
                            .perform(project, p.first.getClientConfig(), new RevertFileAction(p.second, false))
                            .whenCompleted((r) -> {
                                LOG.info("Reverted " + p.second);
                                listener.accept(p.second);
                                VirtualFile vf = p.second.getVirtualFile();
                                if (vf != null) {
                                    needsRefresh.add(vf);
                                }
                            })
                            .whenServerError((ex) -> listener.accept(p.second))
                            .whenOffline(() -> listener.accept(p.second))
                    )
                    .collect(ErrorCollectors.collectActionErrors(vcsExceptions)))
            .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO better exception?
            vcsExceptions.add(new VcsException(e));
        } catch (P4CommandRunner.ServerResultException e) {
            // This should never be reached, because of the collector.
            vcsExceptions.add(e);
        }

        LocalFileSystem lfs = LocalFileSystem.getInstance();
        lfs.refreshFiles(needsRefresh);

        // A refresh of the changes is sometimes needed.
        ChangeListManager.getInstance(project).scheduleUpdate(true);
    }

    @Override
    public void rollbackMissingFileDeletion(List<FilePath> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        forceSync(files, exceptions, listener);
    }

    @Override
    public void rollbackModifiedWithoutCheckout(List<VirtualFile> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        List<FilePath> paths = new ArrayList<>(files.size());
        for (VirtualFile vf: files) {
            paths.add(VcsUtil.getFilePath(vf));
        }
        forceSync(paths, exceptions, listener);
    }


    @Override
    public void rollbackIfUnchanged(VirtualFile file) {
        if (file == null || project.isDisposed()) {
            return;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            LOG.info("Skipping revert for " + file + ": plugin not in a valid state");
            return;
        }
        FilePath fp = VcsUtil.getFilePath(file);
        if (fp == null) {
            LOG.info("Skipping revert for " + file + ": no FilePath found");
            return;
        }
        ClientConfigRoot root = registry.getClientFor(file);
        if (root == null) {
            LOG.info("SKipping revert for " + file + ": no P4 root found");
            return;
        }

        P4ServerComponent
                .perform(project, root.getClientConfig(), new RevertFileAction(fp, true))
                .whenCompleted((r) -> ChangeListManager.getInstance(project).scheduleUpdate(true));
    }


    /**
     * Force a sync from server operation to overwrite local changes.
     * @param files files to sync
     * @param exceptions exceptions encountered
     * @param listener listener on progress
     */
    private void forceSync(List<FilePath> files, List<VcsException> exceptions, RollbackProgressListener listener) {
        if (files.isEmpty() || project.isDisposed()) {
            return;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            return;
        }

        files.stream().collect(Collectors.groupingBy(registry::getClientFor)).entrySet().stream()
                .map((Function<Map.Entry<ClientConfigRoot, List<FilePath>>, P4CommandRunner.ActionAnswer<FetchFilesResult>>) e -> {
                    if (e.getKey() == null) {
                        LOG.info("Skipping revert for " + e.getValue() + ": not in a Perforce root");
                        return new DoneActionAnswer<>(null);
                    }
                    return P4ServerComponent
                            .perform(project, e.getKey().getClientConfig(), new FetchFilesAction(e.getValue(), "", true))
                            .whenCompleted((c) -> listener.accept(e.getValue()))
                            .whenServerError((ex) -> listener.accept(e.getValue()))
                            .whenOffline(() -> listener.accept(e.getValue()));
                })
                .collect(ErrorCollectors.collectActionErrors(exceptions))
                .whenCompleted((c) -> LOG.info("Completed sync of files"))
                .whenFailed((c) -> LOG.info("Failed to sync files"));
    }
}
