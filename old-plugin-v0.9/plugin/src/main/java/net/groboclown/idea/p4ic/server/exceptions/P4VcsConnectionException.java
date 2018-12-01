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
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.P4Server;
import net.groboclown.idea.p4ic.v2.server.P4ServerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4VcsConnectionException extends VcsConnectionProblem {
    @Nullable
    private final ServerConfig config;

    @Nullable
    private final Project project;

    public P4VcsConnectionException(@NotNull String message) {
        this(null, null, message);
    }

    public P4VcsConnectionException(@NotNull Throwable cause) {
        this(null, null, cause);
    }

    public P4VcsConnectionException(@Nullable Project project, @Nullable ServerConfig config, @NotNull String message) {
        super(message);
        this.project = project;
        this.config = config;
    }

    public P4VcsConnectionException(@Nullable Project project, @Nullable ServerConfig config, @NotNull Throwable cause) {
        super(getMessageFor(cause));
        this.project = project;
        this.config = config;

        initCause(cause);
    }


    Project getProject() {
        return project;
    }

    ServerConfig getServerConfig() {
        return config;
    }


    @Override
    public boolean attemptQuickFix(boolean mayDisplayDialogs) {
        if (project == null || project.isDisposed() || config == null) {
            return false;
        }

        final P4ServerManager manager = P4ServerManager.getInstance(project);
        for (P4Server server: manager.getServers()) {
            if (server.getServerConfig().equals(config)) {
                server.workOnline();
                return server.isWorkingOnline();
            }
        }
        return false;
    }

    @Nullable
    public P4JavaException getP4JavaException() {
        if (getCause() != null && getCause() instanceof P4JavaException) {
            return (P4JavaException) getCause();
        }
        return null;
    }

    @NotNull
    static String getMessageFor(@Nullable Throwable t) {
        Throwable prev = null;
        while (t != null && t != prev) {
            if (t.getMessage() != null && t.getMessage().length() > 0) {
                return t.getMessage();
            }
            if (t instanceof RequestException) {
                RequestException re = (RequestException) t;
                return re.getDisplayString();
            }
            prev = t;
            t = t.getCause();
        }
        return "";
    }
}
