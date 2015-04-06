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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.server.FileSpecUtil;
import net.groboclown.idea.p4ic.server.P4Exec;
import net.groboclown.idea.p4ic.server.P4FileInfo;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import net.groboclown.idea.p4ic.server.exceptions.P4Exception;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;

public class AddCopyRunner extends ServerTask<List<P4StatusMessage>> {
    private static final Logger LOG = Logger.getInstance(AddCopyRunner.class);

    private final Project project;
    private final Collection<VirtualFile> addedFiles;
    private final Map<VirtualFile, VirtualFile> copiedFiles;
    private final int destination;

    public AddCopyRunner(
            @NotNull Project project,
            @NotNull Collection<VirtualFile> addedFiles,
            @NotNull Map<VirtualFile, VirtualFile> copiedFiles,
            int destination) {
        this.project = project;
        this.addedFiles = addedFiles;
        this.copiedFiles = copiedFiles;
        this.destination = destination;
    }


    @Override
    public List<P4StatusMessage> run(@NotNull P4Exec exec) throws VcsException, CancellationException {
        int changelistId = -1;
        if (destination > 0) {
            changelistId = destination;
        }

        Set<VirtualFile> allFiles = new HashSet<VirtualFile>();
        allFiles.addAll(addedFiles);
        allFiles.addAll(copiedFiles.keySet());
        allFiles.addAll(copiedFiles.values());
        Map<VirtualFile, P4FileInfo> allMappings = mapVirtualFilesToClient(allFiles, exec);

        // First, perform the copy only if the original is in Perforce;
        // if the source is not in Perforce, then it's an add.
        Set<P4FileInfo> added = sortSet(allMappings, addedFiles);
        Set<P4FileInfo> edited = new HashSet<P4FileInfo>(added);
        List<P4FileInfo> reverted = new ArrayList<P4FileInfo>(added.size());
        Map<P4FileInfo, P4FileInfo> integrated = new HashMap<P4FileInfo, P4FileInfo>();

        Map<VirtualFile, P4FileInfo> clientCopySource = sortMap(allMappings, copiedFiles.values());
        Map<VirtualFile, P4FileInfo> clientCopyTarget = sortMap(allMappings, copiedFiles.keySet());


        boolean useIntegrate = isCopyAnIntegrate();

        List<P4StatusMessage> ret = new ArrayList<P4StatusMessage>();

        for (Map.Entry<VirtualFile, P4FileInfo> e: clientCopyTarget.entrySet()) {
            P4FileInfo target = e.getValue();
            P4FileInfo source = clientCopySource.get(copiedFiles.get(e.getKey()));
            log("Copying " + source + " to " + target);

            if (target.isInClientView()) {
                // For copy, we could perform an integrate to
                // indicate the source.  However, the most common
                // use case is to add a new file with the contents
                // of the old one ("I want a file that's like
                // this one").  This is currently configurable,
                // but may be removed in the future.

                if (target.isOpenForDelete()) {
                    // Need to revert the delete first, so that the
                    // copy can happen
                    log("Copy: revert deleted target " + target);
                    reverted.add(target);
                } else if (target.isOpenForEditOrAdd()) {
                    if (useIntegrate) {
                        // we can't have an existing open for add when
                        // we're going to integrate over it.
                        log("Copy: revert edit/add target " + target);
                        reverted.add(target);
                    } else if (target.getClientAction().isIntegrate()) {
                        // This is an existing integrate or move.  Need
                        // to revert this to allow for the copy.
                        log("Copy: revert edit/add target " + target);
                        reverted.add(target);
                    } else {
                        // The target is already open for
                        // add or edit, so there's nothing
                        // additional to do.
                        log("Copy: target already open for add/edit: " + target);
                        continue;
                    }
                } else if (target.isOpenInClient()) {
                    // Some other behavior that we don't know about.
                    // Assume it's not right.
                    log("Copy: revert unknown state (" + target.getClientAction() + ") target " + target);
                    reverted.add(target);
                }


                if (source.isInDepot()) {
                    if (useIntegrate) {
                        log("Copy: integrate " + source + " to " + target);
                        integrated.put(source, target);
                        added.remove(target);
                    } else if (target.isInDepot()) {
                        log("Copy: marking the target as edited because integration is disabled");
                        edited.add(target);
                        added.remove(target);
                    } else {
                        log("Copy: marking the target as added because integration is disabled");
                        added.add(target);
                    }
                } else {
                    log("Copy: adding target since source is not in depot (client or depot) " + target);
                    added.add(target);
                }
            } else {
                log("Copy: Ignoring target outside client " + target);
            }
        }

        // Revert comes first, so that the add, edit, and integrate can happen
        // on the same files
        if (! reverted.isEmpty()) {
            ret.addAll(exec.revertFiles(project, P4FileInfo.toClientList(reverted)));
        }

        if (! added.isEmpty()) {
            log("Adding " + added);
            ret.addAll(exec.addFiles(project, P4FileInfo.toClientList(added), changelistId));
        }

        if (!edited.isEmpty()) {
            log("Editing " + added);
            ret.addAll(exec.editFiles(project, P4FileInfo.toClientList(edited), changelistId));
        }

        for (Map.Entry<P4FileInfo, P4FileInfo> e: integrated.entrySet()) {
            ret.addAll(exec.integrateFiles(project,
                    e.getKey().toDepotSpec(),
                    e.getValue().toClientSpec(),
                    changelistId,
                    e.getValue().getPath().getIOFile().exists()));
        }

        return ret;
    }


