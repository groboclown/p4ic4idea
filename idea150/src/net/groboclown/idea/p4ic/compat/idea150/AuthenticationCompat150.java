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

import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.passwordSafe.impl.providers.memory.MemoryPasswordSafe;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.compat.AuthenticationCompat;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationException;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationStore;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AuthenticationCompat150 implements AuthenticationCompat {
    @NotNull
    @Override
    public AuthenticationStore createAuthenticationStore(@Nullable Project project) {
        return new AuthenticationStore150(project);
    }

    private static class AuthenticationStore150 implements AuthenticationStore {
        private final Project project;
        private final MemoryPasswordSafe safe = new MemoryPasswordSafe();

        private AuthenticationStore150(@Nullable Project project) {
            this.project = project;
        }

        @Override
        public void clear(@NotNull String service, @NotNull String user) throws AuthenticationException {
            try {
                safe.removePassword(project, AuthenticationCompat150.class, key(service, user));
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
                String passwd = safe.getPassword(project, AuthenticationCompat150.class, key(service, user));
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
                safe.storePassword(project, AuthenticationCompat150.class, key(service, user),
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
