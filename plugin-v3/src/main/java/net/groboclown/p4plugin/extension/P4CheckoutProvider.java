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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProviderEx;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import net.groboclown.p4.server.api.messagebus.ErrorEvent;
import net.groboclown.p4.server.api.messagebus.InternalErrorMessage;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.actions.BasicAction;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import net.groboclown.p4plugin.ui.sync.SyncProjectDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Synchronizes local client files with the server.
 */
public class P4CheckoutProvider extends CheckoutProviderEx {
    private static final Logger LOG = Logger.getInstance(P4CheckoutProvider.class);

    /**
     * Overloads CheckoutProvider#doCheckout(Project, Listener) to provide predefined repository URL.
     * <p>
     * This is expected to run in the current thread.
     */
    @Override
    public void doCheckout(@NotNull Project project, @Nullable final Listener listener,
            @Nullable String predefinedRepositoryUrl) {
        BasicAction.saveAll();

        SyncProjectDialog syncDialog = new SyncProjectDialog(project);
        final ClientConfig clientConfig = syncDialog.showAndGet();
        if (clientConfig == null) {
            return;
        }
        final VirtualFile rootDir = syncDialog.getDirectory();
        if (rootDir == null) {
            return;
        }
        final FilePath rootPath = VcsUtil.getFilePath(rootDir);

        // Because the listener behavior MUST run in a write context, we can't run this in the
        // specialized EdtSinkProcessor.
        new Task.Backgroundable(project,
                P4Bundle.getString("checkout.config.process")) {
            final Object sync = new Object();
            FetchFilesResult res;

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);
                progressIndicator.startNonCancelableSection();
                try {
                    LOG.info("Fetching files into " + rootPath);
                    FetchFilesResult r = P4ServerComponent
                            .perform(project, clientConfig,
                                    new FetchFilesAction(Collections.singletonList(rootPath), null, false))
                            .blockingGet(UserProjectPreferences.getLockWaitTimeoutMillis(project),
                                    TimeUnit.MILLISECONDS);
                    progressIndicator.finishNonCancelableSection();
                    synchronized (sync) {
                        res = r;
                    }
                } catch (InterruptedException e) {
                    InternalErrorMessage.send(project).cacheLockTimeoutError(new ErrorEvent<>(
                            new VcsInterruptedException(e)));
                    progressIndicator.finishNonCancelableSection();
                    onCancel();
                } catch (P4CommandRunner.ServerResultException e) {
                    progressIndicator.finishNonCancelableSection();
                    VcsNotifier.getInstance(project).notifyError(P4Bundle.getString("checkout.config.error.title"),
                            e.getMessage());
                    synchronized (sync) {
                        res = null;
                    }
                }
                progressIndicator.stop();
            }

            @Override
            public void onSuccess() {
                FetchFilesResult r;
                synchronized (sync) {
                    r = res;
                }
                if (r == null) {
                    return;
                }
                // Sync does not mark the directory as dirty.  Otherwise, when the refresh is completed,
                // it would need to run VcsDirtyScopeManager.getInstance(project).fileDirty(rootPath);
                rootDir.refresh(true, true, null);
                if (listener != null) {
                    listener.directoryCheckedOut(rootPath.getIOFile(), P4Vcs.getKey());
                    listener.checkoutCompleted();
                }
            }
        }.queue();
    }

    @Override
    public void doCheckout(@NotNull Project project, @Nullable Listener listener) {
        doCheckout(project, listener, null);
    }

    /**
     * @return a short unique identifier such as git, hg, svn, and so on
     */
    @NotNull
    @Override
    public String getVcsId() {
        return P4VcsKey.VCS_NAME;
    }

    @Override
    public String getVcsName() {
        return P4Bundle.getString("p4ic.scm.name");
    }
}
