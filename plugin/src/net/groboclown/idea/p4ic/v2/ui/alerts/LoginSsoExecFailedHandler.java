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

package net.groboclown.idea.p4ic.v2.ui.alerts;

import com.intellij.openapi.application.ApplicationManager;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.v2.server.connection.CriticalErrorHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public class LoginSsoExecFailedHandler implements CriticalErrorHandler {
    private final String cmd;
    private final String stdout;
    private final String stderr;
    private final Exception error;

    public LoginSsoExecFailedHandler(@NotNull String cmd, @Nullable String stdout,
            @Nullable String stderr, @Nullable Exception error) {
        this.cmd = cmd;
        this.stdout = stdout;
        this.stderr = stderr;
        this.error = error;
    }

    @Override
    public void handleError(@NotNull Date when) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        DistinctDialog.performOnDialog(
                DistinctDialog.key(this, cmd),
                null,
                P4Bundle.message("error.loginsso.exec-failed", cmd, error.getMessage(), stdout, stderr),
                P4Bundle.getString("error.loginsso.exec-failed.title"),
                new String[0],
                null,
                new DistinctDialog.ChoiceActor() {
                    @Override
                    public void onChoice(int choice, @NotNull DistinctDialog.OnEndHandler onEndHandler) {
                        // do nothing
                    }
                }
        );
    }
}
