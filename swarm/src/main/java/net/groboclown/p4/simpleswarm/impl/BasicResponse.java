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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.groboclown.p4.simpleswarm.SwarmLogger;
import net.groboclown.p4.simpleswarm.SwarmVersion;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

class BasicResponse {
    private final SwarmVersion version;
    private final int code;
    private final String reason;
    private final String body;
    private final String error;

    BasicResponse(SwarmLogger logger, SwarmVersion version, HttpResponse response)
            throws IOException {
        this.version = version;
        this.code = response.getStatusLine().getStatusCode();
        this.reason = response.getStatusLine().getReasonPhrase();
        this.body = read(response.getEntity());
        this.error = getBodyError(logger, this.code, this.body);
    }

    BasicResponse(SwarmLogger logger, SwarmVersion version, int code, String reason, String body) {
        this.version = version;
        this.code = code;
        this.reason = reason;
        this.body = body;
        this.error = getBodyError(logger, code, body);
    }

    int getStatusCode() {
        return code;
    }

    JsonObject getBodyAsJson()
            throws SwarmServerResponseException {
        try {
            JsonElement el = JsonUtil.parse(body);
            if (el.isJsonObject()) {
                return el.getAsJsonObject();
            }
            throw new SwarmServerResponseException("Swarm server did not return JSON object.");
        } catch (JsonSyntaxException e) {
            throw new SwarmServerResponseException("Swarm did not return a JSON object", e);
        }
    }

    private Gson buildGson() {
        final GsonBuilder gson = new GsonBuilder();
        gson.setVersion(version.asFloat());
        return gson.create();
    }

    private static String read(HttpEntity entity)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader in = new InputStreamReader(
                entity.getContent(), getEncoding(entity))) {
            char[] buff = new char[4096];
            int len;
            while ((len = in.read(buff, 0, 4096)) > 0) {
                sb.append(buff, 0, len);
            }
        }
        return sb.toString();
    }

    private static Charset getEncoding(HttpEntity entity) {
        Header header = entity.getContentEncoding();
        if (header == null) {
            return Charset.forName("UTF-8");
        }
        for (HeaderElement headerElement : header.getElements()) {
            for (NameValuePair pair : headerElement.getParameters()) {
                if (pair != null && pair.getValue() != null) {
                    if (Charset.isSupported(pair.getValue())) {
                        return Charset.forName(pair.getValue());
                    }
                }
            }
        }
        return Charset.forName("UTF-8");
    }

    SwarmServerResponseException getResponseException(String object, String action, int code) {
        return new SwarmServerResponseException(
                "Swarm server failed during " + action + " of " + object + ": " + (error == null ? reason : error),
                object, action, code);
    }

    private static String getBodyError(SwarmLogger logger, int code, String body) {
        if (code >= 400) {
            logger.warn("Swarm error response: " + body);
            JsonObject json = null;
            try {
                JsonElement el = JsonUtil.parse(body);
                if (el.isJsonObject()) {
                    json = el.getAsJsonObject();
                }
            } catch (SwarmServerResponseException e) {
                // Ignore error
                json = null;
            }
            if (json != null && json.has("error")) {
                return json.get("error").toString();
            } else {
                return null;
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Swarm data response: " + body);
            }
            return null;
        }
    }
}
