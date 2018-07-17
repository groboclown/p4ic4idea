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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProviderEx;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.P4VcsKey;
import net.groboclown.p4.server.api.async.Answer;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesResult;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.impl.commands.AnswerUtil;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.actions.BasicAction;
import net.groboclown.p4plugin.components.P4ServerComponent;
import net.groboclown.p4plugin.ui.DummyProgressIndicator;
import net.groboclown.p4plugin.ui.EdtSinkProcessor;
import net.groboclown.p4plugin.ui.sync.SyncProjectDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class P4CheckoutProvider extends CheckoutProviderEx {
    private static final Logger LOG = Logger.getInstance(P4CheckoutProvider.class);

    /**
     * Overloads CheckoutProvider#doCheckout(Project, Listener) to provide predefined repository URL
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
        final ProgressIndicator progressIndicator =
                DummyProgressIndicator.nullSafe(ProgressManager.getInstance().getProgressIndicator());
        progressIndicator.setIndeterminate(true);
        progressIndicator.start();
        EdtSinkProcessor<FetchFilesResult> processor = new EdtSinkProcessor<>();
        processor.addConsumer((res) -> {
            progressIndicator.stop();
            rootDir.refresh(true, true, () -> {
                if (project.isOpen() && !project.isDisposed() && !project.isDefault()) {
                    VcsDirtyScopeManager.getInstance(project).fileDirty(rootDir);
                }
            });
            if (listener != null) {
                listener.directoryCheckedOut(rootPath.getIOFile(), P4Vcs.getKey());
                listener.checkoutCompleted();
            }
        });
        processor.addErrorHandler((e) -> {
            VcsNotifier.getInstance(project).notifyError(P4Bundle.getString("checkout.config.error.title"),
                    e.getMessage());
        });
        LOG.info("Fetching tiles into " + rootPath);
        processor.processBatchAnswer(() -> Answer.background(sink ->
            P4ServerComponent.getInstance(project).getCommandRunner()
                .perform(clientConfig,
                        new FetchFilesAction(Collections.singletonList(rootPath), null, false))
                .whenCompleted(r -> sink.resolve(Collections.singletonList(r)))
                .whenServerError(sink::reject)
                .whenOffline(() -> sink.reject(AnswerUtil.createOfflineError()))), true);
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
