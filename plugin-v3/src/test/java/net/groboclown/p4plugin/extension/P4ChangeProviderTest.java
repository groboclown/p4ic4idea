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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.idea.mock.MockChangeListManagerGate;
import net.groboclown.idea.mock.MockChangelistBuilder;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4plugin.PluginSetup;
import net.groboclown.p4plugin.ui.DummyProgressIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertContainsExactly;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.api.P4CommandRunner.ClientActionCmd.ADD_EDIT_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class P4ChangeProviderTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    final PluginSetup vcs = new PluginSetup();

    /**
     * Ensure that, when offline, one file change is correctly reported.
     *
     * @param tmp
     */
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void offlineOneChange(TemporaryFolder tmp)
            throws IOException, VcsException {
        List<Throwable> errors = new ArrayList<>();
        vcs.idea.useInlineThreading(errors);
        VcsDirtyScope dirtyScope = mock(VcsDirtyScope.class);
        MockChangeListManagerGate addGate = new MockChangeListManagerGate(vcs.getMockChangelistManager());
        MockChangelistBuilder changeBuilder = new MockChangelistBuilder(addGate, vcs.vcs);
        ProgressIndicator progressIndicator = DummyProgressIndicator.nullSafe(null);

        when(dirtyScope.getVcs()).thenReturn(vcs.vcs);
        when(dirtyScope.wasEveryThingDirty()).thenReturn(true);

        // Setup offline mode
        ClientConfigRoot root = vcs.addClientConfigRoot(tmp, "client");
        assertNotNull(root.getClientRootDir());
        vcs.goOffline(root);
        assertFalse(vcs.registry.isOnline(root.getClientConfig().getClientServerRef()));

        // Ensure the default changelist is in our cache.
        // Should the test be run with no default changelist?  That implies that the server has never been
        // synchronized, so I'm guessing no.
        P4ChangelistId defaultChangeId =
                vcs.addDefaultChangelist(root, LocalChangeList.DEFAULT_NAME);

        // Add the pending add action.
        VirtualFile addedVirtualFile = root.getClientRootDir().createChildData(this, "added.txt");
        FilePath addedFile = VcsUtil.getFilePath(addedVirtualFile);
        // Inline threading, so no need to block.
        vcs.server.getCommandRunner()
                .perform(root.getClientConfig(), new AddEditAction(addedFile, null, defaultChangeId, (String) null));
        assertSize(0, errors);
        assertSize(1, vcs.cacheComponent.getState().pendingActions);
        assertEquals(ADD_EDIT_FILE, vcs.cacheComponent.getState().pendingActions.get(0).clientActionCmd);

        // Run the test.
        P4ChangeProvider provider = new P4ChangeProvider(vcs.vcs);
        provider.getChanges(dirtyScope, changeBuilder, progressIndicator, addGate);

        assertContainsExactly(changeBuilder.addedChangedFiles.values(),
                addedFile);
    }
}
