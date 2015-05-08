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
package net.groboclown.idea.p4ic.server.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * Moves files from the source changelist to the target changelist.
 * If the source changelist is {@link IChangelist#UNKNOWN}, then
 * it will move all the files, regardless of their origin changelist,
 * into the destination.  If the source changelist is known, then the
 * affected file list can be empty, which means move all the changes
 * from the source changelist into the target.
 */
public class MoveFilesBetweenChangelistsTask extends ServerTask<List<P4StatusMessage>> {
    private final Project project;
    private final int target;
    private final List<FilePath> files;
    private final FileInfoCache fileInfoCache;

    public MoveFilesBetweenChangelistsTask(
            @NotNull Project project, int target,
            @NotNull List<FilePath> files,
            @NotNull FileInfoCache fileInfoCache) {
        this.project = project;
        if (target <= 0) {
            target = IChangelist.DEFAULT;
        }
        this.target = target;
        this.files = files;
        this.fileInfoCache = fileInfoCache;
    }

    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        log("moving to change " + target);

        List<P4FileInfo> expectedFiles = exec.loadFileInfo(project, FileSpecUtil.getFromFilePaths(files), fileInfoCache);

        // If a moved/renamed file is moved into another changelist, move
        // its pair there, too.
        List<IFileSpec> openedPairs = new ArrayList<IFileSpec>();
        for (P4FileInfo file: expectedFiles) {
            IFileSpec other = file.getOpenedPair();
            if (other != null) {
                openedPairs.add(other);
            }
        }
        if (! openedPairs.isEmpty()) {
            expectedFiles.addAll(exec.loadFileInfo(project, openedPairs, fileInfoCache));
        }

        Iterator<P4FileInfo> infoIter = expectedFiles.iterator();
        while (infoIter.hasNext()) {
            P4FileInfo file = infoIter.next();
            if (! file.isOpenInClient() || file.getChangelist() == target) {
                log("Skipping " + file + ": either not open in client, or is already in the correct changelist");
                infoIter.remove();
            } else {
                log("Allowing movement of " + file + "@" + file.getChangelist() + "; opened with " + file.getClientAction());
            }
        }
        if (expectedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // Ensure we have the correct target changelist, and verify that
        // it's in the correct state.
        IChangelist targetCl = exec.getChangelist(project, target);
        if (targetCl == null || targetCl.getStatus() == ChangelistStatus.SUBMITTED) {
            // nothing to do
            return Collections.emptyList();
        }

        // Check the source changelist files.
        final List<IFileSpec> sourceFiles = P4FileInfo.toClientList(expectedFiles);

        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();
        if (sourceFiles != null && ! sourceFiles.isEmpty()) {
            // need to move the files into the default changelist.
            log("Reopening files in changelist " + targetCl.getId());
            ret.addAll(exec.reopenFiles(project, sourceFiles, targetCl.getId(), null));
        }

        return ret;
    }
}
