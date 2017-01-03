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

package net.groboclown.idea.p4ic.compat.idea150;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.compat.AuthenticationCompat;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationException;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationStore;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AuthenticationCompat150 extends AuthenticationCompat {
    private static final Class<?> REQUESTING_CLASS = AuthenticationCompat150.class;

    @NotNull
    @Override
    public AuthenticationStore createAuthenticationStore(@Nullable Project project) {
        return new AuthLocalStore(project);
    }

    private static class AuthLocalStore implements AuthenticationStore {
        private final Project project;

        private AuthLocalStore(@Nullable Project project) {
            this.project = project;
        }

        @Override
        public boolean isBlocking() {
            return true;
        }

        @Override
        public void clear(@NotNull String service, @NotNull String user) throws AuthenticationException {
            try {
                PasswordSafe.getInstance().removePassword(project, REQUESTING_CLASS, key(service, user));
            } catch (PasswordSafeException e) {
                throw new AuthenticationException(e);
            }
        }

        @Override
        public boolean has(@NotNull String service, @NotNull String user) throws AuthenticationException {
            return get(service, user) != null;
        }

        @Nullable
        @Override
        public OneUseString get(@NotNull String service, @NotNull String user) throws AuthenticationException {
            try {
                String passwd = PasswordSafe.getInstance().getPassword(
                        project, REQUESTING_CLASS, key(service, user));
                if (passwd == null) {
                    return null;
                }
                return new OneUseString(passwd);
            } catch (PasswordSafeException e) {
                throw new AuthenticationException(e);
            }
        }

        @Override
        public void set(@NotNull String service, @NotNull String user, @NotNull char[] authenticationToken)
                throws AuthenticationException {
            try {
                PasswordSafe.getInstance().storePassword(project, REQUESTING_CLASS, key(service, user),
                        new String(authenticationToken));
            } catch (PasswordSafeException e) {
                throw new AuthenticationException(e);
            }
        }

        private String key(@NotNull String service, @NotNull String user) {
            return service + "<?>" + user;
        }
    }
}
