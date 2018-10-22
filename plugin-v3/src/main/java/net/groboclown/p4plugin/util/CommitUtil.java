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
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;
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

public class CommitUtil {
    private static final Logger LOG = Logger.getInstance(CommitUtil.class);


    @NotNull
    public static List<VcsException> commit(@NotNull Project project, @NotNull List<Change> changes,
            @NotNull String preparedComment,
            @NotNull List<P4Job> jobs, @Nullable JobStatus submitStatus) {
        // Need to split up the files and jobs by server.
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null || registry.isDisposed()) {
            // TODO bundle message
            return Collections.singletonList(new VcsException("System not ready to run"));
        }

        Map<ClientConfigRoot, List<FilePath>> filesByRoot = mapChangedFilesByRoot(registry, changes);
        List<VcsException> errors = new ArrayList<>();

        try {
            boolean completed = filesByRoot.entrySet().stream()
                    .map((entry) -> new Pair<>(entry.getKey(), getFileChangelists(project,
                            entry.getKey().getClientConfig(), entry.getValue())))
                    .map((pair) -> getJobsIn(project, pair.first.getServerConfig(), jobs)
                            .mapActionAsync((associatedJobs) -> {
                                // TODO have a better message report.
                                P4CommandRunner.ActionAnswer<SubmitChangelistResult> ret =
                                        new DoneActionAnswer<>(null);
                                for (P4ChangelistId changelistId : pair.second) {
                                    ret = ret.mapActionAsync((r) -> P4ServerComponent
                                            .perform(project, pair.first.getClientConfig(),
                                                    new SubmitChangelistAction(changelistId,
                                                            associatedJobs, preparedComment, submitStatus)));
                                }
                                return ret;
                            }))
                    .collect(ErrorCollectors.collectActionErrors(errors))
                    .blockingWait(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            if (!completed) {
                errors.add(new VcsException("Failed to wait for submit to complete"));
            }
        } catch (InterruptedException e) {
            errors.add(new VcsException(e));
        }

        return errors;
    }


    @NotNull
    public static Map<ClientConfigRoot, List<FilePath>> mapChangedFilesByRoot(@NotNull ProjectConfigRegistry registry,
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


    @NotNull
    public static List<P4ChangelistId> getFileChangelists(Project project, ClientConfig config,
            Collection<FilePath> files) {
        List<P4ChangelistId> ret = new ArrayList<>();
        Pair<IdeChangelistMap, IdeFileMap>
                cachePair = CacheComponent.getInstance(project).getServerOpenedCache();
        for (FilePath file : files) {
            P4LocalFile p4File = cachePair.second.forIdeFile(file);
            if (p4File != null) {
                P4ChangelistId change = p4File.getChangelistId();
                if (change != null) {
                    ret.add(change);
                } else {
                    LOG.warn("No perforce change associated with file " + file);
                }
            } else {
                LOG.warn("No perforce file associated with " + file);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Associated changes " + ret + " to files " + files);
        }
        return ret;
    }


    private static P4CommandRunner.QueryAnswer<Set<P4Job>> getJobsIn(@NotNull Project project,
            @NotNull ServerConfig config, List<P4Job> jobs) {
        P4CommandRunner.QueryAnswer<Set<P4Job>> ret = new DoneQueryAnswer<>(new HashSet<>());
        for (P4Job job : jobs) {
            ret = ret.mapQueryAsync((associatedJobs) -> P4ServerComponent
                    .query(project, config, new ListJobsQuery(job.getJobId(), null, null, 1))
                    .mapQuery((result) -> {
                        associatedJobs.addAll(result.getJobs());
                        return associatedJobs;
                    }));
        }
        return ret;
    }

}
