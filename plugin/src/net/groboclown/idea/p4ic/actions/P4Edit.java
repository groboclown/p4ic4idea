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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.compat.VcsCompat;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.v2.server.P4Server;
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
        // If an input file does not map to a server, then we still report enabled.  If all the files only map to
        // non-P4 files, then disable.

        LOG.info("Checking enabled state for files " + Arrays.asList(vFiles));

        boolean mapsToServer = false;
        final Map<P4Server, List<VirtualFile>> servers =
                vcs.mapVirtualFilesToP4Server(Arrays.asList(vFiles));
        for (P4Server server : servers.keySet()) {
            if (server != null) {
                if (server.isWorkingOffline()) {
                    LOG.info("Server working offline: " + server);
                    return false;
                }
                mapsToServer = true;
            }
        }
        return mapsToServer;
    }


    @Override
    protected void perform(@NotNull final Project project, @NotNull final P4Vcs vcs, @NotNull final List<VcsException> exceptions, @NotNull final List<VirtualFile> affectedFiles) {
        if (affectedFiles.isEmpty()) {
            return;
        }

        final Map<P4Server, List<VirtualFile>> servers = vcs.mapVirtualFilesToP4Server(affectedFiles);

        FileDocumentManager.getInstance().saveAllDocuments();

        LOG.info("adding or editing files: " + affectedFiles);

        //double clientIndex = 0.0;
        for (Map.Entry<P4Server, List<VirtualFile>> en: servers.entrySet()) {
            final P4Server server = en.getKey();
            List<VirtualFile> files = en.getValue();
            //SubProgressIndicator sub = new SubProgressIndicator(indicator,
            //        0.9 * clientIndex / (double) clients.size(),
            //        0.9 * (clientIndex + 1.0) / (double) clients.size());
            //sub.setFraction(0.0);
            int changelistId = vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(server).getChangeListId();
            //indicator.setFraction(0.2);
            server.addOrEditFiles(files, changelistId);
            VcsDirtyScopeManager.getInstance(project).filesDirty(files, null);

            //clientIndex += 1.0;
        }
        //indicator.setFraction(0.9);
        // No longer supported in IntelliJ 15
        VcsCompat.getInstance().refreshFiles(project, affectedFiles);
    }
}
