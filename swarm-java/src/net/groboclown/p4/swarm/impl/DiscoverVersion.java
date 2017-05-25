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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import net.groboclown.p4.swarm.SwarmConfig;
import net.groboclown.p4.swarm.exceptions.InvalidSwarmServerException;

import java.io.IOException;

/**
 * Discovers the version number of the Swarm server being run.
 *
 * This uses low-level API to construct the request, that way we avoid client API
 * confusion.
 */
public class DiscoverVersion {
    public static SwarmConfig discoverVersion(SwarmConfig config)
            throws IOException, InvalidSwarmServerException {
        final OkHttpClient client = new OkHttpClient();
        return config.withVersion(discoverVersion(config, client));
    }

    static float discoverVersion(SwarmConfig config, OkHttpClient client)
            throws IOException, InvalidSwarmServerException {

        // Start at the lowest number that supports the full version information.
        final Request.Builder reqBuilder = new Request.Builder()
                .url(config.getUri().toASCIIString() + "/api/v1.1/version")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", Credentials.basic(
                    config.getUsername() == null ? "" : config.getUsername(),
                    config.getTicket() == null ? "" : config.getTicket()))
                .get();

        final Call call = client.newCall(reqBuilder.build());
        final Response response = call.execute();
        if (response.code() == 404) {
            reqBuilder.url(config.getUri().toASCIIString() + "/api/v1/version");
            final Call call1 = client.newCall(reqBuilder.build());
            final Response response1 = call1.execute();
            if (response1.code() == 200) {
                return 1.0f;
            }
            // Not a swarm server
            throw new InvalidSwarmServerException("Server " + config.getUri()
                    + " does not respond to Swarm version requests");
        } else if (response.code() == 200) {
            // Valid Swarm server.
            JsonElement versionResponse = new JsonParser().parse(response.body().charStream());
            if (versionResponse.isJsonObject()
                    && versionResponse.getAsJsonObject().has("apiVersions")
                    && versionResponse.getAsJsonObject().get("apiVersions").isJsonArray()) {
                JsonArray versions =
                        versionResponse.getAsJsonObject().get("apiVersions").getAsJsonArray();
                float highest = 1.1f;
                for (int i = 0; i < versions.size(); i++) {
                    Number version = versions.get(i).getAsNumber();
                    if (version != null && version.floatValue() > highest) {
                        highest = version.floatValue();
                    }
                }
                return highest;
            }
            throw new InvalidSwarmServerException("Server " + config.getUri()
                + " did not return valid response with a version request.");
        }

        // Not supported
        throw new InvalidSwarmServerException("Server " + config.getUri()
            + " does not appear to be a valid Swarm server.");
    }
}
