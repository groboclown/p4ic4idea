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
import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

/**
 * This is only called when the file is changed from
 * read-only to writable.
 */
public class P4EditFileProvider implements EditFileProvider {
    private static final Logger LOG = Logger.getInstance(P4EditFileProvider.class);

    public static final String EDIT = "Edit files";


    private final P4Vcs vcs;
    private final P4ChangeListMapping changeListMapping;

    P4EditFileProvider(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
        this.changeListMapping = P4ChangeListMapping.getInstance(vcs.getProject());
    }


    // This method is called with nearly every keystroke, so it must be very, very
    // performant.
    @Override
    public void editFiles(final VirtualFile[] allFiles) throws VcsException {
        if (allFiles == null || allFiles.length <= 0) {
            return;
        }

        // In order to speed up the operation of this call, we will not care who
        // has this open for edit or not.  Make the file writable, then pass on
        // the actual server edit checks to a background thread.

        if (UserProjectPreferences.getEditInSeparateThread(vcs.getProject())) {
            makeWritable(allFiles);
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        openForEdit(allFiles);
                    } catch (VcsInterruptedException e) {
                        AlertManager.getInstance().addWarning(vcs.getProject(),
                                P4Bundle.message("error.cancelled-timeout"),
                                P4Bundle.message("error.cancelled-timeout.changes-added"),
                                e, allFiles);
                    }
                }
            });
        } else {
            openForEdit(allFiles);
        }
    }

    @Override
    public String getRequestText() {
        return null;
    }

    private void makeWritable(@NotNull final VirtualFile[] allFiles) {
        FilePathUtil.makeWritable(vcs.getProject(), allFiles);
    }

    private void openForEdit(final VirtualFile[] allFiles) throws VcsInterruptedException {
        try {
            // In order to better understand where the time goes, this has additional timing code.
            // See bug #99 for the source of this issue.

            long mapVirutalFilesStart = 0L;
            long mapVirtualFilesEnd = 0L;
            int serverCount = 0;
            long addOrEditTime = 0L;

            if (LOG.isDebugEnabled()) {
                mapVirutalFilesStart = System.nanoTime();
            }
            final Map<P4Server, List<VirtualFile>> serverMapping =
                    vcs.mapVirtualFilesToP4Server(Arrays.asList(allFiles));
            if (LOG.isDebugEnabled()) {
                mapVirtualFilesEnd = System.nanoTime();
            }

            for (Entry<P4Server, List<VirtualFile>> entry : serverMapping.entrySet()) {
                final P4Server server = entry.getKey();
                if (server == null) {
                    // not assigned to any server
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Not assigned to server: " + entry.getValue());
                    }
                } else {
                    final int changelist = changeListMapping.
                            getProjectDefaultPerforceChangelist(server).getChangeListId();

                    long addOrEditTimeStart = 0L;
                    long addOrEditTimeEnd = 0L;

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Open for edit/add on changelist " + changelist + ": " + entry.getValue());

                        serverCount++;
                        addOrEditTimeStart = System.nanoTime();
                    }

                    // TODO see if lock is really necessary now with the new server API.
                    // (answer: it's not)
                    vfsLock.lock();
                    try {
                        server.addOrEditFiles(entry.getValue(), changelist);
                    } finally {
                        vfsLock.unlock();
                    }


                    if (LOG.isDebugEnabled()) {
                        addOrEditTimeEnd = System.nanoTime();
                        addOrEditTime += addOrEditTimeEnd - addOrEditTimeStart;
                    }
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Edit timing: sort files: " + (mapVirtualFilesEnd - mapVirutalFilesStart) +
                        "ns, server count: " + serverCount + ", add or edit time: " +
                        addOrEditTime + "ns");
            }
        } catch (InterruptedException e) {
            throw new VcsInterruptedException(e);
        }
    }
}
