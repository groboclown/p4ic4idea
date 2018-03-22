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

package net.groboclown.p4.simpleswarm;

import net.groboclown.p4.simpleswarm.exceptions.InvalidSwarmServerException;
import net.groboclown.p4.simpleswarm.impl.DiscoverVersion;
import net.groboclown.p4.simpleswarm.impl.SimpleClient;

import java.io.IOException;

public class SwarmClientFactory {
    public static SwarmClient createSwarmClient(SwarmConfig config)
            throws IOException, InvalidSwarmServerException {
        SwarmVersion version = DiscoverVersion.discoverVersion(config).getVersion();
        if (version.isAtLeast(1.1)) {
            return new SimpleClient(config);
        }
        // We don't support version 1.0
        throw new InvalidSwarmServerException("Unsupported swarm version " + version.asPath());
    }


    public static boolean isSwarmSupported(SwarmConfig config) {
        if (config.getUri() == null) {
            return false;
        }
        try {
            createSwarmClient(config);
            return true;
        } catch (IOException e) {
            return false;
        } catch (InvalidSwarmServerException e) {
            return false;
        }
    }
}
