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

package net.groboclown.idea.p4ic.compat.auth;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic way to store passwords.  The retrieved password should be blanked out as
 * soon as it is no longer required.
 */
public interface AuthenticationStore {
    void clear(@NotNull String service, @NotNull String user) throws AuthenticationException;

    boolean has(@NotNull String service, @NotNull String user) throws AuthenticationException;

    @Nullable
    OneUseString get(@NotNull String service, @NotNull String user) throws AuthenticationException;

    void set(@NotNull String service, @NotNull String user, @NotNull char[] authenticationToken)
            throws AuthenticationException;
}
