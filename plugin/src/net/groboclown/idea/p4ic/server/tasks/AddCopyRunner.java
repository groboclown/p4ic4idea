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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.UserProjectPreferences;
import net.groboclown.idea.p4ic.server.*;
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
    private final FileInfoCache fileInfoCache;
    private final int destination;

    public AddCopyRunner(
            @NotNull Project project,
            @NotNull Collection<VirtualFile> addedFiles,
            @NotNull Map<VirtualFile, VirtualFile> copiedFiles,
            int destination,
            @NotNull FileInfoCache fileInfoCache) {
        this.project = project;
        this.addedFiles = addedFiles;
        this.copiedFiles = copiedFiles;
        this.destination = destination;
        this.fileInfoCache = fileInfoCache;
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
            LOG.info("Copying " + source + " to " + target);

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
                    LOG.debug("Copy: revert deleted target " + target);
                    reverted.add(target);
                } else if (target.isOpenForEditOrAdd()) {
                    if (useIntegrate) {
                        // we can't have an existing open for add when
                        // we're going to integrate over it.
                        LOG.debug("Copy: revert edit/add target " + target);
                        reverted.add(target);
                    } else if (target.getClientAction().isIntegrate()) {
                        // This is an existing integrate or move.  Need
                        // to revert this to allow for the copy.
                        LOG.debug("Copy: revert edit/add target " + target);
                        reverted.add(target);
                    } else {
                        // The target is already open for
                        // add or edit, so there's nothing
                        // additional to do.
                        LOG.debug("Copy: target already open for add/edit: " + target);
                        continue;
                    }
                } else if (target.isOpenInClient()) {
                    // Some other behavior that we don't know about.
                    // Assume it's not right.
                    LOG.debug("Copy: revert unknown state (" + target.getClientAction() + ") target " + target);
                    reverted.add(target);
                }

                if (source.isInDepot()) {
                    if (useIntegrate) {
                        LOG.debug("Copy: integrate " + source + " to " + target);
                        integrated.put(source, target);
                        added.remove(target);
                    } else if (target.isInDepot()) {
                        LOG.debug("Copy: marking the target as edited because integration is disabled");
                        edited.add(target);
                        added.remove(target);
                    } else {
                        LOG.debug("Copy: marking the target as added because integration is disabled");
                        added.add(target);
                    }
                } else {
                    LOG.debug("Copy: adding target since source is not in depot (client or depot) " + target);
                    added.add(target);
                }
            } else {
                LOG.debug("Copy: Ignoring target outside client " + target);
            }
        }

        // Revert comes first, so that the add, edit, and integrate can happen
        // on the same files
        if (! reverted.isEmpty()) {
            ret.addAll(exec.revertFiles(project, P4FileInfo.toClientList(reverted)));
        }

        // TODO debug for testing out #62; switch these back to "debug"
        if (! added.isEmpty()) {
            LOG.info("Adding " + added);
            ret.addAll(exec.addFiles(project, P4FileInfo.toClientList(added), changelistId));
        }

        if (!edited.isEmpty()) {
            LOG.info("Editing " + added);
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
        final UserProjectPreferences preferences = UserProjectPreferences.getInstance(project);
        if (preferences == null) {
            return UserProjectPreferences.DEFAULT_INTEGRATE_ON_COPY;
        }
        return preferences.getIntegrateOnCopy();
    }

    private static Set<P4FileInfo> sortSet(Map<VirtualFile, P4FileInfo> allMappings, Collection<VirtualFile> files) throws P4Exception {
        Set<P4FileInfo> ret = new HashSet<P4FileInfo>();
        for (VirtualFile vf: files) {
            P4FileInfo info = allMappings.get(vf);
            if (info == null) {
                LOG.warn("No retrieved mapping for " + vf.getPath() + " (all mappings: " + allMappings + ")");
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
                LOG.warn("No retrieved mapping for " + vf.getPath() + " (all mappings: " + allMappings + ")");
                throw new P4Exception(P4Bundle.message("error.copy.no-mapping", vf));
            }
            ret.put(vf, info);
        }
        return ret;
    }

    private Map<VirtualFile, P4FileInfo> mapVirtualFilesToClient(
            @NotNull Collection<VirtualFile> files,
            @NotNull P4Exec exec) throws VcsException {
        final List<VirtualFile> fileList;
        if (files instanceof List) {
            fileList = (List<VirtualFile>) files;
        } else {
            fileList = new ArrayList<VirtualFile>(files);
        }
        final Map<VirtualFile, P4FileInfo> ret = new HashMap<VirtualFile, P4FileInfo>();
        final List<P4FileInfo> fileInfoList =
                exec.loadFileInfo(project, FileSpecUtil.getFromVirtualFiles(fileList), fileInfoCache);
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
        for (VirtualFile vf: fileList) {
            boolean found = false;
            // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
            // use this instead: getIOFile()
            Iterator<P4FileInfo> iter = fileInfoList.iterator();
            while (iter.hasNext()) {
                P4FileInfo fileInfo = iter.next();
                // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
                // use this instead: getIOFile()
                File p4vf = fileInfo.getPath().getIOFile();
                if (FileUtil.filesEqual(p4vf, new File(vf.getCanonicalPath())) || fileInfo.getPath().equals(vf)) {
                    found = true;
                    ret.put(vf, fileInfo);
                    iter.remove();
                }
            }
            if (!found) {
                LOG.warn("No P4 info match for local file " + vf);
            }
        }

        if (!fileInfoList.isEmpty()) {
            LOG.warn("No local file match for P4 files " + fileInfoList);
        }

        LOG.info("Successfully mapped local to perforce: " + ret);
        return ret;
    }
}
