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
package net.groboclown.idea.p4ic.v2.file;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.actions.BasicAction;
import net.groboclown.idea.p4ic.compat.VcsCompat;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import net.groboclown.idea.p4ic.v2.changes.P4ChangeListMapping;
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

        try {
            final Map<P4Server, List<VirtualFile>> servers = vcs.mapVirtualFilesToP4Server(Arrays.asList(vFiles));

            // all we care about for open for edit/add is whether
            // the files map to servers or not.  Online or offline
            // modes don't matter.
            return !(servers.isEmpty() || (servers.size() == 1 && servers.containsKey(null)));
        } catch (InterruptedException e) {
            LOG.info(e);
            return false;
        }
    }



    @Override
    protected void perform(@NotNull final Project project, @NotNull final P4Vcs vcs,
            @NotNull final List<VcsException> exceptions, @NotNull final List<VirtualFile> affectedFiles) {
        if (affectedFiles.isEmpty()) {
            return;
        }

        final Map<P4Server, List<VirtualFile>> servers;
        try {
            servers = vcs.mapVirtualFilesToP4Server(affectedFiles);
        } catch (InterruptedException e) {
            exceptions.add(new P4Exception(e));
            return;
        }

        FileDocumentManager.getInstance().saveAllDocuments();

        P4ChangeListMapping changeListMapping = P4ChangeListMapping.getInstance(project);

        LOG.info("adding or editing files: " + affectedFiles);

        for (Map.Entry<P4Server, List<VirtualFile>> en: servers.entrySet()) {
            final P4Server server = en.getKey();
            List<VirtualFile> files = en.getValue();
            int changelistId = changeListMapping.getProjectDefaultPerforceChangelist(server).getChangeListId();
            server.addOrEditFiles(files, changelistId);
            VcsDirtyScopeManager.getInstance(project).filesDirty(files, null);
        }
        // No longer supported in IntelliJ 15
        VcsCompat.getInstance().refreshFiles(project, affectedFiles);
    }
}
