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

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.extensions.ErrorCollectorExtension;
import net.groboclown.idea.extensions.Errors;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4plugin.PluginSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Collections;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.api.P4CommandRunner.ClientActionCmd.ADD_EDIT_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class P4ChangelistListenerTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    final PluginSetup vcs = new PluginSetup();

    /**
     * Verify that a newly created IDE changelist does not trigger the creation or association of a Perforce
     * changelist.
     */
    @ExtendWith({ TemporaryFolderExtension.class, ErrorCollectorExtension.class })
    @Test
    void offlineCreateNewIdeChangelist(TemporaryFolder tmp, Errors errors)
            throws InterruptedException {
        vcs.idea.useInlineThreading(errors.get());

        // Setup offline mode
        ClientConfigRoot root = vcs.addClientConfigRoot(tmp, "client");
        assertNotNull(root.getClientRootDir());
        vcs.goOffline(root);
        assertFalse(vcs.registry.isOnline(root.getClientConfig().getClientServerRef()));

        // Add the pending add action.
        MockLocalChangeList changeList =
                vcs.addIdeChangelist("Test change", "A test change", false);

        // Run the test.
        P4ChangelistListener listener = new P4ChangelistListener(vcs.idea.getMockProject());
        listener.changeListAdded(changeList);

        Pair<IdeChangelistMap, IdeFileMap>
                cacheState = vcs.cacheComponent.getServerOpenedCache();
        assertEmpty(cacheState.first.getP4ChangesFor(changeList));

        // TODO any additional validations?
    }

    /**
     * Verify that moving a file that's marked for add in the default changelist, when moved
     * to an IDE changelist that isn't linked to a Perforce changelist, causes the creation
     * of a Perforce changelist action, and the file is moved.
     */
    @ExtendWith({ TemporaryFolderExtension.class, ErrorCollectorExtension.class })
    @Test
    void offlineMoveFileToNewChangelist(TemporaryFolder tmp, Errors errors)
            throws InterruptedException, IOException {
        vcs.idea.useInlineThreading(errors.get());

        // Setup offline mode
        ClientConfigRoot root = vcs.addClientConfigRoot(tmp, "client");
        assertNotNull(root.getClientRootDir());
        vcs.goOffline(root);
        assertFalse(vcs.registry.isOnline(root.getClientConfig().getClientServerRef()));

        P4ChangelistId defaultChangeId = vcs.addDefaultChangelist(root);

        // Add the pending add action.
        VirtualFile addedVirtualFile = root.getClientRootDir().createChildData(this, "added.txt");
        FilePath addedFile = VcsUtil.getFilePath(addedVirtualFile);
        // Inline threading, so no need to block.
        vcs.server.getCommandRunner()
                .perform(root.getClientConfig(),
                        new AddEditAction(addedFile, null, defaultChangeId, root.getClientConfig().getCharSetName()));
        errors.assertEmpty();
        assertNotNull(vcs.cacheComponent.getState());
        assertSize(1, vcs.cacheComponent.getState().pendingActions);
        assertEquals(ADD_EDIT_FILE, vcs.cacheComponent.getState().pendingActions.get(0).clientActionCmd);
        Change change = new Change(null,
                new CurrentContentRevision(addedFile));

        // Create the destination IDE change list.
        MockLocalChangeList changeList =
                vcs.addIdeChangelist("Test change", "A test change", false);

        // Run the test.
        P4ChangelistListener listener = new P4ChangelistListener(vcs.idea.getMockProject());
        listener.changesMoved(Collections.singleton(change),
                vcs.getMockChangelistManager().getDefaultChangeList(),
                changeList);

        // Validate
        Pair<IdeChangelistMap, IdeFileMap>
                cacheState = vcs.cacheComponent.getServerOpenedCache();
        assertNotNull(cacheState.first.getP4ChangesFor(changeList));
        assertSize(1, cacheState.first.getP4ChangesFor(changeList));
        assertNotNull(cacheState.second.forIdeFile(addedFile));
        // TODO add more validations
    }
}
