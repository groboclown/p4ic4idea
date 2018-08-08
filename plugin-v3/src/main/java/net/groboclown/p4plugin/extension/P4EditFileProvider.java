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
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.ProjectConfigRegistry;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.api.values.P4FileType;
import net.groboclown.p4.server.impl.util.DispatchActions;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.CacheComponent;
import net.groboclown.p4plugin.components.P4ServerComponent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This is only called when the file is changed from
 * read-only to writable.
 */
public class P4EditFileProvider implements EditFileProvider {
    private static final Logger LOG = Logger.getInstance(P4EditFileProvider.class);

    private final Project project;

    P4EditFileProvider(@NotNull P4Vcs vcs) {
        this.project = vcs.getProject();
    }


    // This method is called with nearly every keystroke, so it must be very, very
    // performant.
    @Override
    public void editFiles(final VirtualFile[] allFiles) throws VcsException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Calling `editFiles` with " + Arrays.asList(allFiles));
        }
        if (allFiles == null || allFiles.length <= 0) {
            return;
        }

        // In order to speed up the operation of this call, we will not care who
        // has this open for edit or not.  Make the file writable, then pass on
        // the actual server edit checks to a background thread.

        // TODO This is ignoring the user property to edit in another thread.

        makeWritable(allFiles);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Map<ClientServerRef, P4ChangelistId> activeChangelistIds = getActiveChangelistIds();
            for (VirtualFile file : allFiles) {
                FilePath fp = VcsUtil.getFilePath(file);
                if (fp == null || !file.isInLocalFileSystem()) {
                    continue;
                }
                ClientConfigRoot root = getClientFor(file);
                if (root != null) {
                    P4ChangelistId id = getActiveChangelistFor(root, activeChangelistIds);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Opening for add/edit: " + fp + " (@" + id + ")");
                    }
                    P4ServerComponent
                            .perform(project, root.getClientConfig(),
                                    new AddEditAction(fp, getFileType(fp), id, (String) null))
                    .whenCompleted((res) -> {
                        LOG.info("Opened for add/edit: " + fp + ": add? " + res.isAdd() + "; cl: " + res.getChangelistId());
                    })
                    .whenOffline(() -> {
                        LOG.warn("Queued add/edit action for " + fp + "; server offline");
                    })
                    .whenServerError((e) -> {
                        LOG.warn("Could not open for add", e);
                    });
                } else {
                    LOG.info("Not under Perforce VCS root: " + file);
                }
            }
        });
    }

    @Override
    public String getRequestText() {
        // TODO verify where this is used, and whether the text makes sense in the context.
        return P4Bundle.getString("file.open-for-edit");
    }

    private void makeWritable(@NotNull final VirtualFile[] allFiles) {
        DispatchActions.writeAction(() -> {
            for (VirtualFile file : allFiles) {
                if (file.isInLocalFileSystem()) {
                    try {
                        file.setWritable(true);
                    } catch (IOException e) {
                        handleError(e);
                    }
                }
            }
        });
    }

    private Map<ClientServerRef, P4ChangelistId> getActiveChangelistIds() {
        LocalChangeList defaultIdeChangeList =
                ChangeListManager.getInstance(project).getDefaultChangeList();
        Map<ClientServerRef, P4ChangelistId> ret = new HashMap<>();
        try {
            CacheComponent.getInstance(project).getServerOpenedCache().first
                    .getP4ChangesFor(defaultIdeChangeList)
                    .forEach((id) -> ret.put(id.getClientServerRef(), id));
        } catch (InterruptedException e) {
            LOG.warn(e);
        }
        return ret;
    }

    @NotNull
    private P4ChangelistId getActiveChangelistFor(ClientConfigRoot root, Map<ClientServerRef, P4ChangelistId> ids) {
        ClientServerRef ref = root.getClientConfig().getClientServerRef();
        P4ChangelistId ret = ids.get(ref);
        if (ret == null) {
            ret = P4ChangelistIdImpl.createDefaultChangelistId(ref);
            ids.put(ref, ret);
        }
        return ret;
    }

    private P4FileType getFileType(FilePath fp) {
        FileType ft = fp.getFileType();
        if (ft.isBinary()) {
            return P4FileType.convert("binary");
        }
        return P4FileType.convert("text");
    }

    private void handleError(IOException e) {
        // FIXME
        LOG.warn("FIXME implement error handler", e);
    }


    private ClientConfigRoot getClientFor(VirtualFile file) {
        ProjectConfigRegistry reg = ProjectConfigRegistry.getInstance(project);
        return reg == null ? null : reg.getClientFor(file);
    }
}
