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
package net.groboclown.p4.simpleswarm.impl;

import net.groboclown.p4.simpleswarm.MockLogger;
import net.groboclown.p4.simpleswarm.SwarmClient;
import net.groboclown.p4.simpleswarm.SwarmClientFactory;
import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.SwarmLogger;
import net.groboclown.p4.simpleswarm.exceptions.InvalidSwarmServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.net.URISyntaxException;


@EnabledIf({
        "!!systemEnvironment['SWARM_URL']",
        "!!systemEnvironment['SWARM_USERNAME']",
        "!!systemEnvironment['SWARM_TICKET']"
})
// Swarm URL must be in a "http://..." or "https://.." style format.
@EnabledIfEnvironmentVariable(named="SWARM_URL", matches="^https?:\\/\\/.+")
class ReviewActionsIntegrationTest {

    @Test
    void create() {

    }

    @Test
    void addChangeToReview() {
    }

    @Test
    void getReview() {
    }

    @Test
    void getReviewIdsForChangelist() {
    }


    private static SwarmClient createClient()
            throws URISyntaxException, IOException, InvalidSwarmServerException {
        SwarmConfig config = new SwarmConfig()
                .withLogger(new MockLogger())
                .withUri(System.getenv("SWARM_URL"))
                .withUsername(System.getenv("SWARM_USERNAME"))
                .withTicket(System.getenv("SWARM_TICKET"));
        return SwarmClientFactory.createSwarmClient(config);
    }

    private static void log(String message, Throwable e) {
        System.err.println("Swarm: " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}