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
package net.groboclown.p4.server.impl.cache.store;

import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.commands.changelist.MoveFilesToChangelistAction;
import net.groboclown.p4.server.impl.values.P4ChangelistIdImpl;
import net.groboclown.idea.altmock.MockFilePath;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProjectCacheStoreTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getSetState(TemporaryFolder tmpDir)
            throws PrimitiveMap.UnmarshalException {
        ProjectCacheStore.State state = new ProjectCacheStore.State();
        ClientServerRef ref = new ClientServerRef(
                P4ServerName.forPortNotNull("test:1234"),
                "client1"
        );
        state.clientState = Collections.singletonList(new ClientQueryCacheStore(ref).getState());
        state.serverState = Collections.singletonList(new ServerQueryCacheStore(ref.getServerName()).getState());
        MockFilePath fp = new MockFilePath(tmpDir.newFile("test-file.txt"));
        ActionStore.State actionState = ActionStore.getState(
                ActionStore.getSourceId(ref),
                new MoveFilesToChangelistAction(new P4ChangelistIdImpl(1, ref),
                        Collections.singletonList(fp)));
        state.pendingActions = Collections.singletonList(actionState);

        Element serialized = XmlSerializer.serialize(state);

        ProjectCacheStore.State unmarshalled = XmlSerializer.deserialize(serialized, ProjectCacheStore.State.class);
        assertNotNull(unmarshalled);
        assertSize(1, unmarshalled.clientState);
        assertSize(1, unmarshalled.serverState);
        assertSize(1, unmarshalled.pendingActions);

        assertNotNull(unmarshalled.pendingActions.get(0));
        ActionStore.PendingAction moveAction = ActionStore.read(unmarshalled.pendingActions.get(0));
        assertThat(moveAction.clientAction, instanceOf(MoveFilesToChangelistAction.class));
    }
}