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

package net.groboclown.p4.swarm.util;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.http.RealResponseBody;
import net.groboclown.p4.swarm.SwarmConfig;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockClient
        extends OkHttpClient {
    private final Map<Request, Response.Builder> responses = new HashMap<Request, Response.Builder>();
    private final Map<Request, IOException> errors = new HashMap<Request, IOException>();
    private final Response.Builder notFoundResponse = new Response.Builder()
            .code(404)
            .message("Not found");

    public static VersionRequestBuilder multiVersionRequest(SwarmConfig config, String path, String... versions) {
        String full = config.getUri().toASCIIString();
        if (! full.endsWith("/")) {
            full = full + '/';
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new VersionRequestBuilder(full, path, versions)
                .header("Accept", "application/json")
                .header("Authorization", Credentials.basic(
                        config.getUsername() == null ? "" : config.getUsername(),
                        config.getTicket() == null ? "" : config.getTicket()));
    }

    public static Request.Builder basicRequest(SwarmConfig config, String path) {
        String full = config.getUri().toASCIIString();
        if (! full.endsWith("/")) {
            full = full + '/';
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        full += path;
        return new Request.Builder()
                .header("Accept", "application/json")
                .header("Authorization", Credentials.basic(
                        config.getUsername() == null ? "" : config.getUsername(),
                        config.getTicket() == null ? "" : config.getTicket()))
                .url(full)
                ;
    }

    public static MockResponseBuilder single(Request.Builder req) {
        MockClient ret = new MockClient();
        return ret.when(req);
    }

    public static VersionResponseBuilder single(VersionRequestBuilder req) {
        MockClient ret = new MockClient();
        return ret.when(req);
    }

    public MockResponseBuilder when(Request.Builder req) {
        Request request = req.build();
        MockResponseBuilder response = new MockResponseBuilder(this, request);
        responses.put(request, response.response);
        return response;
    }

    public VersionResponseBuilder when(VersionRequestBuilder req) {
        VersionResponseBuilder response = new VersionResponseBuilder(req);
        for (Map.Entry<String, Response.Builder> entry : response.responses.entrySet()) {
            responses.put(req.build(entry.getKey()), entry.getValue());
        }
        return response;
    }

    public Call newCall(Request request) {
        final Response.Builder response = getValueFor(request, responses);
        if (response != null) {
            return new MockCall(this, request, response.build());
        }
        final IOException error = getValueFor(request, errors);
        if (error != null) {
            return new MockCall(this, request, error);
        }
        return new MockCall(this, request,
                notFoundResponse.request(request).protocol(Protocol.HTTP_1_1).build());
        // throw new IllegalStateException("No such registered request: " + request);
    }

    private <T> T getValueFor(Request req, Map<Request, T> map) {
        for (Map.Entry<Request, T> entry : map.entrySet()) {
            if (matches(req, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean matches(Request a, Request b) {
        return
            matches(a.urlString(), b.urlString())
            && matches(a.method(), b.method())
            && matches(a.headers(), b.headers());
    }

    private static boolean matches(String a, String b) {
        return (a == null && b == null)
                || (a != null && a.equals(b));
    }

    private static boolean matches(Headers a, Headers b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        Set<String> bkeys = new HashSet<String>();
        for (String bkey : b.names()) {
            bkeys.add(bkey.toLowerCase());
        }
        for (String key : a.names()) {
            if (! bkeys.remove(key.toLowerCase())) {
                return false;
            }
            String av = a.get(key);
            String bv = b.get(key);
            if ((av == null || bv == null) && !(av == null && bv == null)) {
                return false;
            } else if (av != null && ! av.equals(bv)) {
                return false;
            }
        }
        return bkeys.isEmpty();
    }

    private static class MockCall extends Call {
        private final Response response;
        private final IOException exception;

        MockCall(MockClient client, Request originalRequest, Response response) {
            super(client, originalRequest);
            this.response = response;
            this.exception = null;
        }

        MockCall(MockClient client, Request originalRequest, IOException err) {
            super(client, originalRequest);
            this.response = null;
            this.exception = err;
        }

        @Override
        public Response execute() throws IOException {
            if (this.exception != null) {
                throw this.exception;
            }
            return response;
        }
    }

    public class ForRequest {
        private final Response.Builder builder;

        ForRequest(Response.Builder builder) {
            this.builder = builder;
        }

        public Response.Builder use() {
            return builder;
        }
    }

    public static class VersionRequestBuilder {
        private final Map<String, Request.Builder> versions;
        private final Map<String, Request> built = new HashMap<String, Request>();

        private VersionRequestBuilder(String baseUri, String path, String... versions) {
            this.versions = new HashMap<String, Request.Builder>();
            for (String version : versions) {
                Request.Builder req = new Request.Builder()
                    .url(baseUri + "api/v" + version + "/" + path);
                this.versions.put(version, req);
            }
        }

        public VersionRequestBuilder header(String name, String value) {
            for (Request.Builder builder : versions.values()) {
                builder.header(name, value);
            }
            return this;
        }

        private Request build(String version) {
            Request ret = built.get(version);
            if (ret == null) {
                ret = versions.get(version).build();
                built.put(version, ret);
            }
            return ret;
        }
    }

    public static class VersionResponseBuilder {
        private final Map<String, Response.Builder> responses;

        private VersionResponseBuilder(VersionRequestBuilder req) {
            this.responses = new HashMap<String, Response.Builder>();
            for (Map.Entry<String, Response.Builder> entry : responses.entrySet()) {
                responses.put(entry.getKey(),
                        new Response.Builder()
                        .request(req.build(entry.getKey()))
                        .protocol(Protocol.HTTP_1_1));
            }
        }
    }

    public static class MockResponseBuilder {
        private final MockClient client;
        private final Response.Builder response;

        private MockResponseBuilder(MockClient client, Request originalRequest) {
            this.client = client;
            this.response = new Response.Builder()
                .request(originalRequest)
                .protocol(Protocol.HTTP_1_1);
        }

        public MockClient getMockClient() {
            return this.client;
        }

        public MockResponseBuilder body(String text) {
            final Headers headers = response.build().headers();
            final BufferedSource source = Okio.buffer(new ByteArraySource(text));
            final ResponseBody body = new RealResponseBody(headers, source);
            response.body(body);
            return this;
        }

        public MockResponseBuilder status(int code) {
            response.code(code);
            switch (code) {
                case 200:
                    response.message("OK");
                    break;
                case 404:
                    response.message("NOT FOUND");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown code " + code);
            }
            return this;
        }

        public MockResponseBuilder simpleJson(String text) {
            // Allows for simplified simpleJson embedded in a Java string
            String json = text
                    .replace("\"", "\\\"")
                    .replace('\'', '"');
            return body(json);
        }
    }

    public static class ByteArraySource
            implements Source {
        private final byte[] source;
        private final Timeout timeout = new Timeout();
        private boolean hasRead = false;

        ByteArraySource(String source) {
            this.source = source.getBytes();
        }

        @Override
        public long read(Buffer sink, long byteCount)
                throws IOException {
            if (hasRead) {
                return 0;
            }
            hasRead = true;
            sink.write(source);
            return source.length;
        }

        @Override
        public Timeout timeout() {
            return timeout;
        }

        @Override
        public void close()
                throws IOException {
            // do nothing
        }
    }
}
