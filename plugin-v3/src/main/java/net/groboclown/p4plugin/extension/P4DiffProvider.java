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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.diff.DiffMixin;
import com.intellij.openapi.vcs.diff.DiffProviderEx;
import com.intellij.openapi.vcs.diff.ItemLatestState;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionDescription;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.HistoryMessageFormatter;
import net.groboclown.p4.server.api.commands.file.ListFileHistoryQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsQuery;
import net.groboclown.p4.server.api.commands.file.ListFilesDetailsResult;
import net.groboclown.p4.server.api.values.P4FileAction;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.messages.HistoryMessageFormatterImpl;
import net.groboclown.p4plugin.util.HistoryContentLoaderImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class P4DiffProvider extends DiffProviderEx
        implements DiffMixin {
    private static final Logger LOG = Logger.getInstance(P4DiffProvider.class);

    private final Project project;
    private final HistoryMessageFormatter formatter;
    private final HistoryContentLoader loader;

    P4DiffProvider(Project project) {
        this.project = project;
        this.loader = new HistoryContentLoaderImpl(project);
        this.formatter = new HistoryMessageFormatterImpl();
    }

    @Nullable
    @Override
    public VcsRevisionNumber getCurrentRevision(VirtualFile file) {
        if (file.isDirectory()) {
            return null;
        }
        // TODO this needs to return the "have", not "head" revision
        ClientConfigRoot root = getRootFor(file);
        if (root == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not under perforce root: " + file);
            }
            return null;
        }
        FilePath fp = VcsUtil.getFilePath(file);
        if (fp == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not a known FilePath: " + file);
            }
            return null;
        }
        try {
            ListFilesDetailsResult result =
                    P4ServerComponent
                            .query(project, root.getClientConfig().getServerConfig(),
                                    new ListFilesDetailsQuery(root.getClientConfig().getClientServerRef(),
                                            Collections.singletonList(fp), ListFilesDetailsQuery.RevState.HAVE, 1))
                            .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            if (result.getFiles().isEmpty()) {
                LOG.info("No P4 file details found for " + file);
                return null;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Current revision for " + file + ": " + result.getFiles().get(0).getRevision());
            }
            return result.getFiles().get(0).getRevision();
        } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
            LOG.info(e);
            return null;
        }
    }

    @Nullable
    @Override
    public ItemLatestState getLastRevision(VirtualFile virtualFile) {
        return getLastRevision(VcsUtil.getFilePath(virtualFile));
    }

    /**
     * Get the current version of the file as it exists in the depot
     *
     * @param filePath file to fetch a revision
     * @return state of the file
     */
    @Nullable
    @Override
    public ItemLatestState getLastRevision(FilePath filePath) {
        if (filePath.isDirectory()) {
            return null;
        }
        ClientConfigRoot root = getRootFor(filePath);
        if (root == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not under perforce root: " + filePath);
            }
            return null;
        }
        try {
            ListFilesDetailsResult result = P4ServerComponent
                    .query(project, root.getClientConfig().getServerConfig(),
                            new ListFilesDetailsQuery(root.getClientConfig().getClientServerRef(),
                                    Collections.singletonList(filePath), ListFilesDetailsQuery.RevState.HEAD, 1))
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS);
            if (result.getFiles().isEmpty()) {
                LOG.info("No P4 file details found for " + filePath);
                return null;
            }
            P4FileRevision res = result.getFiles().get(0);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Last revision for " + filePath +": " + res);
            }
            return new ItemLatestState(res.getRevisionNumber(),
                    res.getFileAction() != P4FileAction.DELETE && res.getFileAction() != P4FileAction.MOVE_DELETE,
                    true);
        } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
            LOG.info(e);
            return null;
        }
    }

    @Nullable
    @Override
    public ContentRevision createFileContent(final VcsRevisionNumber revisionNumber, VirtualFile selectedFile) {
        FilePath local = VcsUtil.getFilePath(selectedFile);
        final ClientConfigRoot root = getRootFor(local);
        if (root == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not under perforce root: " + selectedFile);
            }
            return null;
        }
        String clientName = root.getClientConfig().getClientname();
        if (clientName == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No client for perforce file: " + selectedFile);
            }
            return null;
        }
        final VcsRevisionNumber.Int iRev;
        if (revisionNumber instanceof VcsRevisionNumber.Int) {
            iRev = (VcsRevisionNumber.Int) revisionNumber;
        } else {
            iRev = new VcsRevisionNumber.Int(IFileSpec.HEAD_REVISION);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generating file content for " + selectedFile + " rev " + iRev);
        }
        return new ContentRevision() {
            @Nullable
            @Override
            public String getContent()
                    throws VcsException {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Loading file content for " + local + " rev " + iRev);
                    }
                    return loader.loadStringContentForLocal(
                            root.getClientConfig().getServerConfig(),
                            clientName,
                            local,
                            iRev.getValue());
                } catch (IOException e) {
                    LOG.info("Problem loading file content for " + local + " rev " + iRev, e);
                    throw new VcsException(e);
                }
            }

            @NotNull
            @Override
            public FilePath getFile() {
                return local;
            }

            @NotNull
            @Override
            public VcsRevisionNumber getRevisionNumber() {
                return revisionNumber;
            }
        };
    }

    @Nullable
    @Override
    public VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot) {
        // Doesn't really mean anything in Perforce.  It stores changelists, not revisions.
        if (LOG.isDebugEnabled()) {
            LOG.debug("Call to getLatestCommittedRevision not valid for perforce (file " + vcsRoot + ")");
        }
        return null;
    }

    @Nullable
    @Override
    public VcsRevisionDescription getCurrentRevisionDescription(VirtualFile file) {
        FilePath local = VcsUtil.getFilePath(file);
        final ClientConfigRoot root = getRootFor(local);
        if (root == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Not under perforce root: " + file);
            }
            return null;
        }
        String clientName = root.getClientConfig().getClientname();
        if (clientName == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No client for perforce file: " + file);
            }
            return null;
        }
        try {
            List<VcsFileRevision> revisions = P4ServerComponent
                    .query(project, root.getClientConfig().getServerConfig(),
                            new ListFileHistoryQuery(root.getClientConfig().getClientServerRef(), local, 1))
                    .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS)
                    .getRevisions(formatter, loader);
            if (revisions.isEmpty()) {
                LOG.info("No revisions found for " + file);
                return null;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Current revision description for " + file + ": " + revisions.get(0));
            }
            return revisions.get(0);
        } catch (InterruptedException | P4CommandRunner.ServerResultException e) {
            // TODO better exception?
            LOG.info(e);
            return null;
        }
    }


    @Nullable
    private ClientConfigRoot getRootFor(VirtualFile f) {
        if (f == null) {
            return null;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return null;
        }
        return registry.getClientFor(f);
    }

    @Nullable
    private ClientConfigRoot getRootFor(FilePath f) {
        if (f == null) {
            return null;
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            return null;
        }
        return registry.getClientFor(f);
    }
}
