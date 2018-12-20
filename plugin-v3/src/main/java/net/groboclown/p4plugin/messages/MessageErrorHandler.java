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

package net.groboclown.p4plugin.messages;

import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.impl.connection.impl.MessageP4RequestErrorHandler;
import net.groboclown.p4plugin.P4Bundle;
import net.groboclown.p4plugin.components.UserProjectPreferences;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class MessageErrorHandler
        extends MessageP4RequestErrorHandler {
    public MessageErrorHandler(@NotNull Project project) {
        super(project);
    }

    protected int getMaxRetryCount() {
        return UserProjectPreferences.getRetryActionCount(getProject());
    }

    @Nls
    @NotNull
    @Override
    protected String getMessage(@NotNull String messageKey, @NotNull Throwable t, Object... arguments) {
        return P4Bundle.message(messageKey, t.getClass().getSimpleName(), t.getMessage(), arguments);
    }
}
