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

package net.groboclown.idea.p4ic.v2.server.connection;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.passwordSafe.impl.providers.memory.MemoryPasswordSafe;
import com.intellij.ide.passwordSafe.ui.PasswordSafePromptDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.PasswordStoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Returned passwords will always be either null or non-empty.
 * Perforce will raise an error if there is an attempt to login with
 * an empty password.
 */
public class PasswordManager implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(PasswordManager.class);

    private static Class REQUESTOR_CLASS = P4Vcs.class;

    @Nullable
    private MemoryPasswordSafe memoryPasswordSafe = new MemoryPasswordSafe();


    @NotNull
    public static PasswordManager getInstance() {
        return ApplicationManager.getApplication().getComponent(PasswordManager.class);
    }

    /**
     *
     *
     * @param config connection config
     * @return password which is null or non-empty.
     */
    @Nullable
    public String getSimplePassword(@NotNull final ServerConfig config) {
        String ret = config.getPlaintextPassword();
        if (ret == null || ret.length() <= 0) {
            return null;
        }
        return ret;
    }


    /**
     * Retrieves the password; it will return null, or a non-empty string.
     *
     * @param project source project
     * @param config server and user configuration
     * @return password which is null or non-empty.
     * @throws PasswordStoreException
     */
    @Nullable
    public String getPassword(@Nullable final Project project, @NotNull final ServerConfig config)
            throws PasswordStoreException {
        if (memoryPasswordSafe == null) {
            LOG.warn("already disposed");
            return null;
        }

        String ret = config.getPlaintextPassword();
        if (ret != null && ret.length() > 0) {
            return config.getPlaintextPassword();
        }
        try {
            ret = memoryPasswordSafe.getPassword(project, REQUESTOR_CLASS, toKey(config));
            if (ret == null || ret.length() <= 0) {
                // From the JavaDoc on the general password getter:
                // This method may be called from the background,
                // and it may need to ask user to enter the master password
                // to access the database by calling {
                //    @link Application#invokeAndWait(Runnable, ModalityState) invokeAndWait()
                // } to show a modal dialog.  So make sure not to call it from the read action.
                // Calling this method from the dispatch thread is allowed.

                if (ApplicationManager.getApplication().isDispatchThread() ||
                        ! ApplicationManager.getApplication().isReadAccessAllowed()) {
                    LOG.debug("Fetching password from PasswordSafe");
                    ret = PasswordSafe.getInstance().getPassword(project,
                            REQUESTOR_CLASS, toKey(config));
                    if (ret != null && ret.length() > 0) {
                        // keep a local copy of the password for future, outside the
                        // dispatch and in a read action, reference.
                        memoryPasswordSafe.storePassword(project, REQUESTOR_CLASS, toKey(config), ret);
                    }
                } else {
                    LOG.warn("Could not get password because the action is called from outside the dispatch thread and in a read action.",
                            new Throwable("stack capture"));
                    // Perform a post-action request for the password.
                    // This ruins some of the actions, but it at least pulls in the
                    // password for future requests.
                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String pw = PasswordSafe.getInstance().getPassword(project,
                                        REQUESTOR_CLASS, toKey(config));
                                if (pw != null) {
                                    memoryPasswordSafe.storePassword(project,
                                            REQUESTOR_CLASS, toKey(config), pw);
                                }
                            } catch (PasswordSafeException e) {
                                AlertManager.getInstance().addWarning(project,
                                        P4Bundle.message("password.store.error.title"),
                                        P4Bundle.message("password.store.error", e.getMessage()),
                                        e, new FilePath[0]);
                            }
                        }
                    });
                }
            }
            if (ret == null || ret.length() <= 0) {
                return null;
            }
            return ret;
        } catch (PasswordSafeException e) {
            throw new PasswordStoreException(e);
        }
    }


    public void forgetPassword(@Nullable Project project, @NotNull final ServerConfig config)
            throws PasswordStoreException {
        PasswordStoreException ex = null;

        // Critical call; do not fail just because it's been disposed.
        if (memoryPasswordSafe != null) {
            try {
                memoryPasswordSafe.removePassword(project, REQUESTOR_CLASS, toKey(config));
            } catch (PasswordSafeException e) {
                ex = new PasswordStoreException(e);
            }
        }

        try {
            PasswordSafe.getInstance().removePassword(project, REQUESTOR_CLASS, toKey(config));
        } catch (PasswordSafeException e) {
            ex = new PasswordStoreException(e);
        }

        if (ex != null) {
            throw ex;
        }
    }


    /**
     * Not really the best place for this method, but it centralizes the PasswordSafe
     * access.
     *
     * @return true if the user entered a password
     */
    public boolean askPassword(@Nullable Project project, @NotNull final ServerConfig config)
            throws PasswordStoreException {
        if (memoryPasswordSafe == null) {
            LOG.warn("Already disposed");
            return false;
        }

        // clear out the memory password.
        try {
            memoryPasswordSafe.removePassword(project, REQUESTOR_CLASS, toKey(config));
        } catch (PasswordSafeException e) {
            throw new PasswordStoreException(e);
        }

        String password = PasswordSafePromptDialog.askPassword(project, ModalityState.any(),
                P4Bundle.message("login.password.title"),
                P4Bundle.message("login.password.message", config.getServiceName(), config.getUsername()),
                REQUESTOR_CLASS, toKey(config),
                true,
                P4Bundle.message("login.password.error")
        );

        if (password == null) {
            // already automatically removed from the store.
            LOG.info("No password entered");
            return false;
        } else {
            // The password that was returned MUST be locally
            // saved, because the user could have selected to not
            // store it.
            LOG.info("New password stored");
            try {
                memoryPasswordSafe.storePassword(
                        project, REQUESTOR_CLASS, toKey(config),
                        password);
            } catch (PasswordSafeException e) {
                LOG.info("Did not store password", e);
                throw new PasswordStoreException(e);
            }
            return true;
        }
    }


    @NotNull
    private static String toKey(@NotNull ServerConfig config) {
        return config.getServiceName() + ">>>" + config.getUsername();
    }


    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        memoryPasswordSafe = null;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "P4PasswordManager";
    }

}
