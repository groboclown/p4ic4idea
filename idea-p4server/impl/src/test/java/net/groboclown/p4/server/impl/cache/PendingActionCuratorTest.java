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
package net.groboclown.p4.server.impl.cache;

import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.file.AddEditAction;
import net.groboclown.p4.server.api.commands.file.DeleteFileAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.file.MoveFileAction;
import net.groboclown.p4.server.api.commands.file.RevertFileAction;
import net.groboclown.p4.server.api.commands.server.LoginAction;
import net.groboclown.p4.server.impl.cache.store.ActionStore;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static net.groboclown.idea.ExtAsserts.assertContainsExactly;
import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class PendingActionCuratorTest {
    private static final P4ServerName REF_A = P4ServerName.forPortNotNull("not-a-server-a:1666");
    private static final P4ServerName REF_B = P4ServerName.forPortNotNull("not-a-server-b:1666");
    private static final ClientServerRef REF_A1 = new ClientServerRef(REF_A, "c1");
    private static final ClientServerRef REF_A2 = new ClientServerRef(REF_A, "c2");
    private static final ClientServerRef REF_B1 = new ClientServerRef(REF_B, "c1");
    private static final ClientServerRef REF_B2 = new ClientServerRef(REF_B, "c2");

    @Test
    void exact_equal_pending() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateChangelistAction action = new CreateChangelistAction(REF_A1, "comment", "local-id");
        ActionStore.PendingAction pending = ActionStore.createPendingAction(REF_A1, action);

        PendingActionCurator.CurateResult res = curator.curate(pending, pending);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void exact_equal_client_action() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateChangelistAction action = new CreateChangelistAction(REF_A2, "comment", "local-id");
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A2, action);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A2, action);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void exact_equal_server_action() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateJobAction action = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A, action);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, action);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void createJob_after_notCreateJob() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateJobAction addedAction = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        LoginAction existingAction = new LoginAction();
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A, existingAction);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, addedAction);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_BOTH_CONTINUE, res);
    }

    @Test
    void createJob1_after_createJob2() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateJobAction addedAction = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        CreateJobAction existingAction = new CreateJobAction(new P4JobImpl("j2", "j2", null));
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A, existingAction);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, addedAction);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_BOTH_CONTINUE, res);
    }

    @Test
    void createJob1_after_createJob1() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateJobAction addedAction = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        CreateJobAction existingAction = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A, existingAction);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, addedAction);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void login() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateJobAction existingAction = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        LoginAction addedAction = new LoginAction();
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A, existingAction);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, addedAction);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void fetchFiles() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        FetchFilesAction addedAction = new FetchFilesAction(Collections.emptyList(), null, false);
        CreateChangelistAction existingAction = new CreateChangelistAction(REF_B2, "comment", "local-id");
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_B2, addedAction);
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_B2, existingAction);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void submitFiles() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        SubmitChangelistAction addedAction = new SubmitChangelistAction(
                new P4ChangelistIdImpl(100, REF_B1), Collections.emptyList(), null, null, null);
        CreateChangelistAction existingAction = new CreateChangelistAction(REF_B1, "comment", "local-id");
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_B1, addedAction);
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_B1, existingAction);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED_STOP, res);
    }

    @Test
    void curateFirstAction() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        List<ActionStore.PendingAction> actions = new ArrayList<>();

        CreateJobAction addedAction = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, addedAction);

        curator.curateActionList(added, actions);

        assertContainsExactly(actions, added);
    }

    @Test
    void curateFile_add_delete() {
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc"
        );
        MockVirtualFile f1 = fs.get("a.txt");

        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl = new P4ChangelistIdImpl(100, REF_A1);

        ActionStore.PendingAction addFile = ActionStore.createPendingAction(REF_A1, new AddEditAction(
                f1.asFilePath(), null, cl, (String) null));
        actions.add(addFile);
        ActionStore.PendingAction deleteFile =
                ActionStore.createPendingAction(REF_A1, new DeleteFileAction(f1.asFilePath(), cl));

        curator.curateActionList(deleteFile, actions);

        // Delete file cannot tell if the file was open for add or edit, so it must be left alone.
        assertContainsExactly(actions, addFile, deleteFile);
    }

    @Test
    void curateFile_delete_delete_sameCl() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl1 = new P4ChangelistIdImpl(100, REF_A1);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc"
        );
        MockVirtualFile f1 = fs.get("a.txt");

        ActionStore.PendingAction firstDelete = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(f1.asFilePath(), cl1));
        actions.add(firstDelete);
        ActionStore.PendingAction secondDelete = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(f1.asFilePath(), cl1));

        curator.curateActionList(secondDelete, actions);

        // The first delete should be curated, so that the intention of the second delete's changelist is maintained.
        assertContainsExactly(actions, firstDelete);
    }

    @Test
    void curateFile_delete_delete_diffCl() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl1 = new P4ChangelistIdImpl(100, REF_A1);
        P4ChangelistIdImpl cl2 = new P4ChangelistIdImpl(101, REF_A1);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc"
        );
        MockVirtualFile f1 = fs.get("a.txt");

        ActionStore.PendingAction firstDelete = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(f1.asFilePath(), cl1));
        actions.add(firstDelete);
        ActionStore.PendingAction secondDelete = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(f1.asFilePath(), cl2));

        curator.curateActionList(secondDelete, actions);

        // The first delete should be curated, so that the intention of the second delete's changelist is maintained.
        assertContainsExactly(actions, secondDelete);
    }

    @Test
    void curateFile_move_deleteTgt() {
        SimplePendingActionFactory actionFactory = new SimplePendingActionFactory(REF_A1);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc",
                "b.txt", "def"
        );
        MockVirtualFile srcFile = fs.get("a.txt");
        MockVirtualFile tgtFile = fs.get("b.txt");
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl1 = new P4ChangelistIdImpl(100, REF_A1);
        P4ChangelistIdImpl cl2 = new P4ChangelistIdImpl(100, REF_A1);

        ActionStore.PendingAction moveFile = ActionStore.createPendingAction(REF_A1,
                new MoveFileAction(srcFile.asFilePath(), tgtFile.asFilePath(), cl1));
        actions.add(moveFile);
        ActionStore.PendingAction deleteFile = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(tgtFile.asFilePath(), cl2));

        curator.curateActionList(deleteFile, actions);

        assertSize(1, actionFactory.created);
        ActionStore.PendingAction created = actionFactory.created.get(0);
        assertContainsExactly(actions, created);

        assertNull(created.serverAction);
        assertNotNull(created.clientAction);
        assertEquals(created.clientAction.getClass(), DeleteFileAction.class);
        DeleteFileAction createdDelete = (DeleteFileAction) created.clientAction;
        assertEquals(cl1, createdDelete.getChangelistId());
    }

    // FIXME add many more situations.

    // The interesting situations are where there are actions-in-the-middle.

    @Test
    void curateFile_delete_revert_add() {
        SimplePendingActionFactory actionFactory = new SimplePendingActionFactory(REF_A1);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc"
        );
        MockVirtualFile file = fs.get("a.txt");
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl = new P4ChangelistIdImpl(100, REF_A1);

        ActionStore.PendingAction deleteFile = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(file.asFilePath(), cl));
        actions.add(deleteFile);
        ActionStore.PendingAction revertFile = ActionStore.createPendingAction(REF_A1,
                new RevertFileAction(file.asFilePath(), false));
        actions.add(revertFile);
        ActionStore.PendingAction addFile = ActionStore.createPendingAction(REF_A1,
                new AddEditAction(file.asFilePath(), null, cl, (String) null));

        curator.curateActionList(addFile, actions);

        // Because offline revert isn't implemented, the revert then add maintains the integrity.
        assertContainsExactly(actions, deleteFile, revertFile, addFile);
    }

    @Test
    void curateFile_delete_add_delete() {
        SimplePendingActionFactory actionFactory = new SimplePendingActionFactory(REF_A1);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc"
        );
        MockVirtualFile file = fs.get("a.txt");
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl = new P4ChangelistIdImpl(100, REF_A1);

        ActionStore.PendingAction deleteFile = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(file.asFilePath(), cl));
        actions.add(deleteFile);
        ActionStore.PendingAction addFile = ActionStore.createPendingAction(REF_A1,
                new AddEditAction(file.asFilePath(), null, cl, (String) null));
        actions.add(addFile);

        // Reuse the delete request
        curator.curateActionList(deleteFile, actions);

        // Because we stop looking with the delete -> add, the list should be maintained as expected.
        assertContainsExactly(actions, deleteFile, addFile, deleteFile);
    }


    @Test
    void curateFile_addTgt_moveSrc_deleteSrc() {
        SimplePendingActionFactory actionFactory = new SimplePendingActionFactory(REF_A1);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc",
                "b.txt", "def"
        );
        MockVirtualFile src = fs.get("a.txt");
        MockVirtualFile tgt = fs.get("b.txt");
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl1 = new P4ChangelistIdImpl(100, REF_A1);
        P4ChangelistIdImpl cl2 = new P4ChangelistIdImpl(100, REF_A1);

        ActionStore.PendingAction addFile = ActionStore.createPendingAction(REF_A1,
                new AddEditAction(tgt.asFilePath(), null, cl1, (String) null));
        actions.add(addFile);
        ActionStore.PendingAction moveFile = ActionStore.createPendingAction(REF_A1,
                new MoveFileAction(src.asFilePath(), tgt.asFilePath(), cl1));
        actions.add(moveFile);
        ActionStore.PendingAction deleteFile = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(src.asFilePath(), cl2));

        curator.curateActionList(deleteFile, actions);

        assertSize(0, actionFactory.created);
        assertContainsExactly(actions, addFile, moveFile);
    }


    @Test
    void curateFile_addTgt_moveSrc_deleteTgt() {
        SimplePendingActionFactory actionFactory = new SimplePendingActionFactory(REF_A1);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);
        Map<String, MockVirtualFile> fs = MockVirtualFileSystem.createTree(
                "a.txt", "abc",
                "b.txt", "def"
        );
        MockVirtualFile src = fs.get("a.txt");
        MockVirtualFile tgt = fs.get("b.txt");
        List<ActionStore.PendingAction> actions = new ArrayList<>();
        P4ChangelistIdImpl cl1 = new P4ChangelistIdImpl(100, REF_A1);
        P4ChangelistIdImpl cl2 = new P4ChangelistIdImpl(100, REF_A1);

        ActionStore.PendingAction addFile = ActionStore.createPendingAction(REF_A1,
                new AddEditAction(tgt.asFilePath(), null, cl1, (String) null));
        actions.add(addFile);
        ActionStore.PendingAction moveFile = ActionStore.createPendingAction(REF_A1,
                new MoveFileAction(src.asFilePath(), tgt.asFilePath(), cl1));
        actions.add(moveFile);
        ActionStore.PendingAction deleteFile = ActionStore.createPendingAction(REF_A1,
                new DeleteFileAction(tgt.asFilePath(), cl2));

        curator.curateActionList(deleteFile, actions);

        assertSize(1, actionFactory.created);
        ActionStore.PendingAction createdDelete = actionFactory.created.get(0);
        assertNull(createdDelete.serverAction);
        assertNotNull(createdDelete.clientAction);
        assertEquals(createdDelete.clientAction.getClass(), DeleteFileAction.class);
        DeleteFileAction deleteAction = (DeleteFileAction) createdDelete.clientAction;
        assertEquals(src.asFilePath(), deleteAction.getFile());
        assertEquals(cl1, deleteAction.getChangelistId());
        assertContainsExactly(actions, addFile, createdDelete);
    }


    static class SimplePendingActionFactory implements PendingActionCurator.PendingActionFactory {
        final ClientServerRef ref;
        List<ActionStore.PendingAction> created = new ArrayList<>();

        SimplePendingActionFactory(ClientServerRef ref) {
            this.ref = ref;
        }

        @NotNull
        @Override
        public ActionStore.PendingAction create(@NotNull P4CommandRunner.ClientAction<?> action) {
            ActionStore.PendingAction ret = ActionStore.createPendingAction(ref, action);
            created.add(ret);
            return ret;
        }

        @NotNull
        @Override
        public ActionStore.PendingAction create(@NotNull P4CommandRunner.ServerAction<?> action) {
            ActionStore.PendingAction ret = ActionStore.createPendingAction(ref.getServerName(), action);
            created.add(ret);
            return ret;
        }
    }
}
