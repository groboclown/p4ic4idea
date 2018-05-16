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
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ClientConfig;
import net.groboclown.p4.server.api.config.ServerConfig;

public class PendingActionFactory {
    public static <R extends P4CommandRunner.ClientResult> PendingAction create(
            ClientServerRef config, P4CommandRunner.ClientAction<R> action) {
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }

    public static <R extends P4CommandRunner.ServerResult> PendingAction create(
            P4ServerName config, P4CommandRunner.ServerAction<R> action) {
        // FIXME implement
        throw new IllegalStateException("not implemented");
    }


    public static String getSourceId(ClientConfig config) {
        return getSourceId(config.getClientServerRef());
    }


    public static String getSourceId(ClientServerRef config) {
        return "client:" + config.toString();
    }


    public static String getSourceId(ServerConfig config) {
        return getSourceId(config.getServerName());
    }


    public static String getSourceId(P4ServerName config) {
        return "server:" + config.getUrl();
    }
}
