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

package net.groboclown.p4plugin.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.commands.changelist.ListJobsQuery;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.OptionalClientServerConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4.server.api.values.JobStatus;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4LocalFile;
import net.groboclown.p4.server.impl.commands.DoneActionAnswer;
import net.groboclown.p4.server.impl.commands.DoneQueryAnswer;
import net.groboclown.p4.server.impl.util.ErrorCollectors;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommitUtil {
    private static final Logger LOG = Logger.getInstance(CommitUtil.class);


    @NotNull
    public static List<VcsException> commit(@NotNull Project project, @NotNull List<Change> changes,
            @NotNull String preparedComment,
            @NotNull List<P4Job> jobs, @Nullable JobStatus submitStatus) {
        // See #52
        if (preparedComment.trim().isEmpty()) {
            // TODO bundle message
            return Collections.singletonList(new VcsException("Must provide a comment on submit"));
        }

        // Need to split up the files and jobs by server.
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            // TODO bundle message
            return Collections.singletonList(new VcsException("System not ready to run"));
        }

        Map<ClientConfigRoot, List<FilePath>> filesByRoot = mapChangedFilesByRoot(registry, changes);
        List<VcsException> errors = new ArrayList<>();

        // This bit of code can incorrectly duplicate changelist items to submit.  Need to protect against that.
        Set<P4ChangelistId> submitted = new HashSet<>();

        try {
            boolean completed = filesByRoot.entrySet().stream()
                    .map(entry -> getJobsIn(project, entry.getKey(),entry.getValue(), jobs))
                    .flatMap(data -> getFileChangelists(project, data).stream())
                    .map(data -> data.answer
                            .mapActionAsync((associatedJobs) -> {
                                if (submitted.contains(data.changelistId)) {
                                    LOG.warn("Skipped double submit for " + data.changelistId);
                                    return new DoneActionAnswer<>(null);
                                } else {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("Requesting submit for " + data.changelistId);
                                    }
                                    submitted.add(data.changelistId);
                                    return P4ServerComponent
                                            .perform(project, data.root.getClientConfig(),
                                                    new SubmitChangelistAction(data.changelistId,
                                                            data.files,
                                                            associatedJobs, preparedComment, submitStatus));
                                }
                            })
                            .whenCompleted(res -> {
                                // Submit requires that the corresponding link between the changelist and the
                                // IDE change list be severed.  We do this by telling the cache that the P4 changelist
                                // was "deleted" (that is, no longer pending).
                                try {
                                    CacheComponent.getInstance(project)
                                            .getServerOpenedCache().first
                                            .changelistDeleted(res.getChangelistId());
                                } catch (InterruptedException e) {
                                    InternalErrorMessage.send(project).cacheLockTimeoutError(
                                            new ErrorEvent<>(new VcsInterruptedException(
                                                    "Timed out waiting for cache to become available", e)));
                                }
                            })
                    )
                    .collect(ErrorCollectors.collectActionErrors(errors))
                    .blockingWait(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            if (!completed) {
                errors.add(new VcsException("Failed to wait for submit to complete"));
            }
        } catch (InterruptedException e) {
            // Note: handled by IDE, not by our custom event model.
            errors.add(new VcsInterruptedException(e));
        }

        return errors;
    }


    @NotNull
    private static Map<ClientConfigRoot, List<FilePath>> mapChangedFilesByRoot(@NotNull ProjectConfigRegistry registry,
            @NotNull Collection<Change> changes) {
        Map<ClientConfigRoot, List<FilePath>> ret = new HashMap<>();
        for (Change change : changes) {
            {
                ContentRevision before = change.getBeforeRevision();
                if (before != null) {
                    ClientConfigRoot root = registry.getClientFor(before.getFile());
                    List<FilePath> paths = ret.computeIfAbsent(root, k -> new ArrayList<>());
                    paths.add(before.getFile());
                }
            }
            {
                ContentRevision after = change.getAfterRevision();
                if (after != null) {
                    ClientConfigRoot root = registry.getClientFor(after.getFile());
                    List<FilePath> paths = ret.computeIfAbsent(root, k -> new ArrayList<>());
                    paths.add(after.getFile());
                }
            }
        }
        return ret;
    }


    private static JobData getJobsIn(Project project, ClientConfigRoot root,
            List<FilePath> files, List<P4Job> jobs) {
        return new JobData(root, files, getJobsIn(project, root.getClientConfig(), jobs));
    }

    @NotNull
    private static List<SubmitChangelistData> getFileChangelists(Project project, JobData data) {
        Map<P4ChangelistId, List<FilePath>> ret = new HashMap<>();
        Pair<IdeChangelistMap, IdeFileMap>
                cachePair = CacheComponent.getInstance(project).getServerOpenedCache();
        List<P4LocalFile> noChangelistFiles = new ArrayList<>();
        for (FilePath file : data.files) {
            P4LocalFile p4File = cachePair.second.forIdeFile(file);
            if (p4File != null) {
                P4ChangelistId change = p4File.getChangelistId();
                if (change == null) {
                    noChangelistFiles.add(p4File);
                } else {
                    ret.computeIfAbsent(change, c -> new ArrayList<>()).add(file);
                }
            } else {
                LOG.warn("No cached perforce file associated with " + file);
            }
        }

        if (! noChangelistFiles.isEmpty()) {
            // This scenario should never happen. But if it does...
            // Should it just add/edit on the default changelist and submit with everything else?
            LOG.warn("No perforce change associated with files " + noChangelistFiles);
            InternalErrorMessage.send(project).p4ApiInternalError(new ErrorEvent<>(
                    new Exception("Attempted to submit files that weren't open for edit/add: " + noChangelistFiles)));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Associated changes " + ret + " to files " + data.files);
        }
        return ret.entrySet().stream()
                .map(e -> new SubmitChangelistData(data, e.getValue(), e.getKey()))
                .collect(Collectors.toList());
    }


    private static P4CommandRunner.QueryAnswer<Set<P4Job>> getJobsIn(@NotNull Project project,
            @NotNull ClientConfig config, List<P4Job> jobs) {
        P4CommandRunner.QueryAnswer<Set<P4Job>> ret = new DoneQueryAnswer<>(new HashSet<>());
        for (P4Job job : jobs) {
            ret = ret.mapQueryAsync((associatedJobs) -> P4ServerComponent
                    .query(
                            project, new OptionalClientServerConfig(config),
                            new ListJobsQuery(job.getJobId(), null, null, 1))
                    .mapQuery((result) -> {
                        associatedJobs.addAll(result.getJobs());
                        return associatedJobs;
                    }));
        }
        return ret;
    }


    static class SubmitChangelistData {
        final ClientConfigRoot root;
        final List<FilePath> files;
        final P4ChangelistId changelistId;
        final P4CommandRunner.QueryAnswer<Set<P4Job>> answer;

        SubmitChangelistData(@NotNull JobData data,
                @NotNull List<FilePath> files, @NotNull P4ChangelistId changelistId) {
            this.root = data.root;
            this.files = files;
            this.answer = data.jobAnswer;
            this.changelistId = changelistId;
        }
    }

    static class JobData {
        final ClientConfigRoot root;
        final List<FilePath> files;
        final P4CommandRunner.QueryAnswer<Set<P4Job>> jobAnswer;

        JobData(@NotNull ClientConfigRoot root, @NotNull List<FilePath> files,
                @NotNull P4CommandRunner.QueryAnswer<Set<P4Job>> jobAnswer) {
            this.root = root;
            this.files = files;
            this.jobAnswer = jobAnswer;
        }
    }
}
