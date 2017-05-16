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
     *
     * @param passwd
     * @return
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
