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
import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.SwarmLogger;
import net.groboclown.p4.simpleswarm.exceptions.UnauthorizedAccessException;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BasicRequestTest {

    @Test
    public void get_path_401()
            throws IOException {
        MockHttpRequester mock = new MockHttpRequester();
        mock.withCall()
                .expectsRequest((HttpUriRequest r) ->
                        assertThat("bad URL",
                            r.getURI().toASCIIString(),
                            is("http://my.server:10/api/v2/version")))
                .withResponse(401, "Invalid Credentials", "x-application/json", "{}");
        BasicRequest req = new BasicRequest(mock);
        try {
            req.get(createConfig(), "version");
            fail("Did not throw UnauthorizedAccessException");
        } catch (UnauthorizedAccessException e) {
            assertThat(e.getMessage(), is("Invalid Credentials"));
        }
    }

    @Test
    public void postJson() {
    }

    @Test
    public void postForm() {
    }

    @Test
    public void patch() {
    }

    @Test
    public void toQuery() {
    }

    @Test
    public void toEncodedValues() {
    }

    @Test
    public void toUrl() {
    }

    private static SwarmConfig createConfig() {
        return createConfig(new MockLogger());
    }

    private static SwarmConfig createConfig(SwarmLogger logger) {
        try {
            return new SwarmConfig()
                    .withUri("http://my.server:10")
                    .withLogger(logger)
                    .withUsername("user")
                    .withPassword("pass")
                    .withVersion(2.0f);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}