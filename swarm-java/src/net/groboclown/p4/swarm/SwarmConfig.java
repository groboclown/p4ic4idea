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

package net.groboclown.p4.swarm;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;

import java.net.URI;
import java.net.URISyntaxException;

public class SwarmConfig {
    private URI uri;
    private String username;
    private String ticket;
    private SwarmVersion version;

    private SwarmConfig() {
        // Do nothing
    }

    /**
     * Loads the URI from the p4d server.
     *
     * @param server Perforce server to reference
     * @return this
     * @throws P4JavaException if there was a problem communicating with the server
     */
    public SwarmConfig withServerInfo(IOptionsServer server)
            throws P4JavaException {
        return withUri(P4ServerSwarmUtil.getSwarmURI(server));
    }

    /**
     * Loads the URI and creates a new ticket from the p4d server.
     *
     * @param server Perforce server to reference
     * @param password user's password
     * @return this
     * @throws P4JavaException if there was a problem communicating with the server,
     *      or if the login was invalid.
     */
    public SwarmConfig withServerInfo(IOptionsServer server, String password)
            throws P4JavaException {
        return
                withUri(P4ServerSwarmUtil.getSwarmURI(server))
                .withTicket(P4ServerSwarmUtil.getTicket(server, password));
    }

    public SwarmConfig withUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public SwarmConfig withUri(String uri)
            throws URISyntaxException {
        return withUri(new URI(uri));
    }

    public SwarmConfig withUsername(String username) {
        this.username = username;
        return this;
    }

    public SwarmConfig withTicket(String ticket) {
        this.ticket = ticket;
        return this;
    }

    public SwarmConfig withVersion(float version) {
        this.version = new SwarmVersion(version);
        return this;
    }

    /**
     * The authentication for the swarm server might allow a password.
     * Recommended to not use this.
     *
     * @param passwd user password.
     * @return this
     */
    public SwarmConfig withPassword(String passwd) {
        // The underlying API is the same, whether you pass a password or
        // a ticket.
        this.ticket = passwd;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public String getVersionPath() {
        return this.version.asPath();
    }

    public String getUsername() {
        return username;
    }

    public String getTicket() {
        return ticket;
    }

    public SwarmVersion getVersion() {
        return version;
    }
}
