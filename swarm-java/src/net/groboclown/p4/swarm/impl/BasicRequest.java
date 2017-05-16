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
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Simple request / response low-level API.
 */
public class BasicRequest {
    private static final String API_PATH = "/api/";

    public static BasicResponse get(SwarmConfig config, String path)
            throws IOException {
        return get(config, path, null);
    }

    public static BasicResponse get(SwarmConfig config, String path, Map<String, String> query)
            throws IOException {
        String url = toUrl(config, path, query);
        HttpGet request = new HttpGet(url);
        return request(config, request);
    }


    private static BasicResponse request(SwarmConfig config, HttpUriRequest request)
            throws IOException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(config.getUsername(), config.getTicket());
        provider.setCredentials(AuthScope.ANY, credentials);

        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider)
                .build();
        try {
            HttpResponse response = client.execute(request);
            return new BasicResponse(config.getVersion(), response);
        } finally {
            HttpClientUtils.closeQuietly(client);
        }
    }



    public static String toUrl(SwarmConfig config, String path, Map<String, String> query)
            throws UnsupportedEncodingException {
        StringBuilder ret = new StringBuilder(config.getUri().toASCIIString());
        ret.append(API_PATH).append(config.getVersionPath()).append(path);
        if (query != null) {
            char next = '?';
            for (Map.Entry<String, String> entry : query.entrySet()) {
                ret.append(next);
                ret.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                next = '&';
            }
        }
        return ret.toString();
    }
}
