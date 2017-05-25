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
import net.groboclown.p4.swarm.util.MockClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DiscoverVersionTest {
    private SwarmConfig config;


    @Test
    public void testV1()
            throws IOException, InvalidSwarmServerException {
        MockClient client = MockClient.single(
            MockClient.basicRequest(config, "api/v1/version")
            .get()
            )
            .status(200)
            .simpleJson("{'year':'2015','version':'SWARM/1.0/A/B/C'}")
            .getMockClient();
        float version = DiscoverVersion.discoverVersion(config, client);
        assertThat(version, is(1.0f));
    }

    @Test
    public void testV11()
            throws IOException, InvalidSwarmServerException {
        MockClient client = MockClient.single(
            MockClient.basicRequest(config, "api/v1.1/version")
            .get()
            )
            .status(200)
            .simpleJson("{'year':'2015','version':'SWARM/1.0/A/B/C','apiVersions':[1,1.1]}")
            .getMockClient();
        float version = DiscoverVersion.discoverVersion(config, client);
        assertThat(version, is(1.1f));
    }

    @Test
    public void testV6()
            throws IOException, InvalidSwarmServerException {
        MockClient client = MockClient.single(
                MockClient.basicRequest(config, "api/v1.1/version")
                        .get()
        )
                .status(200)
                .simpleJson("{'year':'2015','version':'SWARM/1.0/A/B/C','apiVersions':[1,1.1,2,3,4,5,6]}")
                .getMockClient();
        float version = DiscoverVersion.discoverVersion(config, client);
        assertThat(version, is(6.f));
    }

    @Test(expected = InvalidSwarmServerException.class)
    public void testNotSwarm()
            throws IOException, InvalidSwarmServerException {
        MockClient client = new MockClient();
        DiscoverVersion.discoverVersion(config, client);
    }

    @Before
    public void before()
            throws URISyntaxException {
        config = new SwarmConfig()
                .withUri("http://localhost");
    }
}
