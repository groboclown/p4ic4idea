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
import com.perforce.p4java.exception.P4JavaException;
import net.groboclown.idea.p4ic.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class P4LoginRequiresPasswordException
        extends P4LoginException {
    public P4LoginRequiresPasswordException(@NotNull P4JavaException cause) {
        super(cause);
    }

    public P4LoginRequiresPasswordException(@NotNull final P4UnknownLoginException e) {
        super(e);
    }

    public P4LoginRequiresPasswordException(@Nullable Project project, @Nullable ServerConfig serverConfig, @NotNull P4JavaException cause) {
        super(project, serverConfig, cause);
    }

    public P4JavaException getP4JavaException() {
        if (getCause() != null && getCause() instanceof P4JavaException) {
            return (P4JavaException) getCause();
        }
        return null;
    }
}
