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

package net.groboclown.p4plugin.components;

import com.intellij.credentialStore.CredentialPromptDialog;
import com.intellij.credentialStore.OneTimeString;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.ApplicationPasswordRegistry;
import net.groboclown.p4.server.api.config.ServerConfig;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.awt.*;


public class ApplicationPasswordRegistryComponent extends ApplicationPasswordRegistry {
    @NotNull
    @Override
    public Promise<OneTimeString> getOrAskFor(@Nullable final Project project, @NotNull final ServerConfig config) {
        return get(config)
                .thenAsync((res) -> {
                    if (res == null) {
                        return promptForPassword(project, config);
                    }
                    return Promises.resolvedPromise(res);
                });
    }

    @Override
    public void askForNewPassword(@Nullable Project project, @NotNull ServerConfig config) {
        promptForPassword(project, config);
    }


    private Promise<OneTimeString> promptForPassword(@Nullable Project project, @NotNull ServerConfig config) {
        final AsyncPromise<OneTimeString> ret = new AsyncPromise<>();
        // Because this can happen while a dialog is shown, we instead want to force it to run.
        // So don't use the ApplicationManager to run later.
        EventQueue.invokeLater(() -> {
            CredentialPromptDialog.askPassword(
                    project,
                    P4Bundle.getString("login.password.title"),
                    P4Bundle.message("login.password.message",
                            config.getServerName().getDisplayName(),
                            config.getUsername()),
                    getCredentialAttributes(config, false),
                    true,
                    null);
        });
        return ret;
    }
}
