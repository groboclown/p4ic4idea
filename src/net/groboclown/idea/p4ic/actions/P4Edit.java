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
package net.groboclown.idea.p4ic.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsFileUtil;
import net.groboclown.idea.p4ic.background.Background;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.SubProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class P4Edit extends BasicAction {
    private static final Logger LOG = Logger.getInstance(P4Edit.class);

    public static final String EDIT = "Edit files";


    @NotNull
    @Override
    protected String getActionName() {
        return EDIT;
    }

    @Override
    protected boolean isEnabled(@NotNull Project project, @NotNull P4Vcs vcs, @NotNull VirtualFile... vFiles) {
        Map<Client, List<VirtualFile>> clients;
        try {
            clients = vcs.mapVirtualFilesToClient(Arrays.asList(vFiles));
        } catch (P4InvalidConfigException e) {
            return false;
        }
        for (Client client: clients.keySet()) {
            if (client.isWorkingOffline()) {
                return false;
            }
        }
        return true;
    }


    @Override
    protected void perform(@NotNull final Project project, @NotNull final P4Vcs vcs, @NotNull final List<VcsException> exceptions, @NotNull final List<VirtualFile> affectedFiles) {
        if (affectedFiles.isEmpty()) {
            return;
        }

        final Map<Client, List<VirtualFile>> clients;
        try {
            clients = vcs.mapVirtualFilesToClient(affectedFiles);
        } catch (P4InvalidConfigException e) {
            exceptions.add(e);
            return;
        }


        FileDocumentManager.getInstance().saveAllDocuments();

        LOG.info("adding or editing files: " + affectedFiles);

        // FIXME this may be bad form - it probably needs to run in-process.
        Background.runInBackground(project, EDIT, vcs.getConfiguration().getEditOption(), new Background.ER() {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                double clientIndex = 0.0;
                for (Map.Entry<Client, List<VirtualFile>> en: clients.entrySet()) {
                    final Client client = en.getKey();
                    List<VirtualFile> files = en.getValue();
                    SubProgressIndicator sub = new SubProgressIndicator(indicator,
                            0.9 * clientIndex / (double) clients.size(),
                            0.9 * (clientIndex + 1.0) / (double) clients.size());
                    sub.setFraction(0.0);
                    int changelistId = vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client).getChangeListId();
                    indicator.setFraction(0.2);
                    //System.out.println("project default changelist: " + changelistId);
                    List<P4StatusMessage> messages = null;
                    try {
                        messages = client.getServer().editFiles(files, changelistId);
                        indicator.setFraction(0.8);
                        P4StatusMessage.throwIfError(messages, true);
                        indicator.setFraction(0.85);

                        // Move the file into the correct changelist.
                        //ChangesViewI cvm = ChangesViewManager.getInstance(getProject());
                        //for (VirtualFile file: selected) {
                        //    LOG.info("Marking " + file + " as needing change refresh");
                        //    cvm.refreshChangesViewNodeAsync(file);
                        //}
                        VcsDirtyScopeManager.getInstance(project).filesDirty(files, null);
                    } catch (VcsException e) {
                        exceptions.add(e);
                    }

                    clientIndex += 1.0;
                }
                indicator.setFraction(0.9);
                VcsFileUtil.refreshFiles(project, affectedFiles);
            }
        });
    }
}
