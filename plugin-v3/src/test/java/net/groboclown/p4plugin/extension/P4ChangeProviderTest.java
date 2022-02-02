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
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.openapi.vcs.changes.VcsDirtyScope;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.file.ExtendedFileSpec;
import net.groboclown.idea.extensions.ErrorCollectorExtension;
import net.groboclown.idea.extensions.Errors;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.idea.mock.MockChangeListManagerGate;
import net.groboclown.idea.mock.MockChangelistBuilder;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4.server.impl.util.FileSpecBuildUtil;
import net.groboclown.p4plugin.IdeConsts;
import net.groboclown.p4plugin.PluginSetup;
import net.groboclown.p4plugin.ui.DummyProgressIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static net.groboclown.idea.ExtAsserts.assertContainsExactly;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static net.groboclown.p4.server.api.P4CommandRunner.ClientActionCmd.ADD_EDIT_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class P4ChangeProviderTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    final PluginSetup vcs = new PluginSetup();

    /**
     * Ensure that, when offline, one file change is correctly reported.
     *
     * @param tmp temp folder
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
        P4ChangelistId defaultChangeId = vcs.addDefaultChangelist(root);

        // Add the pending add action.
        VirtualFile addedVirtualFile = root.getClientRootDir().createChildData(this, "added.txt");
        FilePath addedFile = VcsUtil.getFilePath(addedVirtualFile);
        // Inline threading, so no need to block.
        vcs.server.getCommandRunner()
                .perform(root.getClientConfig(),
                        new AddEditAction(addedFile, null, defaultChangeId, root.getClientConfig().getCharSetName()));
        assertSize(0, errors);
        assertNotNull(vcs.cacheComponent.getState());
        assertSize(1, vcs.cacheComponent.getState().pendingActions);
        assertEquals(ADD_EDIT_FILE, vcs.cacheComponent.getState().pendingActions.get(0).clientActionCmd);

        // Run the test.
        P4ChangeProvider provider = new P4ChangeProvider(vcs.vcs, vcs.getMockProject());
        provider.getChanges(dirtyScope, changeBuilder, progressIndicator, addGate);

        // Validations
        assertContainsExactly(changeBuilder.addedChangedFiles.values(),
                addedFile);
        assertSize(1, changeBuilder.addedChanges.keySet());
        Map.Entry<String, Change> change =
                changeBuilder.addedChanges.entrySet().iterator().next();
        assertEquals(IdeConsts.DEFAULT_LOCAL_CHANGELIST_NAME, change.getKey());
        assertNull(change.getValue().getBeforeRevision());
        assertNotNull(change.getValue().getAfterRevision());
        assertEquals(addedFile, change.getValue().getAfterRevision().getFile());

        assertSize(0, changeBuilder.ignored);
        assertSize(0, changeBuilder.locallyDeleted);
        assertSize(0, changeBuilder.lockedFolder);
        assertSize(0, changeBuilder.modifiedWithoutCheckout);
        assertSize(0, changeBuilder.removedChanges);
        assertSize(0, changeBuilder.unversioned);

        assertSize(0, addGate.removed);
        assertSize(0, addGate.added);
    }

    /**
     * Ensure that, when offline, adding a new IDE change list does not cause the plugin to create a
     * new Perforce changelist, or some other unexpected side effect.
     *
     * @param tmp temp folder
     */
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void offlineNewIdeChangelist(TemporaryFolder tmp)
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
        P4ChangelistId defaultChangeId = vcs.addDefaultChangelist(root);

        // Add the IDE changelist.
        vcs.addIdeChangelist("Test change", "A test change", false);

        // Run the test.
        P4ChangeProvider provider = new P4ChangeProvider(vcs.vcs, vcs.getMockProject());
        provider.getChanges(dirtyScope, changeBuilder, progressIndicator, addGate);

        // Validations
        assertSize(0, changeBuilder.addedChangedFiles.values());
        assertSize(0, changeBuilder.addedChanges.keySet());

        assertSize(0, changeBuilder.ignored);
        assertSize(0, changeBuilder.locallyDeleted);
        assertSize(0, changeBuilder.lockedFolder);
        assertSize(0, changeBuilder.modifiedWithoutCheckout);
        assertSize(0, changeBuilder.removedChanges);
        assertSize(0, changeBuilder.unversioned);

        assertSize(0, addGate.removed);
        assertSize(0, addGate.added);
    }

    /**
     * Verify that a file open for add in a non-default changelist is not moved.
     */
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void offlineFileInNonDefaultChangelist(TemporaryFolder tmp)
            throws IOException {
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
        P4ChangelistId defaultChangeId = vcs.addDefaultChangelist(root);

        // Add the IDE changelist.
        MockLocalChangeList newIdeChange = vcs.addIdeChangelist("Test change", "A test", false);
        // Link the IDE changelist to a Perforce change.
        P4ChangelistId newP4Change = vcs.addNewChangelist(root, 1, "Test change");
        vcs.linkP4ChangelistToIdeChangelist(root, newP4Change, newIdeChange);

        // Add the pending add action.
        VirtualFile addedVirtualFile = root.getClientRootDir().createChildData(this, "added.txt");
        FilePath addedFile = VcsUtil.getFilePath(addedVirtualFile);
        // Inline threading, so no need to block.
        vcs.server.getCommandRunner()
                .perform(root.getClientConfig(), new AddEditAction(addedFile, null, newP4Change, "UTF-8"));
        assertSize(0, errors);
        assertSize(1, vcs.cacheComponent.getState().pendingActions);
        assertEquals(ADD_EDIT_FILE, vcs.cacheComponent.getState().pendingActions.get(0).clientActionCmd);
    }


    /**
     * The bug this tests: A file is open for edit in the default changelist, and there is another changelist
     * linked to an IDE changelist.  Then the user moves the file to another changelist, and then refreshes the
     * change view.  The bug was where the refresh would loop because the provider would not correctly adjust
     * the file change.
     *
     * @param tmp
     * @param errors
     * @throws InterruptedException
     * @throws IOException
     */
    @ExtendWith({ TemporaryFolderExtension.class, ErrorCollectorExtension.class })
    @Test
    void refreshAfterFileMoved(TemporaryFolder tmp, Errors errors)
            throws InterruptedException, IOException, VcsException {
        vcs.idea.useInlineThreading(errors.get());
        final VcsDirtyScope dirtyScope = mock(VcsDirtyScope.class);
        final MockChangeListManagerGate addGate = new MockChangeListManagerGate(vcs.getMockChangelistManager());
        final MockChangelistBuilder changeBuilder = new MockChangelistBuilder(addGate, vcs.vcs);
        final ProgressIndicator progressIndicator = DummyProgressIndicator.nullSafe(null);

        when(dirtyScope.getVcs()).thenReturn(vcs.vcs);
        when(dirtyScope.wasEveryThingDirty()).thenReturn(true);

        // Simulate online mode
        ClientConfigRoot root = vcs.addClientConfigRoot(tmp, "client");
        assertNotNull(root.getClientRootDir());
        vcs.goOnline(root);

        // The IDE should have a default changelist.
        MockLocalChangeList ideDefaultCl = vcs.addIdeChangelist(IdeConsts.DEFAULT_LOCAL_CHANGELIST_NAME, null, true);

        // Setup the server: 2 changelists, 1 file in default changelist.
        final File openedFile = tmp.newFile("test.txt");
        final IFileSpec openedFileSpec = FileSpecBuildUtil.forFiles(openedFile).get(0);
        /* Default changelist is implicit, so we don't need to explicitly construct it.
        final Changelist defCl = new Changelist(
                0, root.getClientConfig().getClientname(),
                root.getClientConfig().getServerConfig().getUsername(),
                ChangelistStatus.PENDING, new Date(), "<default>", false, null
        );
        defCl.setFileSpecs(Collections.singletonList(openedFileSpec));
        */
        final Changelist secondCl = new Changelist(
                2, root.getClientConfig().getClientname(),
                root.getClientConfig().getServerConfig().getUsername(),
                ChangelistStatus.PENDING, new Date(), "new cl", false, null
        );
        secondCl.setFileSpecs(Collections.emptyList());
        vcs.connectionManager
                .withErrors(errors)
                .withServerSetup((config, base) -> {
                    //defCl.setServerImpl(base);
                    secondCl.setServerImpl(base);
                    when(base.getChangelists(eq(null), any())).thenReturn(Arrays.asList(
                            //defCl,
                            secondCl
                    ));
                    //when(base.getChangelist(eq(0), any())).thenReturn(defCl);
                    when(base.getChangelist(eq(2), any())).thenReturn(secondCl);
                    // Get the files opened in the default list.
                    ExtendedFileSpec spec = new ExtendedFileSpec("//depot/test.txt");
                    spec.setAction(FileAction.ADD);
                    when(base.getExtendedFiles(
                            argThat(t -> t.size() == 1 && t.get(0)
                                    .getOriginalPathString().equals("//" + root.getClientConfig().getClientname() + "/...")),
                            argThat(t -> t.getAffectedByChangelist() == 0))).thenReturn(Collections.singletonList(
                                    spec
                    ));
                    return base;
                })
                .withClientSetup((config, base) -> {
                    when(base.openedFiles(any(), any())).then((args) -> {
                        return args.getArguments()[0];
                    });
                    return base;
                });

        // Update the cache.
        P4ChangeProvider provider = new P4ChangeProvider(vcs.vcs, vcs.getMockProject());
        provider.getChanges(dirtyScope, changeBuilder, progressIndicator, addGate);

        // Initial validations that refresh worked.
        assertSize(1, changeBuilder.addedChangedFiles.values());
        assertSize(1, changeBuilder.addedChanges.keySet());

        assertSize(0, changeBuilder.ignored);
        assertSize(0, changeBuilder.locallyDeleted);
        assertSize(0, changeBuilder.lockedFolder);
        assertSize(0, changeBuilder.modifiedWithoutCheckout);
        assertSize(0, changeBuilder.removedChanges);
        assertSize(0, changeBuilder.unversioned);

        assertSize(0, addGate.removed);
        assertSize(1, addGate.added);
    }
}
