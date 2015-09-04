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
package net.groboclown.idea.p4ic.server.exceptions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConnectionProblem;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.server.ServerStatus;
import net.groboclown.idea.p4ic.server.ServerStoreService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4DisconnectedException extends VcsConnectionProblem {
    @Nullable
    private final ServerConfig config;

    @Nullable
    private final Project project;

    public P4DisconnectedException() {
        this(null, null, P4Bundle.message("exception.not-connected"));
    }

    public P4DisconnectedException(@NotNull String message) {
        this(null, null, message);
    }

    public P4DisconnectedException(@NotNull Throwable cause) {
        this(null, null, cause);
    }

    public P4DisconnectedException(@Nullable Project project, @Nullable ServerConfig config, @NotNull String message) {
        super(message);
        this.project = project;
        this.config = config;
    }

    public P4DisconnectedException(@Nullable Project project, @Nullable ServerConfig config, @NotNull Throwable cause) {
        super(cause.getMessage());
        this.project = project;
        this.config = config;

        initCause(cause);
    }


    @Override
    public boolean attemptQuickFix(boolean mayDisplayDialogs) {
        if (project == null || project.isDisposed() || config == null) {
            return false;
        }
        try {
            ServerStatus status = ServerStoreService.getInstance().getServerStatus(project, config);
            if (status.isWorkingOnline()) {
                if (status.onDisconnect()) {
                    //status.onReconnect();
                    return true;
                }
            }
            return false;
        } catch (NullPointerException npe) {
            // ignore
            return false;
        } catch (VcsConnectionProblem vcsConnectionProblem) {
            return false;
        }
    }

}
