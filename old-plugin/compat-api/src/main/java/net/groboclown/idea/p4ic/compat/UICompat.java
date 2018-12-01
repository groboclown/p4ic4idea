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

package net.groboclown.idea.p4ic.compat;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class UICompat {
    public static final UICompat getInstance() {
        return CompatManager.getInstance().getUICompat();
    }

    /**
     * Get the global progress indicator.
     *
     * @return global progress indicator.
     */
    public abstract ProgressIndicator getGlobalProgressIndicator();

    /**
     * Show the VCS configuration GUI, including an "Apply" button.
     *
     * No longer reports the result of the action, as this can lead to bad states
     * when the thread is waiting for the gui to return.
     *
     * @param project vcs project
     * @param configurable vcs.getConfigurable()
     */
    public abstract void editVcsConfiguration(Project project, Configurable configurable);


    /**
     * Ask the user for a password.
     *
     * @param project project source
     * @param title window title
     * @param message main message to display
     * @param requester password key owner class
     * @param key password key
     * @param resetPassword true if the password should be reset before running; not entering a
     *                      password will blank it out.
     * @param error the error message to display
     * @return the password entered by the user, or null if the user cancelled the action.
     */
    @Nullable
    public abstract String askPassword(@Nullable Project project,
            @NotNull String title, @NotNull String message, Class<?> requester,
            @NotNull String key, boolean resetPassword, @NotNull String error);
}
