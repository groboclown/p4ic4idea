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
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4DisconnectedException extends P4VcsConnectionException {
    public P4DisconnectedException() {
        super(P4Bundle.message("exception.not-connected"));
    }

    public P4DisconnectedException(@NotNull String message) {
        super(message);
    }

    public P4DisconnectedException(@NotNull Throwable cause) {
        super(cause);
    }

    public P4DisconnectedException(@Nullable Project project, @Nullable ServerConfig config, @NotNull String message) {
        super(project, config, message);
    }

    public P4DisconnectedException(@Nullable Project project, @Nullable ServerConfig config, @NotNull Throwable cause) {
        super(project, config, cause);
    }
}
