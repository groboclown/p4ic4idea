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

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.groboclown.idea.p4ic.background.VcsFutureSetter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application-wide password storage.  It handles the possible persistence of passwords,
 * and allows for a single processing point for requesting a password from the user.
 *
 * TODO make the use of this class more secure.
 */
@State(
        name = "PerforcePasswordStore",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/perforce.xml")
        }
)
public class PasswordStoreService
        implements ApplicationComponent, PersistentStateComponent<Element> {

    private final Object sync = new Object();

    @NotNull
    private PasswordStorage persist = new PasswordStorage();

    private final PasswordStorage trans = new PasswordStorage();


    public final static class PasswordStorage {
        public Map<String, char[]> storage = new HashMap<String, char[]>();
    }


    public void setPassword(@NotNull String serviceName, @Nullable char[] password, boolean persistent) {
        char[] copy = null;
        if (password != null) {
            copy = new char[password.length];
            System.arraycopy(password, 0, copy, 0, password.length);
        }
        if (persistent) {
            synchronized (sync) {
                trans.storage.remove(serviceName);
                if (password == null) {
                    persist.storage.remove(serviceName);
                } else {
                    persist.storage.put(serviceName, copy);
                }
            }
        } else {
            synchronized (sync) {
                persist.storage.remove(serviceName);
                if (password == null) {
                    trans.storage.remove(serviceName);
                } else {
                    trans.storage.put(serviceName, copy);
                }
            }
        }
    }


    public boolean hasPassword(@NotNull String serviceName) {
        synchronized (sync) {
            return persist.storage.containsKey(serviceName) || trans.storage.containsKey(serviceName);
        }
    }


    /**
     * Searches the storage for the password associated with the service name.
     *
     * @param serviceName service
     * @return password, or <tt>null</tt> if not found.  The returned array should
     *   be filled with 0 after using for security reasons.
     */
    public char[] getPassword(@NotNull String serviceName) {
        char[] password;
        synchronized (sync) {
            password = persist.storage.get(serviceName);
            if (password == null) {
                password = trans.storage.get(serviceName);
            }
        }
        char[] ret = null;
        if (password != null) {
            ret = new char[password.length];
            System.arraycopy(password, 0, ret, 0, password.length);
        }
        return ret;
    }

    /**
     * Returns the password stored (either persistent or transient.  If the password is not
     * found in either place, then the user is prompted with a password dialog asking for the
     * password (which will be stored in either the persistent or transient store, depending
     * on the value of <tt>persistent</tt>).
     *
     * @param persistent should the password be stored?
     * @param removeExistingPassword should the existing password, if it is stored, be forgotten?
     * @param future returns the value; is a future so that this may correctly ask
     *               the user for the password from within the EDT.
     *               The value is the password, or <tt>null</tt> if not found;
     *               if the user cancelled a prompt, then null is returned
     *               as well.  For security reasons, the returned array should be filled
     */
    public void findPassword(@NotNull final Project project, @NotNull final String serviceName, @NotNull final String title,
                             @NotNull final String message, final boolean persistent,
                             boolean removeExistingPassword, final VcsFutureSetter<char[]> future) {
        char[] password;
        synchronized (sync) {
            password = persist.storage.get(serviceName);
            if (password == null) {
                password = trans.storage.get(serviceName);
                if (password != null && removeExistingPassword) {
                    trans.storage.remove(serviceName);
                    password = null;
                }
            } else if (removeExistingPassword) {
                persist.storage.remove(serviceName);
                password = null;
            }
        }
        if (password != null) {
            char[] copy = new char[password.length];
            System.arraycopy(password, 0, copy, 0, password.length);
            future.set(copy);
        } else {
            future.runInEdt(new Runnable() {
                @Override
                public void run() {
                    String password = Messages.showPasswordDialog(project,
                            message, title,
                            Messages.getQuestionIcon());
                    if (password == null) {
                        future.set(null);
                    } else {
                        char[] val = password.toCharArray();
                        setPassword(serviceName, val, persistent);
                        future.set(val);
                        // don't clear val, because it was assigned to the future.
                    }
                }
            });
        }
    }



    @Nullable
    @Override
    public Element getState() {
        Element ret = new Element("storage");
        synchronized (sync) {
            for (Map.Entry<String, char[]> en: persist.storage.entrySet()) {
                Element el = new Element("map");
                ret.addContent(el);
                el.setAttribute("key", en.getKey());
                el.setAttribute("value", new String(en.getValue()));
            }
        }
        return ret;
    }

    @Override
    public void loadState(@NotNull Element state) {
        PasswordStorage newStore = new PasswordStorage();
        List<Element> kids = state.getChildren("map");
        if (kids != null) {
            for (Element kid: kids) {
                String key = kid.getAttributeValue("key");
                String val = kid.getAttributeValue("value");
                if (key != null && val != null) {
                    newStore.storage.put(key, val.toCharArray());
                }
            }
        }
        synchronized (sync) {
            this.persist = newStore;
        }
    }


    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        // do nothing
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "PerforcePasswordStore";
    }
}
