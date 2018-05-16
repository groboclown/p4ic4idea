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

import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;

import java.util.Map;

public class MockPendingAction implements PendingAction {
    private P4CommandRunner.ClientAction<?> clientAction;
    private P4CommandRunner.ServerAction<?> serverAction;
    private String sourceId;

    public MockPendingAction withClientAction(P4CommandRunner.ClientAction<?> action) {
        this.clientAction = action;
        this.serverAction = null;
        return this;
    }

    public MockPendingAction withServerAction(P4CommandRunner.ServerAction<?> action) {
        this.clientAction = null;
        this.serverAction = action;
        return this;
    }

    public MockPendingAction withSource(ClientConfig config) {
        this.sourceId = PendingActionFactory.getSourceId(config);
        return this;
    }

    @Override
    public Map<String, String> getState() {
        return null;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }

    @Override
    public String getActionId() {
        return null;
    }

    @Override
    public boolean isClientAction() {
        return clientAction != null;
    }

    @Override
    public P4CommandRunner.ClientAction<?> getClientAction() {
        return clientAction;
    }

    @Override
    public boolean isServerAction() {
        return serverAction != null;
    }

    @Override
    public P4CommandRunner.ServerAction<?> getServerAction() {
        return serverAction;
    }
}
