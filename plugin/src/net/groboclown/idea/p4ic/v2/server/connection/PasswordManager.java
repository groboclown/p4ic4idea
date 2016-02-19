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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.compat.UICompat;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordStoreException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Returned passwords will always be either null or non-empty.
 * Perforce will raise an error if there is an attempt to login with
 * an empty password.
 */
@State(
        name = "PasswordManager",
        reloadable = true,
        storages = {
                @Storage(
                        id = "default",
                        file = StoragePathMacros.APP_CONFIG + "/perforce-servers.xml"
                )
        }
)
public class PasswordManager implements ApplicationComponent, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(PasswordManager.class);
    private static final String PASSWORD_MANAGER_TAG = "password-manager";
    private static final String HAS_PASSWORD_IN_MEMORY_TAG = "has-password-in-memory";
    private static final String SERVER_KEY_TAG = "server-key";

    private static Class REQUESTOR_CLASS = P4Vcs.class;

    @Nullable
    private MemoryPasswordSafe memoryPasswordSafe = new MemoryPasswordSafe();

    // the keys that are saved into the in-memory safe.
    private final Set<String> hasPasswordInMemory = Collections.synchronizedSet(new HashSet<String>());

    // the keys that are saved into the password data store persistence.
    private final Set<String> hasPasswordInStorage = Collections.synchronizedSet(new HashSet<String>());


    @NotNull
    public static PasswordManager getInstance() {
        return ApplicationManager.getApplication().getComponent(PasswordManager.class);
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
    public String getPassword(@Nullable final Project project, @NotNull final ServerConfig config,
            boolean forceLogin)
            throws PasswordStoreException, PasswordAccessedWrongException {
        if (memoryPasswordSafe == null) {
            LOG.warn("already disposed");
            return null;
        }

        final String key = toKey(config);

        // TODO look at storing these in the memory safe
        String ret = config.getPlaintextPassword();
        if (ret != null && ret.length() > 0) {
            LOG.debug("Using plaintext for " + key);
            return config.getPlaintextPassword();
        }
        if (! forceLogin && ! hasPasswordInStorage.contains(key)) {
            // do not inspect the password safes
            LOG.debug("Skipping the password safe check for " + key);
            return null;
        }


        try {
            ret = memoryPasswordSafe.getPassword(project, REQUESTOR_CLASS, key);
            if (ret == null || ret.length() <= 0) {
                if (hasPasswordInMemory.contains(toKey(config))) {
                    // already set the value, and it's empty.
                    LOG.debug("Using stored null password for " + key);
                    return null;
                }


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
                            REQUESTOR_CLASS, key);
                    if (ret != null && ret.length() > 0) {
                        // keep a local copy of the password for future, outside the
                        // dispatch and in a read action, reference.
                        memoryPasswordSafe.storePassword(project, REQUESTOR_CLASS, key, ret);
                    }
                    hasPasswordInMemory.add(key);
                } else {
                    LOG.warn("Could not get password because the action is called from outside the dispatch thread and in a read action.");
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Read action for " + key, new Throwable());
                    }
                    // Perform a post-action request for the password.
                    // This ruins some of the actions, but it at least pulls in the
                    // password for future requests.
                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                LOG.debug("Fetching for " + key + " in background thread");
                                String pw = PasswordSafe.getInstance().getPassword(project,
                                        REQUESTOR_CLASS, key);
                                hasPasswordInMemory.add(key);
                                if (pw != null) {
                                    memoryPasswordSafe.storePassword(project,
                                            REQUESTOR_CLASS, key, pw);
                                } else {
                                    // force the login.
                                    //P4LoginException ex = new P4LoginException(project, config,
                                    //        P4Bundle.message("login.password.error"));
                                    //AlertManager.getInstance().addCriticalError(
                                    //        new PasswordRequiredHandler(project, config), ex);
                                    LOG.warn("Not forcing a login");
                                }
                            } catch (PasswordSafeException e) {
                                AlertManager.getInstance().addWarning(project,
                                        P4Bundle.message("password.store.error.title"),
                                        P4Bundle.message("password.store.error"),
                                        e, new FilePath[0]);
                            }
                        }
                    });

                    throw new PasswordAccessedWrongException();
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


    public void forgetPassword(@Nullable final Project project, @NotNull final ServerConfig config)
            throws PasswordStoreException {
        final String key = toKey(config);
        LOG.debug("Forgetting for " + key);

        String ret = config.getPlaintextPassword();
        if (ret != null && ret.length() > 0) {
            LOG.debug("Cannot forget password, as it is a plaintext password");
            return;
        }

        PasswordStoreException ex = null;

        // Do not remove the password from the "has password", because we still "have" it
        // pulled into the memory safe.

        // Critical call; do not fail just because it's been disposed.
        if (memoryPasswordSafe != null) {
            try {
                memoryPasswordSafe.removePassword(project, REQUESTOR_CLASS, key);
            } catch (PasswordSafeException e) {
                ex = new PasswordStoreException(e);
            }
            hasPasswordInMemory.remove(key);
        }

        // this can wait on shared resources, so make sure
        // it runs in the background, to avoid deadlocks.
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    PasswordSafe.getInstance().removePassword(project, REQUESTOR_CLASS, key);
                } catch (PasswordSafeException e) {
                    LOG.error(e);
                    // ex = new PasswordStoreException(e);
                }
                hasPasswordInStorage.remove(key);
            }
        });

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

        final String key = toKey(config);

        // clear out the memory password.
        try {
            memoryPasswordSafe.removePassword(project, REQUESTOR_CLASS, key);
        } catch (PasswordSafeException e) {
            throw new PasswordStoreException(e);
        }
        hasPasswordInMemory.remove(key);

        String password = UICompat.getInstance().askPassword(project,
                P4Bundle.message("login.password.title"),
                P4Bundle.message("login.password.message", config.getServiceName(), config.getUsername()),
                REQUESTOR_CLASS, key,
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
            hasPasswordInStorage.add(key);
            try {
                memoryPasswordSafe.storePassword(
                        project, REQUESTOR_CLASS, key,
                        password);
                hasPasswordInMemory.add(key);
            } catch (PasswordSafeException e) {
                LOG.info("Did not store password for " + key, e);
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


    @Override
    public Element getState() {
        Element ret = new Element(PASSWORD_MANAGER_TAG);
        Element hpm = new Element(HAS_PASSWORD_IN_MEMORY_TAG);
        ret.addContent(hpm);
        for (String key : hasPasswordInStorage) {
            Element sk = new Element(SERVER_KEY_TAG);
            hpm.addContent(sk);
            sk.addContent(key);
        }
        return ret;
    }

    @Override
    public void loadState(Element state) {
        hasPasswordInStorage.clear();
        for (Element hpm: state.getChildren(HAS_PASSWORD_IN_MEMORY_TAG)) {
            for (Element sk: hpm.getChildren(SERVER_KEY_TAG)) {
                String key = sk.getText().trim();
                if (key.length() > 0) {
                    hasPasswordInStorage.add(key);
                }
            }
        }
    }
}