    private boolean isCopyAnIntegrate() {
        // TODO make this a user preference at the project level

        return false;

    }

    private static Set<P4FileInfo> sortSet(Map<VirtualFile, P4FileInfo> allMappings, Collection<VirtualFile> files) throws P4Exception {
        Set<P4FileInfo> ret = new HashSet<P4FileInfo>();
        for (VirtualFile vf: files) {
            P4FileInfo info = allMappings.get(vf);
            if (info == null) {
                LOG.warn("No retrieved mapping for " + vf.getPath());
            } else {
                ret.add(info);
            }
        }
        return ret;
    }

    private static Map<VirtualFile, P4FileInfo> sortMap(Map<VirtualFile, P4FileInfo> allMappings, Collection<VirtualFile> files) throws P4Exception {
        Map<VirtualFile, P4FileInfo> ret = new HashMap<VirtualFile, P4FileInfo>();
        for (VirtualFile vf: files) {
            P4FileInfo info = allMappings.get(vf);
            if (info == null) {
                throw new P4Exception("No retrieved mapping for " + vf);
            }
            ret.put(vf, info);
        }
        return ret;
    }

    private Map<VirtualFile, P4FileInfo> mapVirtualFilesToClient(
            @NotNull Collection<VirtualFile> files,
            @NotNull P4Exec exec) throws VcsException {
        // This is really slow, but allows for reuse of the invoked method
        Map<String, VirtualFile> reverseLookup = new HashMap<String, VirtualFile>();
        for (VirtualFile vf : files) {
            // make sure we have the correct name and path separators so it matches up with the FilePath value.
            String path = (new File(vf.getPath())).getAbsolutePath();
            if (reverseLookup.containsKey(path)) {
                throw new IllegalArgumentException("duplicate file " + path);
            }
            reverseLookup.put(path, vf);
        }

        Map<VirtualFile, P4FileInfo> ret = new HashMap<VirtualFile, P4FileInfo>();
        for (P4FileInfo file : exec.loadFileInfo(project, FileSpecUtil.getFromVirtualFiles(reverseLookup.values()))) {
            // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
            // use this instead: getIOFile().getAbsolutePath()
            VirtualFile vf = reverseLookup.remove(file.getPath().getIOFile().getAbsolutePath());
            if (vf == null) {
                // It's a soft error, because we don't expect to get files we didn't request.
                LOG.warn("ERROR: no vf mapping for " + file);
            } else {
                ret.put(vf, file);
            }
        }

        if (!reverseLookup.isEmpty()) {
            // This is a correct LOG.info statement.
            log("No p4 files found for " + reverseLookup.values());
        }

        return ret;
    }
}
