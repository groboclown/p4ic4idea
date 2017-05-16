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

import net.groboclown.p4.swarm.SwarmConfig;
import net.groboclown.p4.swarm.exceptions.InvalidSwarmServerException;
import net.groboclown.p4.swarm.model.response.Version;

import java.io.IOException;

/**
 * Discovers the version number of the Swarm server being run.
 */
public class DiscoverVersion {
    public static SwarmConfig discoverVersion(SwarmConfig config)
            throws IOException, InvalidSwarmServerException {
        // Start at the lowest number that supports the full version information.
        config.withVersion(1.1f);

        BasicResponse ret = BasicRequest.get(config, "version", null);
        if (ret.getStatusCode() == 404) {
            // This is either a v1 server, or not a Swarm server.
            return checkForVersion1(config);
        } else if (ret.getStatusCode() == 200) {
            // Valid Swarm server.
            Version supportedVersions = ret.getBodyAs(Version.class);
            float highest = 1.1f;
            for (float f : supportedVersions.getApiVersions()) {
                if (f > highest) {
                    highest = f;
                }
            }
            return config.withVersion(highest);
        }

        // Not supported
        throw new InvalidSwarmServerException("Request for version is not supported");
    }

    private static SwarmConfig checkForVersion1(SwarmConfig config)
            throws IOException, InvalidSwarmServerException {
        config.withVersion(1.0f);
        BasicResponse response = BasicRequest.get(config, "version", null);
        if (response.getStatusCode() != 200) {
            // Not supported version.
            throw new InvalidSwarmServerException("Request for version is not supported");
        }
        return config;
    }
}
