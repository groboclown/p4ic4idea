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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.idea.p4ic.server.*;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CancellationException;

public class MoveRunner extends ServerTask<List<P4StatusMessage>> {
    private static final Logger LOG = Logger.getInstance(MoveRunner.class);

    private final Project project;
    private final Map<FilePath, FilePath> movedFiles;
    private final int destination;
    private final FileInfoCache fileInfoCache;

    public MoveRunner(Project project, @NotNull Map<FilePath, FilePath> movedFiles,
                      int destination, @NotNull FileInfoCache fileInfoCache) {
        this.project = project;
        this.movedFiles = movedFiles;
        this.destination = destination;
        this.fileInfoCache = fileInfoCache;
    }

    @NotNull
    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        int changelistId = -1;
        if (destination > 0) {
            changelistId = destination;
        }

        // The move command MUST handle these scenarios:
        // 1. Both files are in the client.
        //    If the target is open for edit (or add or delete), revert it.
        //    Open the source file for edit (or add), and perform a move.
        //    Hopefully, this will correctly open for edit the destination.
        // 2. The source is in the depot, but the target isn't.
        //    Possibly revert the source file, then perform a delete on it.
        // 3. The target is in the depot, but the source isn't.
        //    Open the file for edit.

        List<IFileSpec> reverted = new ArrayList<IFileSpec>();
        List<IFileSpec> added = new ArrayList<IFileSpec>();
        List<IFileSpec> deleted = new ArrayList<IFileSpec>();
        List<IFileSpec> edited = new ArrayList<IFileSpec>();
        List<MoveData> moved = new ArrayList<MoveData>();

        Set<FilePath> allFiles = new HashSet<FilePath>();
        allFiles.addAll(movedFiles.keySet());
        allFiles.addAll(movedFiles.values());
        Map<FilePath, P4FileInfo> allMappings = mapFilePathsToClient(allFiles, exec);


        Map<FilePath, P4FileInfo> clientMoveSource = sortMap(allMappings, movedFiles.keySet());
        Map<FilePath, P4FileInfo> clientMoveTarget = sortMap(allMappings, movedFiles.values());

        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();

        for (Map.Entry<FilePath, P4FileInfo> e : clientMoveSource.entrySet()) {
            P4FileInfo source = e.getValue();
            P4FileInfo target = clientMoveTarget.get(movedFiles.get(e.getKey()));
            LOG.info("Moving " + source + " to " + target);
            if (target.isInClientView()) {
                if (source.isInDepot() || source.isOpenInClient()) {
                    boolean moveClientFiles = true;

                    // According to the "move" usage, a file must be
                    // open for add or edit; if it's open for add, then
                    // it won't be in the depot but it will be open in
                    // the client.
                    if (source.isOpenForDelete()) {
                        // The source is open for delete, which isn't allowed.
                        LOG.debug("Move: revert delete on " + source);
                        reverted.add(target.toClientSpec());
                    } else
                    if (! source.isOpenInClient()) {
                        // The source must be open for edit or add.
                        LOG.debug("Move: open for edit " + source);
                        edited.add(source.toDepotSpec());
                    }

                    if (! source.getPath().getIOFile().exists()) {
                        // if the source file doesn't exist but the target does,
                        // then that's because IDEA already moved the file.
                        // We need to touch the source file so that Perforce
                        // won't fail, then tell the move operation to
                        // not touch the client files, because IDEA has
                        // already done that operation.
                        moveClientFiles = false;
                    }

                    // The target can't be already open.
                    if (target.isOpenInClient()) {
                        LOG.debug("Move: revert target " + target);
                        reverted.add(target.toClientSpec());
                    }

                    // Move the file
                    LOG.debug("Move: move " + source + " to " + target);
                    moved.add(new MoveData(source, target, moveClientFiles));
                } else {
                    // Source file is not in the depot or open in the client
                    if (target.isInDepot()) {
                        if (! target.isOpenInClient()) {
                            LOG.debug("Move: edit " + target);
                            edited.add(target.toDepotSpec());
                        } else if (target.isOpenForDelete()) {
                            LOG.debug("Move: revert for delete " + target);
                            reverted.add(target.toClientSpec());
                            edited.add(target.toDepotSpec());
                        } else {
                            LOG.debug("Move: no need to re-edit " + target);
                        }
                    } else if (! target.isOpenInClient()) {
                        LOG.debug("Move: add " + target);
                        added.add(target.toClientSpec());
                    } else {
                        LOG.debug("Move: no need to re-add " + target);
                    }
                }
            } else {
                // It's moved to outside the depot.  No need to inspect target.
                if (source.isOpenInClient()) {
                    // revert the file
                    LOG.debug("Move: revert add or edit " + source);
                    reverted.add(source.toClientSpec());
                }
                if (source.isInDepot()) {
                    LOG.debug("Move: delete " + source);
                    deleted.add(source.toDepotSpec());
                } else {
                    LOG.debug("Move: not in depot yet: " + source);
                }
            }
        }

