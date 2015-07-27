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
package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.background.VcsSettableFuture;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;

public class PasswordStore {
    private PasswordStore() {
        // Static utility class
    }


    /**
     *
     * @param config config with the server connection settings.
     * @return the password, or null if it isn't already stored.
     */
    public static char[] getOptionalPasswordFor(@NotNull ServerConfig config) {
        return getService().getPassword(getServiceNameFor(config));
    }


    /**
     *
     * @param project project for the config
     * @param config configuration storing the password
     * @param removeExistingPassword true if the (possibly) stored version is wrong,
     *                               and the user needs to specify the password again.
     * @return the password, or <tt>null</tt> if the user didn't give one.
     */
    public static char[] getRequiredPasswordFor(@NotNull Project project, @NotNull ServerConfig config, boolean removeExistingPassword) {
        VcsSettableFuture<char[]> future = VcsSettableFuture.create();
        getService().findPassword(project, getServiceNameFor(config),
                P4Bundle.message("configuration.dialog.password.title"),
                P4Bundle.message("configuration.dialog.password.message", config.getPort(), config.getUsername()),
                isPersistentPassword(config), removeExistingPassword, future);
        try {
            return future.get();
        } catch (VcsException e) {
            return null;
        } catch (CancellationException e) {
            return null;
        }
    }


    public static void storePasswordFor(@NotNull ServerConfig config, @NotNull char[] password) {
        getService().setPassword(getServiceNameFor(config), password,
                isPersistentPassword(config));
    }


    @NotNull
    private static String getServiceNameFor(@NotNull ServerConfig config) {
        return config.getServiceName();
    }


    private static boolean isPersistentPassword(@NotNull ServerConfig config) {
        return config.storePasswordLocally();
    }


    @NotNull
    private static PasswordStoreService getService() {
        return ApplicationManager.getApplication().getComponent(PasswordStoreService.class);
    }
}
