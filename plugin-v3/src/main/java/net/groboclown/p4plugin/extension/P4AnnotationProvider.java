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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.AnnotateFileQuery;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.values.P4FileRevision;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.commands.HistoryMessageFormatter;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.messages.HistoryMessageFormatterImpl;
import net.groboclown.p4plugin.revision.P4AnnotatedFileImpl;
import net.groboclown.p4plugin.util.HistoryContentLoaderImpl;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class P4AnnotationProvider
        implements AnnotationProvider {
    private static final Logger LOG = Logger.getInstance(P4AnnotationProvider.class);

    private final Project project;
    private final HistoryMessageFormatter messageFormatter = new HistoryMessageFormatterImpl();
    private final HistoryContentLoader contentLoader;

    P4AnnotationProvider(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
        this.contentLoader = new HistoryContentLoaderImpl(project);
    }

    @NotNull
    @Override
    public FileAnnotation annotate(@NotNull VirtualFile file) throws VcsException {
        return annotate(file, null);
    }

    @NotNull
    @Override
    public FileAnnotation annotate(@NotNull VirtualFile file, VcsFileRevision revision) throws VcsException {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            LOG.info("Fetching annotation from the EDT");
            // TODO bundle for error messages
            throw new VcsException("Does not support fetching annotations from the EDT.");
        }
        // TODO use a better location for this constant.
        int rev = IFileSpec.HEAD_REVISION;
        if (revision != null) {
            VcsRevisionNumber revNumber = revision.getRevisionNumber();
            if (revNumber instanceof VcsRevisionNumber.Int) {
                rev = ((VcsRevisionNumber.Int) revNumber).getValue();
            } else {
                LOG.warn("Unknown file revision " + revision + " for " + file + "; using head revision");
            }
        }
        FilePath fp = VcsUtil.getFilePath(file);
        if (fp == null) {
            // TODO bundle for error messages
            throw new VcsException("No known Perforce server for " + file);
        }
        ProjectConfigRegistry registry = ProjectConfigRegistry.getInstance(project);
        if (registry == null) {
            // TODO bundle for error messages
            throw new VcsException("Project not configured for showing annotations");
        }
        ClientConfigRoot client = registry.getClientFor(file);
        if (client == null) {
            // TODO bundle for error messages
            throw new VcsException("No known Perforce server for " + file);
        }
        String clientname = client.getClientConfig().getClientname();
        if (clientname == null) {
            // TODO bundle for error messages
            throw new VcsException("No workspace name set for Perforce connection for " + file);
        }
        try {
            return new P4AnnotatedFileImpl(project, fp,
                    messageFormatter, contentLoader,
                    P4ServerComponent
                        .query(project, client.getClientConfig(), new AnnotateFileQuery(fp, rev))
                        .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }

    /**
     * Check whether the annotation retrieval is valid (or possible) for the
     * particular file revision (or version in the repository).
     *
     * @param rev File revision to be checked.
     * @return true if annotation it valid for the given revision.
     */
    @Override
    public boolean isAnnotationValid(@NotNull VcsFileRevision rev) {
        return (rev instanceof P4FileRevision);
    }
}
