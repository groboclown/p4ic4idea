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

package net.groboclown.idea.p4ic.compat.idea163;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialStore;
import com.intellij.credentialStore.CredentialStoreFactory;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordStorage;
import com.intellij.ide.passwordSafe.impl.providers.memory.MemoryPasswordSafe;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.compat.AuthenticationCompat;
import net.groboclown.idea.p4ic.compat.auth.AuthenticationStore;
import net.groboclown.idea.p4ic.compat.auth.OneUseString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AuthenticationCompat163 implements AuthenticationCompat {
    @NotNull
    @Override
    public AuthenticationStore createAuthenticationStore(@Nullable Project project) {
        ExtensionsArea extensionsArea = Extensions.getRootArea();
        ExtensionPoint<CredentialStoreFactory> extensionPoint =
                extensionsArea.getExtensionPoint(CredentialStoreFactory.CREDENTIAL_STORE_FACTORY);
        CredentialStoreFactory factory = extensionPoint.getExtension();
        if (factory == null) {
            return new CredentialAuthStore(new CredentialStoreFactory() {
                @Nullable
                @Override
                public PasswordStorage create() {
                    return PasswordSafe.getInstance();
                }
            });
        }
        return new CredentialAuthStore(factory);
    }


    private static class CredentialAuthStore implements AuthenticationStore {
        private final CredentialStore credentials;

        private CredentialAuthStore(@NotNull CredentialStoreFactory factory) {
            this.credentials = factory.create();
        }

        @Override
        public void clear(@NotNull String service, @NotNull String user) {
            credentials.setPassword(createAttributes(service, user), null);
        }

        @Override
        public boolean has(@NotNull String service, @NotNull String user) {
            return get(service, user) != null;
        }

        @Nullable
        @Override
        public OneUseString get(@NotNull String service, @NotNull String user) {
            final String password = credentials.getPassword(createAttributes(service, user));
            if (password == null) {
                return null;
            }
            return new OneUseString(password);
        }

        @Override
        public void set(@NotNull String service, @NotNull String user, @NotNull char[] authenticationToken) {
            credentials.setPassword(createAttributes(service, user), new String(authenticationToken));
        }

        private CredentialAttributes createAttributes(@NotNull String service, @NotNull String user) {
            return new CredentialAttributes(service, user, getClass());
        }
    }
}
