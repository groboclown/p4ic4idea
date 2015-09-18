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

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.passwordSafe.PasswordStorage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PasswordStore {
    private static final Logger LOG = Logger.getInstance(PasswordStore.class);

    private PasswordStore() {
        // Static utility class
    }


    /**
     *
     * @param config config with the server connection settings.
     * @return the password, or null if it isn't already stored.
     */
    @Nullable
    public static char[] getPasswordFor(@Nullable Project project, @NotNull ServerConfig config) {
        String ret;
        try {
            ret = getService().getPassword(project, PasswordStore.class, getServiceNameFor(config));
        } catch (PasswordSafeException e) {
            // Password problems seem to be universally just logged
            LOG.info(e);
            return null;
        }
        if (ret == null) {
            return null;
        }
        return ret.toCharArray();
    }


    public static void storePasswordFor(@NotNull Project project, @NotNull ServerConfig config, @NotNull char[] password) {
        try {
            getService().storePassword(project, PasswordStore.class, getServiceNameFor(config), new String(password));
        } catch (PasswordSafeException e) {
            // Password problems seem to be universally just logged
            LOG.info(e);
        }
    }


    @NotNull
    private static String getServiceNameFor(@NotNull ServerConfig config) {
        return config.getServiceName();
    }


    @NotNull
    private static PasswordStorage getService() {
        return PasswordSafe.getInstance();
    }
}
