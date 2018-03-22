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

package net.groboclown.idea.p4ic.compat.idea171;

import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.CredentialPromptDialog;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.compat.UICompat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UICompat171 extends UICompat {
    @Override
    public ProgressIndicator getGlobalProgressIndicator() {
        return ProgressManager.getGlobalProgressIndicator();
    }

    @Override
    public void editVcsConfiguration(final Project project, final Configurable configurable) {
        // With the latest updates to IntelliJ, we can no longer run the "editConfigurable" inside an "invokeLater",
        // because editConfigurable shows a dialog, and this call hangs until the dialog returns, which is
        // supposed to be an "invokeAndWait" kind of call.
        // To work around the small chance that this is running inside an invokeLater call, we need to
        // trick IntelliJ to not log an ugly error against us.
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        ShowSettingsUtil.getInstance().editConfigurable(
                                project,
                                ShowSettingsUtilImpl.createDimensionKey(configurable),
                                configurable,
                                // Show apply button?
                                true);
                    }
                });
            }
        });
    }

    @Nullable
    @Override
    public String askPassword(@Nullable final Project project, @NotNull final String title,
            @NotNull final String message,
            final Class<?> requester, @NotNull final String key, final boolean resetPassword,
            @NotNull final String error) {
        return CredentialPromptDialog.askPassword(project, title, message,
                CredentialAttributesKt.CredentialAttributes(requester, key), resetPassword, error);
    }
}
