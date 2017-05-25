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

import net.groboclown.p4.swarm.exceptions.InvalidSwarmServerException;
import net.groboclown.p4.swarm.impl.DiscoverVersion;
import net.groboclown.p4.swarm.impl.v1.V1Proxy;
import net.groboclown.p4.swarm.impl.v2.V2Proxy;
import net.groboclown.p4.swarm.impl.v3.V3Proxy;
import net.groboclown.p4.swarm.impl.v4.V4Proxy;
import net.groboclown.p4.swarm.impl.v5.V5Proxy;
import net.groboclown.p4.swarm.impl.v6.V6Proxy;

import java.io.IOException;

public class SwarmClientFactory {
    public static SwarmClient createSwarmClient(SwarmConfig config)
            throws IOException, InvalidSwarmServerException {
        DiscoverVersion.discoverVersion(config);
        SwarmVersion version = config.getVersion();
        if (version.isAtLeast(6.)) {
            return new V6Proxy(config);
        }
        if (version.isAtLeast(5.)) {
            return new V5Proxy(config);
        }
        if (version.isAtLeast(4.)) {
            return new V4Proxy(config);
        }
        if (version.isAtLeast(3.)) {
            return new V3Proxy(config);
        }
        // Version 2 can report itself as 1.2
        if (version.isAtLeast(2.) || version.isAtLeast(1.2)) {
            return new V2Proxy(config);
        }
        if (version.isAtLeast(1.1)) {
            return new V1Proxy(config);
        }
        // We don't support version 1.0
        throw new InvalidSwarmServerException("Unsupported swarm version " + version.asPath());
    }
}
