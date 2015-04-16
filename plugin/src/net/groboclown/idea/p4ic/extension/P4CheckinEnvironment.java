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
package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.NullableFunction;
import com.intellij.util.PairConsumer;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.changes.P4ChangeListCache;
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.changes.P4ChangesViewRefresher;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.exceptions.P4InvalidConfigException;
import net.groboclown.idea.p4ic.ui.P4OnCheckinPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class P4CheckinEnvironment implements CheckinEnvironment {
    private static final Logger LOG = Logger.getInstance(P4CheckinEnvironment.class);

    private final P4Vcs vcs;

    public P4CheckinEnvironment(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }

    @Nullable
    @Override
    public RefreshableOnComponent createAdditionalOptionsPanel(CheckinProjectPanel panel, PairConsumer<Object, Object> additionalDataConsumer) {
        return new P4OnCheckinPanel(vcs.getProject(), panel);
    }

    @Nullable
    @Override
    public String getDefaultMessageFor(FilePath[] filesToCheckin) {
        return null;
    }

    @Nullable
    @Override
    public String getHelpId() {
        return null;
    }

    @Override
    public String getCheckinOperationName() {
        return P4Bundle.message("commit.operation.name");
    }

    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, String preparedComment) {
        return commit(changes, preparedComment, NullableFunction.NULL, new HashSet<String>());
    }

    @Nullable
    @Override
    public List<VcsException> commit(List<Change> changes, final String preparedComment,
            @NotNull NullableFunction<Object, Object> parametersHolder, Set<String> feedback) {
        LOG.info("Submit to server: " + changes);
        final List<VcsException> errors = new ArrayList<VcsException>();

        // Find all the files and their respective P4 changelists.
        // This method deals with the problem of discovering the
        // changelists to submit, and their associated P4 client.
        // The server end deals with filtering out the files that
        // aren't requested to submit.
        final ChangeListManager clm = ChangeListManager.getInstance(vcs.getProject());
        final Map<Client, List<FilePath>> defaultChangeFiles = new HashMap<Client, List<FilePath>>();
        final Map<Client, Map<P4ChangeListId, List<FilePath>>> pathsPerChangeList = new HashMap<Client, Map<P4ChangeListId, List<FilePath>>>();
        for (Change change: changes) {
            if (change != null) {
                LocalChangeList cl = clm.getChangeList(change);
                splitChanges(change, cl, pathsPerChangeList, defaultChangeFiles);
            }
        }

        // If there are files in the default changelist, they need to have their
        // own changelist.  This needs to happen first, because all files that
        // are not in the following changelists are moved into the default.

        // This just puts the defaults into a changelist.  They will be submitted
        // with the rest of the changelists below.

        LOG.info("changes in a changelist: " + pathsPerChangeList);
        LOG.info("changes in default changelists: " + defaultChangeFiles);

        if (! defaultChangeFiles.isEmpty()) {
            for (Map.Entry<Client, List<FilePath>> en: defaultChangeFiles.entrySet()) {
                Client client = en.getKey();
                try {
                    final P4ChangeListId changeList = P4ChangeListCache.getInstance().createChangeList(
                            client, preparedComment);
                    Map<P4ChangeListId, List<FilePath>> clFp = pathsPerChangeList.get(client);
                    if (clFp == null) {
                        clFp = new HashMap<P4ChangeListId, List<FilePath>>();
                        pathsPerChangeList.put(client, clFp);
                    }
                    LOG.info("moving from default changelist in " + client + " to " + changeList + ": " + en.getValue());
                    P4ChangeListCache.getInstance().addFilesToChangelist(client,
                            changeList, en.getValue());
                    clFp.put(changeList, en.getValue());
                } catch (VcsException e) {
                    LOG.warn("Problem sorting files into changelists for client " +
                            client + ": " + en.getValue(), e);
                    errors.add(e);
                }
            }
        }
        for (Map.Entry<Client, Map<P4ChangeListId, List<FilePath>>> en: pathsPerChangeList.entrySet()) {
            final Client client = en.getKey();
            for (Map.Entry<P4ChangeListId, List<FilePath>> clEn: en.getValue().entrySet()) {
                LOG.info("Submit to " + client + " cl " + clEn.getValue() + " files " +
                    clEn.getValue());
                try {
                    client.getServer().submitChangelist(clEn.getValue(),

                            // TODO add jobs
                            Collections.<String>emptyList(),

                            // TODO add job status
                            null,

                            clEn.getKey().getChangeListId());
                } catch (VcsException e) {
                    LOG.warn("Problem submitting changelist " +
                            clEn.getKey().getChangeListId(), e);
                    errors.add(e);
                }
            }
        }

        LOG.info("Errors: " + errors);

        // Mark the changes as needing an update
        P4ChangesViewRefresher.refreshLater(vcs.getProject());
        return errors;
    }

    private void splitChanges(@NotNull Change change, @Nullable LocalChangeList lcl,
            @NotNull Map<Client, Map<P4ChangeListId, List<FilePath>>> clientPathsPerChangeList,
            @NotNull Map<Client, List<FilePath>> defaultChangeFiles) {
        final FilePath fp;
        if (change.getVirtualFile() == null) {
            // possibly deleted.
            if (change.getBeforeRevision() != null) {
                fp = change.getBeforeRevision().getFile();
            } else {
                LOG.info("Tried to submit a change (" + change + ") which has no file");
                return;
            }
        } else {
            fp = VcsUtil.getFilePath(change.getVirtualFile());
        }
        if (fp == null) {
            LOG.info("Change " + change + " had no associated file path");
            return;
        }

        final Client client = vcs.getClientFor(fp);
        if (client == null) {
            // not under p4 control
            LOG.info("Tried to submit a change (" + change + " / " + fp + ") that is not under P4 control");
            return;
        }
        if (lcl != null) {
            Collection<P4ChangeListId> p4clList = vcs.getChangeListMapping().getPerforceChangelists(lcl);
            if (p4clList != null) {
                // find the changelist
                for (P4ChangeListId p4cl: p4clList) {
                    if (p4cl.isIn(client)) {
                        // each IDEA changelist stores at most 1 p4 changelist per client.
                        // so we can exit once it's a client match.
                        if (p4cl.isNumberedChangelist()) {
                            Map<P4ChangeListId, List<FilePath>> pathsPerChangeList = clientPathsPerChangeList.get(client);
                            if (pathsPerChangeList == null) {
                                pathsPerChangeList = new HashMap<P4ChangeListId, List<FilePath>>();
                                clientPathsPerChangeList.put(client, pathsPerChangeList);
                            }
                            List<FilePath> files = pathsPerChangeList.get(p4cl);
                            if (files == null) {
                                files = new ArrayList<FilePath>();
                                pathsPerChangeList.put(p4cl, files);
                            }
                            files.add(fp);
                        } else {
                            addToDefaultChangeFiles(client, fp, defaultChangeFiles);
                        }
                        return;
                    }
                }
            }
        } else {
            LOG.info("Not in a changelist: " + fp + "; putting in the default changelist");
            addToDefaultChangeFiles(client, fp, defaultChangeFiles);
        }
    }

    private void addToDefaultChangeFiles(@NotNull Client client, @NotNull FilePath fp,
            @NotNull Map<Client, List<FilePath>> map) {
        List<FilePath> fpList = map.get(client);
        if (fpList == null) {
            fpList = new ArrayList<FilePath>();
            map.put(client, fpList);
        }
        fpList.add(fp);
    }


    @Nullable
    @Override
    public List<VcsException> scheduleMissingFileForDeletion(List<FilePath> files) {
        LOG.info("scheduleMissingFileForDeletion: " + files);
        final List<VcsException> ret = new ArrayList<VcsException>();
        final Map<Client, List<FilePath>> map;
        try {
            map = vcs.mapFilePathToClient(files);
        } catch (P4InvalidConfigException e) {
            ret.add(e);
            // Can't proceed if this fails
            return ret;
        }

        for (Entry<Client, List<FilePath>> entry : map.entrySet()) {
            final Client client = entry.getKey();
            final P4ChangeListId defaultChange = vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client);
            if (client.isWorkingOnline()) {
                try {
                    client.getServer().deleteFiles(entry.getValue(), defaultChange.getChangeListId());
                } catch (VcsException e) {
                    ret.add(e);
                }
            } else {
                ret.add(new VcsException("client is offline: " + client));
            }
        }

        return ret;
    }

    @Nullable
    @Override
    public List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files) {
        LOG.info("scheduleUnversionedFilesForAddition: " + files);
        final List<VcsException> ret = new ArrayList<VcsException>();

        final Map<Client, List<VirtualFile>> map;
        try {
            map = vcs.mapVirtualFilesToClient(files);
        } catch (P4InvalidConfigException e) {
            ret.add(e);
            // Can't proceed if this fails
            return ret;
        }

        for (Entry<Client, List<VirtualFile>> entry : map.entrySet()) {
            final Client client = entry.getKey();
            final P4ChangeListId defaultChange = vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client);
            if (client.isWorkingOnline()) {
                try {
                    client.getServer().addOrCopyFiles(entry.getValue(),
                            Collections.<VirtualFile, VirtualFile>emptyMap(),
                            defaultChange.getChangeListId());
                } catch (VcsException e) {
                    ret.add(e);
                }
            } else {
                ret.add(new VcsException("client is offline: " + client));
            }
        }

        return ret;
    }

    @Override
    public boolean keepChangeListAfterCommit(ChangeList changeList) {
        return false;
    }

    @Override
    public boolean isRefreshAfterCommitNeeded() {
        // File status (read-only state) may have changed, or CVS substitution
        // may have happened.
        return true;
    }
}
