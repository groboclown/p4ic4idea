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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.groboclown.p4.simpleswarm.exceptions.ResponseFormatException;
import net.groboclown.p4.simpleswarm.exceptions.SwarmServerResponseException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    private static final JsonParser PARSER = new JsonParser();

    private JsonUtil() {
        // do nothing
    }

    public static JsonElement parse(String s)
            throws SwarmServerResponseException {
        try {
            JsonElement el = PARSER.parse(s);
            if (el != null) {
                return el;
            }
            throw new SwarmServerResponseException("Swarm did not return an object: " + s);
        } catch (JsonSyntaxException e) {
            throw new SwarmServerResponseException("Swarm did not return a JSON object: " + s, e);
        }
    }

    public static int getIntKey(JsonObject obj, String key)
            throws ResponseFormatException {
        if (obj == null || ! obj.has(key)) {
            throw new ResponseFormatException(key, obj);
        }
        JsonElement val = obj.get(key);
        if (! val.isJsonPrimitive()) {
            throw new ResponseFormatException(key, val);
        }
        try {
            return val.getAsInt();
        } catch (NumberFormatException e) {
            throw new ResponseFormatException(key, val, e);
        }
    }

    public static String[] getNullableStringArrayKey(JsonObject obj, String key)
            throws ResponseFormatException {
        JsonArray vay = getNullableArrayKey(obj, key);
        if (vay == null) {
            return null;
        }
        List<String> ret = new ArrayList<>(vay.size());
        for (int i = 0; i < vay.size(); i++) {
            JsonElement vi = vay.get(i);
            if (vi.isJsonNull()) {
                ret.add(null);
            } else if (vi.isJsonPrimitive()) {
                ret.add(vi.getAsString());
            } else {
                throw new ResponseFormatException(key + '[' + i + ']', vi);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    public static String getNullableStringKey(JsonObject obj, String key)
            throws ResponseFormatException {
        JsonElement val = getNullableKey(obj, key);
        if (val == null) {
            return null;
        }
        if (val.isJsonPrimitive()) {
            return val.getAsString();
        }
        throw new ResponseFormatException(key, val);
    }

    public static int[] getNullableIntArrayKey(JsonObject obj, String key)
            throws ResponseFormatException {
        JsonArray vay = getNullableArrayKey(obj, key);
        if (vay == null) {
            return null;
        }
        int[] ret = new int[vay.size()];
        for (int i = 0; i < vay.size(); i++) {
            JsonElement vi = vay.get(i);
            if (vi.isJsonPrimitive()) {
                try {
                    ret[i] = vi.getAsInt();
                } catch (NumberFormatException e) {
                    throw new ResponseFormatException(key + '[' + i + ']', vi, e);
                }
            } else {
                throw new ResponseFormatException(key + '[' + i + ']', vi);
            }
        }
        return ret;
    }

    public static Date getNullableTimestampKey(JsonObject obj, String key)
            throws ResponseFormatException {
        JsonElement val = getNullableKey(obj, key);
        if (val == null) {
            return null;
        }
        if (val.isJsonPrimitive()) {
            try {
                long v = val.getAsLong();
                return new Date(v);
            } catch (NumberFormatException e) {
                throw new ResponseFormatException(key, val, e);
            }
        }
        throw new ResponseFormatException(key, val);
    }

    public static JsonArray getNullableArrayKey(JsonObject obj, String key)
            throws ResponseFormatException {
        JsonElement val = getNullableKey(obj, key);
        if (val == null) {
            return null;
        }
        if (val.isJsonArray()) {
            return val.getAsJsonArray();
        }
        throw new ResponseFormatException(key, val);
    }


    public static Map<String, JsonElement> getNullableMapKey(JsonObject obj, String key)
            throws ResponseFormatException {
        Map<String, JsonElement> ret = new HashMap<>();
        JsonElement el = getNullableKey(obj, key);
        if (el == null) {
            return ret;
        }
        if (! el.isJsonObject()) {
            throw new ResponseFormatException(key, el);
        }
        JsonObject jo = el.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
            ret.put(entry.getKey(), entry.getValue());
        }
        return ret;
    }


    public static boolean getNullableBooleanKey(JsonObject obj, String key, boolean defaultValue)
            throws ResponseFormatException {
        JsonElement val = getNullableKey(obj, key);
        if (val == null) {
            return defaultValue;
        }
        if (val.isJsonPrimitive()) {
            return val.getAsBoolean();
        }
        throw new ResponseFormatException(key, val);
    }


    private static JsonElement getNullableKey(JsonObject obj, String key) {
        if (obj == null || ! obj.has(key)) {
            return null;
        }
        JsonElement val = obj.get(key);
        if (val == null || val.isJsonNull()) {
            return null;
        }
        return val;
    }
}
