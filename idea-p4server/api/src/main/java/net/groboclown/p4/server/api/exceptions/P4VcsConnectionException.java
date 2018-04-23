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
package net.groboclown.p4.server.api.exceptions;

import com.intellij.openapi.vcs.VcsConnectionProblem;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.messagebus.ReconnectMessage;
import org.jetbrains.annotations.NotNull;

/**
 * A problem occurred because the user was disconnected from the server.
 * This is not related to the connection request being wrong.
 */
public class P4VcsConnectionException extends VcsConnectionProblem {
    @NotNull
    private final ClientServerRef ref;

    public P4VcsConnectionException(@NotNull ClientServerRef ref) {
        super("Connection to server " + ref.getServerDisplayId() + " failed");
        this.ref = ref;
    }

    public P4VcsConnectionException(@NotNull ClientServerRef ref, @NotNull Throwable cause) {
        super("Connection to server " + ref.getServerDisplayId() + " failed");
        initCause(cause);
        this.ref = ref;
    }

    @Override
    public boolean attemptQuickFix(boolean mayDisplayDialogs) {
        ReconnectMessage.requestReconnectToClient(ref, mayDisplayDialogs);
        return false;
    }
}
