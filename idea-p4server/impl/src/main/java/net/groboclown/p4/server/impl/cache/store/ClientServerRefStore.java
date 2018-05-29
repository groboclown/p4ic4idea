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

import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;

public class ClientServerRefStore {

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public String serverPort;
        public String clientName;
    }


    public static State getState(ClientServerRef ref) {
        State ret = new State();
        ret.serverPort = ref.getServerName().getFullPort();
        ret.clientName = ref.getClientName();
        return ret;
    }

    public static ClientServerRef read(State state) {
        P4ServerName server = P4ServerName.forPortNotNull(state.serverPort);
        return new ClientServerRef(server, state.clientName);
    }
}