        if (! reverted.isEmpty()) {
            ret.addAll(exec.revertFiles(project, reverted));
        }
        if (! added.isEmpty()) {
            ret.addAll(exec.addFiles(project, added, changelistId));
        }
        if (! deleted.isEmpty()) {
            ret.addAll(exec.deleteFiles(project, deleted, changelistId, true));
        }
        if (! edited.isEmpty()) {
            ret.addAll(exec.editFiles(project, edited, changelistId));
        }
        for (MoveData md: moved) {
            exec.moveFile(project, md.source, md.target, changelistId, ! md.moveClientFiles);
        }

        return ret;
    }

    private Map<FilePath, P4FileInfo> mapFilePathsToClient(
            @NotNull Collection<FilePath> files,
            @NotNull P4Exec exec) throws VcsException {
        final List<FilePath> fileList;
        if (files instanceof List) {
            fileList = (List<FilePath>) files;
        } else {
            fileList = new ArrayList<FilePath>(files);
        }
        final Map<FilePath, P4FileInfo> ret = new HashMap<FilePath, P4FileInfo>();
        final List<P4FileInfo> fileInfoList =
                exec.loadFileInfo(project, FileSpecUtil.getFromFilePaths(fileList), fileInfoCache);
        if (fileInfoList.size() == fileList.size()) {
            // Everything worked as expected.
            for (int i = 0; i < fileInfoList.size(); i++) {
                ret.put(fileList.get(i), fileInfoList.get(i));
            }
            // TODO debugging while looking at #62.
            LOG.info("Mapped local to perforce: " + ret);
            return ret;
        }

        // Incorrect input file mapping.  Related to #62.
        // Include better logging for "when" it occurs again.
        // NOTE: this seems to happen when the input file is a new
        // file that's not in Perforce.
        LOG.error("Could not map all input files (" + fileList + ") to Perforce depots (did find " +
                fileInfoList + ")");

        // So do the long matching process for what did work.
        for (FilePath fp : fileList) {
            boolean found = false;
            // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
            // use this instead: getIOFile().getAbsolutePath()
            String path = fp.getIOFile().getAbsolutePath();
            Iterator<P4FileInfo> iter = fileInfoList.iterator();
            while (iter.hasNext()) {
                P4FileInfo fileInfo = iter.next();
                // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
                // use this instead: getIOFile().getAbsolutePath()
                String p4FilePath = fileInfo.getPath().getIOFile().getAbsolutePath();
                if (p4FilePath.equals(path) || fileInfo.getPath().equals(fp)) {
                    found = true;
                    ret.put(fp, fileInfo);
                    iter.remove();
                }
            }
            if (! found) {
                LOG.warn("No P4 info match for local file " + fp);
            }
        }

        if (! fileInfoList.isEmpty()) {
            LOG.warn("No local file match for P4 files " + fileInfoList);
        }

        LOG.info("Successfully mapped local to perforce: " + ret);
        return ret;
    }

    private Map<FilePath, P4FileInfo> sortMap(Map<FilePath, P4FileInfo> allMappings, Collection<FilePath> files) throws P4Exception {
        Map<FilePath, P4FileInfo> ret = new HashMap<FilePath, P4FileInfo>();
        for (FilePath vf : files) {
            P4FileInfo info = allMappings.get(vf);
            if (info == null) {
                LOG.info("No retrieved mapping for " + vf + ": it's probably not under source control");
            } else {
                ret.put(vf, info);
            }
        }
        return ret;
    }


    static class MoveData {
        final IFileSpec source;
        final IFileSpec target;
        final boolean moveClientFiles;

        MoveData(P4FileInfo source, P4FileInfo target, boolean moveClientFiles) throws P4Exception {
            this.source = source.toDepotSpec();
            this.target = target.toClientSpec();
            this.moveClientFiles = moveClientFiles;
        }
    }
}
