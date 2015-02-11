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
import net.groboclown.idea.p4ic.changes.P4ChangeListId;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.ui.P4OnCheckinPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
        //System.out.println("Submit to server: " + changes);
        final List<VcsException> errors = new ArrayList<VcsException>();

        // Find all the files and their respective P4 changelists.
        final ChangeListManager clm = ChangeListManager.getInstance(vcs.getProject());
        final Map<Client, List<FilePath>> defaultChangeFiles = new HashMap<Client, List<FilePath>>();
        final Map<Client, Map<Integer, List<FilePath>>> pathsPerChangeList = new HashMap<Client, Map<Integer, List<FilePath>>>();
        for (Change change: changes) {
            if (change != null && change.getVirtualFile() != null) {
                LocalChangeList cl = clm.getChangeList(change);
                splitChanges(change, cl, pathsPerChangeList, defaultChangeFiles);
            }
        }

        // FIXME Currently disabled because of server crashes on Windows
        // (caused due to incorrect API usage, but still...)
        errors.add(new VcsException("Submit is disabled."));
        /*
        // If there are files in the default changelist, they need to have their
        // own changelist.  This needs to happen first, because all files that
        // are not in the following changelists are moved into the default.

        if (! defaultChangeFiles.isEmpty()) {
            try {
                IChangelist changelist = vcs.getServer().createChangelist(preparedComment);
                //System.out.println("Submitting files in default changelist as new changelist id " + changelist.getId());
                errors.addAll(P4StatusMessage.messagesAsErrors(
                        vcs.getServer().submitChangelist(
                                defaultChangeFiles, null, changelist.getId())));
            } catch (VcsException e) {
                e.printStackTrace();
                errors.add(e);
            } catch (Exception e) {
                e.printStackTrace();
                errors.add(new VcsException(e));
            }
        }
        for (Map.Entry<Integer, List<FilePath>> en: pathsPerChangeList.entrySet()) {
            try {
                IChangelist changelist = vcs.getServer().getChangelist(en.getKey());
                if (changelist == null || changelist.getStatus() == ChangelistStatus.SUBMITTED) {
                    errors.add(new VcsException("changelist " + changelist + " is already submitted or deleted"));
                } else {
                    errors.addAll(P4StatusMessage.messagesAsErrors(
                            vcs.getServer().submitChangelist(
                                    en.getValue(), null, changelist.getId())));
                }
            } catch (VcsException e) {
                e.printStackTrace();
                errors.add(e);
            } catch (Exception e) {
                e.printStackTrace();
                errors.add(new VcsException(e));
            }
        }
        */

        return errors;
    }

    private void splitChanges(@NotNull Change change, @Nullable LocalChangeList lcl,
            @NotNull Map<Client, Map<Integer, List<FilePath>>> clientPathsPerChangeList,
            @NotNull Map<Client, List<FilePath>> defaultChangeFiles) {
        final VirtualFile vf = change.getVirtualFile();
        if (vf == null) {
            // unknown file.  deleted?
            LOG.info("Tried to submit a change (" + change + ") which has no file");
            return;
        }
        final FilePath fp = VcsUtil.getFilePath(vf);
        final Client client = vcs.getClientFor(fp);
        if (client == null) {
            // not under p4 control
            LOG.info("Tried to submit a change (" + change + ") that is not under P4 control");
            return;
        }
        if (lcl != null) {
            Collection<P4ChangeListId> p4clList = vcs.getChangeListMapping().getPerforceChangelists(lcl);
            if (p4clList != null) {
                // find the client
                for (P4ChangeListId p4cl: p4clList) {
                    if (p4cl.isIn(client)) {
                        // each IDEA changelist stores at most 1 p4 changelist per client.
                        // so we can exit once it's a client match.
                        if (p4cl.isNumberedChangelist()) {
                            Map<Integer, List<FilePath>> pathsPerChangeList = clientPathsPerChangeList.get(client);
                            if (pathsPerChangeList == null) {
                                pathsPerChangeList = new HashMap<Integer, List<FilePath>>();
                                clientPathsPerChangeList.put(client, pathsPerChangeList);
                            }
                            List<FilePath> files = pathsPerChangeList.get(p4cl.getChangeListId());
                            if (files == null) {
                                files = new ArrayList<FilePath>();
                                pathsPerChangeList.put(p4cl.getChangeListId(), files);
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
        // FIXME figure out how to add these to the changelist; but which one?
        return null;
    }

    @Nullable
    @Override
    public List<VcsException> scheduleUnversionedFilesForAddition(List<VirtualFile> files) {
        // FIXME figure out how to add these to the changelist; but which one?
        return null;
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
