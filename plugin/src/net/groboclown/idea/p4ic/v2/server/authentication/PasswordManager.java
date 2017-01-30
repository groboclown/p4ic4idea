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

package net.groboclown.idea.p4ic.v2.server.authentication;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.compat.AuthenticationCompat;
import net.groboclown.idea.p4ic.compat.UICompat;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationException;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationStore;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import net.groboclown.idea.p4ic.config.ServerConfig;
import net.groboclown.idea.p4ic.extension.P4Vcs;
import net.groboclown.idea.p4ic.server.exceptions.PasswordAccessedWrongException;
import net.groboclown.idea.p4ic.server.exceptions.PasswordStoreException;
import net.groboclown.idea.p4ic.v2.server.connection.AlertManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    // This used to use the MemoryPasswordSafe.  However, as per the Java doc:
    // "The provider that stores passwords in memory in encrypted from. It does not stores passwords on the disk,
    // so all passwords are forgotten after application exit. Some efforts are done to complicate retrieving passwords
    // from page file. However the passwords could be still retrieved from the memory using debugger or full memory
    // dump."
    // The JavaDoc then follows this up with:
    // used in https://github.com/groboclown/p4ic4idea, cannot be deleted
    // So, let's be nice and not use the MemoryPasswordSafe.

    @NotNull
    private Map<String, char[]> memoryPasswordSafe = Collections.synchronizedMap(new HashMap<String, char[]>());

    // the keys that are saved into the password data store persistence.
    private final Set<String> hasPasswordInStorage = Collections.synchronizedSet(new HashSet<String>());

    private volatile boolean disposed = false;


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
     * @throws PasswordStoreException if there was an underlying storage error.
     */
    @NotNull
    public OneUseString getPassword(@Nullable final Project project, @NotNull final ServerConfig config,
            boolean forceFetch)
            throws PasswordStoreException, PasswordAccessedWrongException {

        if (disposed) {
            LOG.warn("already disposed");
            throw new PasswordAccessedWrongException();
        }

        final String key = toKey(config);

        {
            final String plaintextPassword = config.getPlaintextPassword();
            if (plaintextPassword != null && plaintextPassword.length() > 0) {
                LOG.debug("Using plaintext for " + key);
                return new OneUseString(config.getPlaintextPassword());
            }
        }

        try {
            OneUseString chPass = getMemoryPassword(config);
            if (chPass != null) {
                return chPass;
            } else {

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
                    final AuthenticationStore passwordStore = getAuthenticationStore(project);
                    OneUseString ret = passwordStore.get(config.getServerName().getFullPort(), config.getUsername());
                    if (ret == null) {
                        // Weird situation.
                        LOG.info("Storage for password key " + key + " was null");
                        ret = new OneUseString(new char[0]);
                    }
                    // keep a local copy of the password for future, outside the
                    // dispatch and in a read action, reference.
                    ret = setMemoryPassword(config, ret);
                    return ret;
                } else if (! forceFetch) {
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
                            final AuthenticationStore passwordStore = getAuthenticationStore(project);
                            try {
                                LOG.debug("Fetching for " + key + " in background thread");
                                OneUseString pw = passwordStore.get(config.getServerName().getFullPort(), config.getUsername());
                                if (pw != null) {
                                    setMemoryPassword(config, pw);
                                } else {
                                    setMemoryPassword(config, new OneUseString(new char[0]));
                                    LOG.warn("Not forcing a login");
                                }
                            } catch (AuthenticationException e) {
                                if (project == null) {
                                    LOG.warn(P4Bundle.message("password.store.error"), e);
                                } else {
                                    AlertManager.getInstance().addWarning(project,
                                            P4Bundle.message("password.store.error.title"),
                                            P4Bundle.message("password.store.error"),
                                            e, new FilePath[0]);
                                }
                            }
                        }
                    });

                    throw new PasswordAccessedWrongException();
                } else {
                    // The user did not force a password fetch.
                    return new OneUseString((char[]) null);
                }
            }
        } catch (AuthenticationException e) {
            throw new PasswordStoreException(e);
        }
    }


    public void forgetPassword(@Nullable final Project project, @NotNull final ServerConfig config)
            throws PasswordStoreException {
        final String key = toKey(config);
        LOG.debug("Forgetting for " + key);

        // If the password is stored in the plaintext file, we still should go through
        // this logic.

        // Do not remove the password from the "has password", because we still "have" it
        // pulled into the memory safe.

        // Critical call; do not fail just because it's been disposed.
        clearMemoryPassword(config);

        // this can wait on shared resources, so make sure
        // it runs in the background, to avoid deadlocks.
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    getAuthenticationStore(project).clear(config.getServerName().getFullPort(), config.getUsername());
                } catch (AuthenticationException e) {
                    e.printStackTrace();
                }
                hasPasswordInStorage.remove(key);
            }
        });
    }


    /**
     * Resets the user's password, and asks for a new one.  This will block input.
     *
     * Not really the best place for this method, but it centralizes the PasswordSafe
     * access.
     *
     * @return true if the user entered a password
     */
    public boolean askPassword(@Nullable Project project, @NotNull final ServerConfig config)
            throws PasswordStoreException {
        if (disposed) {
            LOG.warn("Already disposed");
            return false;
        }

        final String key = toKey(config);

        String password = UICompat.getInstance().askPassword(project,
                P4Bundle.message("login.password.title"),
                // Note: using the nice server name, rather than the whole ugly string (#116)
                P4Bundle.message("login.password.message", config.getServerName().getDisplayName(), config.getUsername()),
                REQUESTOR_CLASS, key,
                true,
                P4Bundle.message("login.password.error")
        );

        if (password == null) {
            // already automatically removed from the store.
            clearMemoryPassword(config);
            LOG.info("No password entered");
            return false;
        } else {
            // The password that was returned MUST be locally
            // saved, because the user could have selected to not
            // store it.
            LOG.info("New password stored");
            hasPasswordInStorage.add(key);
            setMemoryPassword(config, password.toCharArray());
            return true;
        }
    }


    @NotNull
    private static String toKey(@NotNull ServerConfig config) {
        return config.getServerName().getFullPort() + ">>>" + config.getUsername();
    }


    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        disposed = true;
        for (char[] chars : memoryPasswordSafe.values()) {
            if (chars != null) {
                Arrays.fill(chars, (char) 0);
            }
        }
        memoryPasswordSafe.clear();
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

    private void setMemoryPassword(@NotNull ServerConfig config, @NotNull char[] password) {
        final String key = toKey(config);
        memoryPasswordSafe.put(key, password);
    }

    private OneUseString setMemoryPassword(@NotNull ServerConfig config, @NotNull OneUseString password) {
        final String key = toKey(config);
        return password.use(new OneUseString.WithString<OneUseString>() {
            @Override
            public OneUseString with(char[] value) {
                memoryPasswordSafe.put(key, value);
                return new OneUseString(value);
            }
        });
    }

    @Nullable
    private OneUseString getMemoryPassword(@NotNull ServerConfig config) {
        final String key = toKey(config);
        char[] pass = memoryPasswordSafe.get(key);
        if (pass == null) {
            return null;
        }
        return new OneUseString(pass);
    }

    private void clearMemoryPassword(@NotNull ServerConfig config) {
        final String key = toKey(config);
        char[] pass = memoryPasswordSafe.remove(key);
        if (pass != null) {
            Arrays.fill(pass, (char) 0);
        }
    }

    private static AuthenticationStore getAuthenticationStore(@Nullable Project project) {
        return AuthenticationCompat.getInstance().createAuthenticationStore(project);
    }
}
