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

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.changelist.CreateChangelistAction;
import net.groboclown.p4.server.api.commands.changelist.CreateJobAction;
import net.groboclown.p4.server.api.commands.changelist.SubmitChangelistAction;
import net.groboclown.p4.server.api.commands.file.FetchFilesAction;
import net.groboclown.p4.server.api.commands.server.LoginAction;
import net.groboclown.p4.server.impl.cache.store.ActionStore;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.p4.server.impl.values.P4JobImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
    }

    @Test
    void exact_equal_client_action() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateChangelistAction action = new CreateChangelistAction(REF_A2, "comment", "local-id");
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A2, action);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A2, action);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
    }

    @Test
    void exact_equal_server_action() {
        PendingActionCurator.PendingActionFactory actionFactory = mock(PendingActionCurator.PendingActionFactory.class);
        PendingActionCurator curator = new PendingActionCurator(actionFactory);

        CreateJobAction action = new CreateJobAction(new P4JobImpl("j1", "j1", null));
        ActionStore.PendingAction existing = ActionStore.createPendingAction(REF_A, action);
        ActionStore.PendingAction added = ActionStore.createPendingAction(REF_A, action);

        PendingActionCurator.CurateResult res = curator.curate(added, existing);
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
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
        assertEquals(PendingActionCurator.KEEP_BOTH, res);
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
        assertEquals(PendingActionCurator.KEEP_BOTH, res);
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
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
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
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
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
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
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
        assertEquals(PendingActionCurator.KEEP_EXISTING_REMOVE_ADDED, res);
    }

    // TODO test the changelist actions.

    // The rest of the cases, though appropriately tested here, can be better tested in context with the
    // CachePendingActionHandlerImpl.  That tells me that the corresponding loop may be better handled in
    // this class.
}
