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


import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used as a reference to a connection object.
 * It is mostly used by the caches, so that it can persist to which
 * connection it applies.
 * <p>
 * It can directly be displayed to the user with a {@link #toString()}
 * call.
 */
public final class ClientServerRef {
    private final P4ServerName serverName;
    private final String clientName;


    public ClientServerRef(@NotNull final P4ServerName serverName, @Nullable final String clientName) {
        this.serverName = serverName;
        this.clientName = clientName;
    }


    @NotNull
    public P4ServerName getServerName() {
        return serverName;
    }


    @NotNull
    public String getServerDisplayId() {
        return getServerName().getDisplayName();
    }


    @Nullable
    public String getClientName() {
        return clientName;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass().equals(getClass())) {
            ClientServerRef that = (ClientServerRef) o;
            return that.serverName.equals(serverName) &&
                    Comparing.equal(that.clientName, clientName);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return (serverName.hashCode() << 3) +
                (clientName == null ? 0 : clientName.hashCode());
    }


    @Override
    public String toString() {
        return clientName + "@" + serverName.getDisplayName();
    }
}
