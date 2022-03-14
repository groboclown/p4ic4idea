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

package net.groboclown.p4.server.api;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.credentialStore.OneTimeString;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.util.Arrays;


/**
 * Maintains the registry of plaintext passwords (either stored in files or entered by the user).
 * Passwords are stored in such a way that there may be a delay in fetching the actual value,
 * so they are returned as Future objects.
 * <p>
 * This does not post messages to the message bus, as that would mean it's trivially easy for
 * malicious plugins to steal them.
 */
@Service(Service.Level.APP)
public abstract class ApplicationPasswordRegistry {
    private static final Logger LOG = Logger.getInstance(ApplicationPasswordRegistry.class);

    public static final Class<ApplicationPasswordRegistry> COMPONENT_CLASS = ApplicationPasswordRegistry.class;

    @NotNull
    public static ApplicationPasswordRegistry getInstance() {
        return ApplicationManager.getApplication().getService(COMPONENT_CLASS);
    }

    /**
     * Stores the given password in the registry, associated with the config.  After being
     * called, the password will be blanked out.
     */
    public final void store(@NotNull ServerConfig config, @NotNull char[] password, boolean inMemoryOnly) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Storing password for config " + config.getServerName());
        }
        final CredentialAttributes attr = getCredentialAttributes(config, inMemoryOnly);
        PasswordSafe.getInstance().set(attr, new Credentials(config.getUsername(), password));
        Arrays.fill(password, (char) 0);
    }

    /**
     * Remove the password associated with the configuration.
     *
     * @param config server configuration for the password.
     */
    public final void remove(@NotNull ServerConfig config) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing password for config " + config.getServerName());
        }
        CredentialAttributes attr = getCredentialAttributes(config, false);
        PasswordSafe.getInstance().set(attr, new Credentials(config.getUsername(), (String) null));
    }

    /**
     * Retrieve the stored password.  If it was not stored, then ask the user for the
     * password.  If the user cancels the password prompt, the password is not implicitly
     * forgotten; it must be explicitly removed.
     * <p>
     * This must be implemented carefully.  At startup time, in particular, many systems in
     * the plugin can make requests for a password simultaneously.  If no password has been
     * stored, then the user can receive multiple password prompts.
     *
     * @param config configuration to prompt for the password.
     * @return the password as a promise.  If the one time string is null, then the user did not
     *      enter a password.
     */
    @NotNull
    public abstract Promise<OneTimeString> getOrAskFor(@Nullable Project project, @NotNull ServerConfig config);

    public abstract void askForNewPassword(@Nullable Project project, @NotNull ServerConfig config);

    /**
     * Retrieve the stored password.  If it was not stored, then the promise's
     * resolved value will be null.
     *
     * @param config source
     * @return promise with a null value (if no password stored) or the password.
     */
    @NotNull
    public final Promise<OneTimeString> get(@NotNull final ServerConfig config) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Fetching password for config " + config.getServerName());
        }
        final CredentialAttributes attr = getCredentialAttributes(config, true);
        return PasswordSafe.getInstance().getAsync(attr)
                .then((c) -> c == null ? null : c.getPassword())
                .onProcessed((p) -> {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Fetched password for config " + config.getServerName());
                    }
                })
                .onError((t) -> LOG.warn("Password fetch generated an error", t));
    }

    @NotNull
    protected CredentialAttributes getCredentialAttributes(@NotNull ServerConfig config, boolean inMemory) {
        return new CredentialAttributes(
                "p4ic4idea:" + config.getServerName().getFullPort(),
                config.getUsername(),
                null, inMemory);
    }
}
