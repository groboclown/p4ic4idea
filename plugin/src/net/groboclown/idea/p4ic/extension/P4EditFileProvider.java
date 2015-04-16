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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.server.P4StatusMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This is only called when the file is changed from
 * read-only to writable.
 */
public class P4EditFileProvider implements EditFileProvider {
    private static final Logger LOG = Logger.getInstance(P4EditFileProvider.class);

    public static final String EDIT = "Edit files";

    private final P4Vcs vcs;

    /**
     * Synchronizes on VFS operations; IDEA can send requests to open for edit and move,
     * or open for edit and delete at nearly the same time.  If these are allowed to
     * collide, then the incorrect actions can happen.  By making all these basic
     * actions be synchronized, we guarantee the right operation, but at a slight
     * cost to time.
     *
     * This should be shared with @{link P4VFSListener}
     */
    private final Object vfsSync;

    public P4EditFileProvider(@NotNull P4Vcs vcs, @NotNull Object vfsSync) {
        this.vcs = vcs;
        this.vfsSync = vfsSync;
    }


    @Override
    public void editFiles(final VirtualFile[] allFiles) throws VcsException {
        if (allFiles == null || allFiles.length <= 0) {
            return;
        }
        final Set<VirtualFile> unhandledFiles = new HashSet<VirtualFile>(Arrays.asList(allFiles));
        List<P4StatusMessage> messages = new ArrayList<P4StatusMessage>();
        Map<Client, List<VirtualFile>> mapping = vcs.mapVirtualFilesToClient(unhandledFiles);
        for (Map.Entry<Client, List<VirtualFile>> en: mapping.entrySet()) {
            final Client client = en.getKey();
            final List<VirtualFile> files = en.getValue();
            if (client.isWorkingOnline()) {
                LOG.info("EditFileProvider (" + client + ") edit " + files);
                unhandledFiles.removeAll(files);

                // file editing is always run from within the AWT event
                // dispatch thread, and the UI requires that the edit must
                // finish before this method exits.

                // This, however, may call back into the EDT, which will cause a
                // deadlock if we perform a "startAndWait".
                synchronized (vfsSync) {
                    messages.addAll(client.getServer().editFiles(files,
                            vcs.getChangeListMapping().getProjectDefaultPerforceChangelist(client).getChangeListId()));
                }
            }
        }
        final List<VirtualFile> notWritable = new ArrayList<VirtualFile>();

        if (! unhandledFiles.isEmpty()) {
            LOG.info("will not edit files due to offline mode or not under Perforce: " + unhandledFiles);

            // Set the files writable, if possible
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    for (VirtualFile file : unhandledFiles) {
                        try {
                            file.setWritable(true);
                        } catch (IOException e) {
                            File f = VcsUtil.getFilePath(file).getIOFile();
                            if (!f.setWritable(true)) {
                                notWritable.add(file);
                            }
                        }
                    }
                }
            });
            if (!notWritable.isEmpty()) {
                throw new VcsException("Could not change to writable: " + notWritable);
            }
        }

        LOG.info("messages: " + messages);

        P4StatusMessage.throwIfError(messages, true);
    }

    @Override
    public String getRequestText() {
        return null;
    }
}
