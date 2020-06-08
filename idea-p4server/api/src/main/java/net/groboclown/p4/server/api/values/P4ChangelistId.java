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
package net.groboclown.p4.server.api.values;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import net.groboclown.p4.server.api.ClientServerRef;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ServerConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.Immutable;

/**
 * Perforce changelists must be identified by their ID and their origin
 * server.
 * <p>
 * For a changelist that is pending creation, this object will not change.  Instead, when the
 * creation finishes, the cached version should be refreshed to reflect the new changelist.
 */
@Immutable
public interface P4ChangelistId extends VcsRevisionNumber {
    enum State {
        /** The changelist is a formally created changelist on the server, and not a default changelist. */
        NUMBERED,
        /** The changelist is the unnumbered, "default" changelist for the client, stored on the server. */
        DEFAULT,
        /** The changelist is pending creation on the server. */
        PENDING_CREATION
    }

    int getChangelistId();

    @NotNull
    P4ServerName getServerName();

    /**
     * The client that created the changelist.
     *
     * @return the name of the client that created the changelist.
     */
    @NotNull
    String getClientname();

    @NotNull
    ClientServerRef getClientServerRef();

    @NotNull
    State getState();

    /**
     *
     * @return true if this is the default changelist for the client.
     */
    boolean isDefaultChangelist();

    /**
     * Is this changelist in the given client?  Matches on the client name
     * and the server config id.
     *
     * @param serverConfig server to check
     * @return true if the given client matches the server config and client name.
     */
    boolean isIn(@NotNull ServerConfig serverConfig);
}
