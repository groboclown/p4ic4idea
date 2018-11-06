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

import net.groboclown.p4.simpleswarm.SwarmConfig;
import net.groboclown.p4.simpleswarm.exceptions.UnauthorizedAccessException;
import net.groboclown.p4.simpleswarm.impl.http.HttpClientRequester;
import net.groboclown.p4.simpleswarm.impl.http.HttpClientRequesterImpl;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Simple request / response low-level API.
 */
class BasicRequest {
    private static final String API_PATH = "/api/";
    private final HttpClientRequester http;

    BasicRequest() {
        this(new HttpClientRequesterImpl());
    }

    BasicRequest(HttpClientRequester httpClientRequester) {
        this.http = httpClientRequester;
    }

    BasicResponse get(SwarmConfig config, String path)
            throws IOException, UnauthorizedAccessException {
        return get(config, path, null);
    }

    BasicResponse get(SwarmConfig config, String path, Map<String, ?> query)
            throws IOException, UnauthorizedAccessException {
        final String url = toUrl(config, path, query);
        if (url == null) {
            throw new IOException("No swarm URL in configuration");
        }
        final HttpGet request = new HttpGet(url);
        return request(config, request);
    }

    BasicResponse postJson(SwarmConfig config, String path, String body)
            throws IOException, UnauthorizedAccessException {
        final String url = toUrl(config, path, null);
        final HttpPost request = new HttpPost(url);
        final HttpEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        return request(config, request);
    }

    BasicResponse postForm(SwarmConfig config, String path, Map<String, Object> form)
            throws IOException, UnauthorizedAccessException {
        final String url = toUrl(config, path, null);
        final HttpPost request = new HttpPost(url);
        final HttpEntity entity = new StringEntity(toFormBody(form), ContentType.APPLICATION_FORM_URLENCODED);
        request.setEntity(entity);
        return request(config, request);
    }

    BasicResponse patch(SwarmConfig config, String path, String body)
            throws IOException, UnauthorizedAccessException {
        throw new IOException("unsupported operation at the moment");
    }


    private static String toFormBody(Map<String, ?> form)
            throws UnsupportedEncodingException {
        StringBuilder ret = toQuery(form);
        if (ret.length() > 0 && ret.charAt(0) == '?') {
            ret.deleteCharAt(0);
        }
        return ret.toString();
    }


    private BasicResponse request(SwarmConfig config, HttpUriRequest request)
            throws IOException, UnauthorizedAccessException {
        CredentialsProvider provider = new BasicCredentialsProvider();
        config.getLogger().debug("Request to server with " + config.getUsername() + ": " + request);
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(config.getUsername(), config.getTicket());
        provider.setCredentials(AuthScope.ANY, credentials);
        AuthCache authCache = new BasicAuthCache();
        authCache.put(getHost(request), new BasicScheme());

        // Add AuthCache to the execution context
        final HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(provider);
        context.setAuthCache(authCache);

        BasicResponse ret = http.execute(request, context, provider, (HttpResponse response) -> {
            if (response.getStatusLine().getStatusCode() == 401) {
                throw new UnauthorizedAccessException(response.getStatusLine().getReasonPhrase(),
                        response.getStatusLine().getStatusCode());
            }
            return new BasicResponse(config.getLogger(), config.getVersion(), response);
        });
        if (config.getLogger().isDebugEnabled()) {
            config.getLogger().debug("Request for " + request.getURI() + " returned " +
                    ret.getStatusCode() + ": " + ret.getRawBody());
        }
        return ret;
    }


    private static HttpHost getHost(HttpUriRequest request) {
        URI uri = request.getURI();
        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }


    // Swarm supports 2 array types: a comma separated list, and a series of
    // the same argument.  For the final type,
    static StringBuilder toQuery(Map<String, ?> query)
            throws UnsupportedEncodingException {
        StringBuilder ret = new StringBuilder();
        if (query != null && query.size() > 0) {
            char next = '?';
            for (Map.Entry<String, ?> entry : query.entrySet()) {
                Collection<String> vals = toEncodedValues(entry.getKey(), entry.getValue());
                for (String val : vals) {
                    ret
                        .append(next)
                        .append(urlEncode(entry.getKey()))
                        .append('=')
                        .append(val);
                    next = '&';
                }
            }
        }
        return ret;
    }


    static Collection<String> toEncodedValues(String key, Object val)
            throws UnsupportedEncodingException {
        if (val == null) {
            throw new NullPointerException(key);
        }
        if (val instanceof Number || val instanceof String) {
            return Collections.singleton(urlEncode(val.toString()));
        }
        boolean multiValue = key.endsWith("[]");
        if (val instanceof Collection) {
            List<String> ret = new ArrayList<String>();
            int i = 0;
            for (Object o : (Collection) val) {
                if (o == null) {
                    throw new NullPointerException(key + '[' + i + ']');
                } else if (o instanceof Number || o instanceof String) {
                    if (multiValue) {
                        ret.add(urlEncode(o.toString()));
                    } else {
                        ret.add(o.toString());
                    }
                } else {
                    throw new IllegalArgumentException(key + '[' + i + "]: " + val.getClass() + "=" + val);
                }
                i++;
            }
            if (! multiValue) {
                StringBuilder sb = new StringBuilder();
                String join = "";
                for (String s : ret) {
                    sb.append(join).append(s);
                    join = ",";
                }
                return Collections.singleton(urlEncode(sb.toString()));
            }
            return ret;
        }
        if (val instanceof Array) {
            List<String> ret = new ArrayList<String>();
            for (int i = 0; i < Array.getLength(val); i++) {
                Object o = Array.get(val, i);
                if (o == null) {
                    throw new NullPointerException(key + '[' + i + ']');
                } else if (o instanceof Number || o instanceof String) {
                    if (multiValue) {
                        ret.add(urlEncode(o.toString()));
                    } else {
                        ret.add(o.toString());
                    }
                } else {
                    throw new IllegalArgumentException(key + '[' + i + "]: " + val.getClass() + "=" + val);
                }
                i++;
            }
            if (! multiValue) {
                StringBuilder sb = new StringBuilder();
                String join = "";
                for (String s : ret) {
                    sb.append(join).append(s);
                    join = ",";
                }
                return Collections.singleton(urlEncode(sb.toString()));
            }
            return ret;
        }
        throw new IllegalArgumentException(key + ": " + val.getClass() + "=" + val);
    }


    static String toUrl(SwarmConfig config, String path, Map<String, ?> query)
            throws UnsupportedEncodingException {
        if (config == null || path == null || config.getUri() == null) {
            return null;
        }
        return config.getUri().toASCIIString() + API_PATH + config.getVersionPath() + path +
                toQuery(query);
    }


    private static String urlEncode(String value)
            throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }
}
