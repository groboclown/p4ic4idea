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

package net.groboclown.idea.mock;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;

import java.util.HashMap;
import java.util.Map;

public class MockPasswordSafe extends PasswordSafe {
    private Map<String, String> passwords = new HashMap<>();

    @Override
    public void set(@NotNull CredentialAttributes credentialAttributes, @Nullable Credentials credentials, boolean inMemoryOnly) {
        String pw = credentials == null ? null : credentials.getPasswordAsString();
        if (pw == null) {
            passwords.remove(getPasswordKey(credentialAttributes));
        } else {
            passwords.put(getPasswordKey(credentialAttributes), pw);
        }
    }

    @Override
    public boolean isMemoryOnly() {
        return true;
    }

    @NotNull
    @Override
    public Promise<Credentials> getAsync(@NotNull CredentialAttributes credentialAttributes) {
        return Promise.resolve(get(credentialAttributes));
    }

    @Override
    public boolean isPasswordStoredOnlyInMemory(@NotNull CredentialAttributes credentialAttributes,
            @NotNull Credentials credentials) {
        return true;
    }

    @Nullable
    @Override
    public Credentials get(@NotNull CredentialAttributes credentialAttributes) {
        String pw = passwords.get(getPasswordKey(credentialAttributes));
        if (pw == null) {
            return null;
        }
        return new Credentials(credentialAttributes.getUserName(), pw);
    }

    @Override
    public void set(@NotNull CredentialAttributes credentialAttributes, @Nullable Credentials credentials) {
        set(credentialAttributes, credentials, true);
    }

    private String getPasswordKey(CredentialAttributes attr) {
        return attr.getServiceName() + '\u263a' + attr.getUserName();
    }
}
