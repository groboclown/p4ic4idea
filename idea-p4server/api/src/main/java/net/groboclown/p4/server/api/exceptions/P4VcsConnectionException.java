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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.messagebus.ReconnectRequestMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A problem occurred because the user was disconnected from the server.
 * This is not related to the connection request being wrong.
 */
public class P4VcsConnectionException extends VcsConnectionProblem {
    @Nullable
    private final Project project;

    @NotNull
    private final ClientServerRef ref;

    public P4VcsConnectionException(@Nullable Project project, @NotNull ClientServerRef ref) {
        super("Connection to server " + ref.getServerDisplayId() + " failed");
        this.project = project;
        this.ref = ref;
    }

    public P4VcsConnectionException(@Nullable Project project, @NotNull ClientServerRef ref, @NotNull Throwable cause) {
        this(project, ref);
        initCause(cause);
    }

    @Override
    public boolean attemptQuickFix(boolean mayDisplayDialogs) {
        if (project != null) {
            ReconnectRequestMessage.requestReconnectToClient(project, ref, mayDisplayDialogs);
        }
        return false;
    }
}
