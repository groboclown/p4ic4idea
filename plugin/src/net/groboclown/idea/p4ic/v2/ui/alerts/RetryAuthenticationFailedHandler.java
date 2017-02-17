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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.v2.server.connection.ServerConnectedController;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class RetryAuthenticationFailedHandler extends AbstractErrorHandler {
    private static final Logger LOG = Logger.getInstance(RetryAuthenticationFailedHandler.class);

    private final ServerConfig config;

    public RetryAuthenticationFailedHandler(@NotNull final Project project,
            @NotNull final ServerConnectedController connectedController,
            @NotNull final ServerConfig config,
            @NotNull final Exception exception) {
        super(project, connectedController, exception);
        this.config = config;
    }

    @Override
    public void handleError(@NotNull final Date when) {
        LOG.warn("P4ServerName refused authentication too many times for server " + config.getServerId());

        DistinctDialog.performOnDialog(
                DistinctDialog.key(this, config.getServerName(), config.getUsername()),
                getProject(),
                P4Bundle.message("configuration.retry-auth-problem-ask", getExceptionMessage()),
                P4Bundle.message("configuration.retry-auth-problem.title"),
                new String[]{
                        P4Bundle.message("configuration.retry-auth-problem.retry"),
                        P4Bundle.message("configuration.retry-auth-problem.config-change"),
                        P4Bundle.message("configuration.retry-auth-problem.offline")
                },
                Messages.getErrorIcon(),
                new DistinctDialog.ChoiceActor() {
                    @Override
                    public void onChoice(int choice, @NotNull final DistinctDialog.OnEndHandler onEndHandler) {
                        switch (choice) {
                            case 0:
                                // first option: re-enter password
                                // This needs to run in another event, otherwise the
                                // message dialog will stay active forever.
                                onEndHandler.handleInOtherThread();
                                ApplicationManager.getApplication().invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            connect();
                                        } finally {
                                            onEndHandler.handleInOtherThread();
                                        }
                                    }
                                });
                                break;
                            case 1:
                                // 2nd option: update server config
                                tryConfigChange();
                                break;
                            default:
                                // 3rd option: work offline
                                goOffline();
                                break;
                        }
                    }
                }
        );
    }
}
