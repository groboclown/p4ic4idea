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

package net.groboclown.p4.server.api.commands.server;

import net.groboclown.p4.server.api.P4CommandRunner;
import org.jetbrains.annotations.NotNull;

public class ServerInfoQuery implements P4CommandRunner.ServerNameQuery<ServerInfoResult> {
    public static final ServerInfoQuery INSTANCE = new ServerInfoQuery();

    private ServerInfoQuery() {
        // do nothing
    }

    @NotNull
    @Override
    public Class<? extends ServerInfoResult> getResultType() {
        return ServerInfoResult.class;
    }

    @Override
    public P4CommandRunner.ServerNameQueryCmd getCmd() {
        return P4CommandRunner.ServerNameQueryCmd.SERVER_INFO;
    }
}
