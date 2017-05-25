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

package net.groboclown.p4.swarm.impl;

import net.groboclown.p4.swarm.SwarmClient;
import net.groboclown.p4.swarm.SwarmConfig;
import net.groboclown.p4.swarm.client.ApiClient;
import net.groboclown.p4.swarm.client.auth.HttpBasicAuth;

public abstract class AbstractProxyClient implements SwarmClient {
    private final SwarmConfig config;

    protected AbstractProxyClient(SwarmConfig config) {
        this.config = config;
    }

    protected SwarmConfig getConfig() {
        return config;
    }

    protected ApiClient createClient() {
        ApiClient client = new ApiClient();

        HttpBasicAuth auth = (HttpBasicAuth) client.getAuthentication("p4_auth");
        auth.setUsername(config.getUsername());
        auth.setPassword(config.getTicket());

        client.setBasePath(config.getUri().toASCIIString());

        return client;
    }
}
