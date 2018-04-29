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

package net.groboclown.p4.server.api.util;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Values stored in the JRE classes that can be troublesome to extract,
 * and even more troublesome to mock out.
 */
public class JreSettings {
    private final static Map<String, String> OVERRIDES = new HashMap<>();

    @NonNls
    @Nullable
    public static String getEnv(@NotNull @NonNls String key) {
        if (OVERRIDES.containsKey(key)) {
            // even if null!
            return OVERRIDES.get(key);
        }
        return System.getenv(key);
    }

    @NonNls
    @Nullable
    public static String getEnv(@NotNull @NonNls String key, @Nullable String defaultValue) {
        if (OVERRIDES.containsKey(key)) {
            String ret = OVERRIDES.get(key);
            if (ret == null) {
                ret = defaultValue;
            }
            return ret;
        }
        String ret = System.getenv(key);
        if (ret == null) {
            ret = defaultValue;
        }
        return ret;
    }

    @NonNls
    @Nullable
    public static String getProperty(@NotNull @NonNls String key) {
        if (OVERRIDES.containsKey(key)) {
            // even if null!
            return OVERRIDES.get(key);
        }
        return System.getProperty(key);
    }

    @NonNls
    @Nullable
    public static String getProperty(@NotNull @NonNls String key, @Nullable String defaultValue) {
        if (OVERRIDES.containsKey(key)) {
            String ret = OVERRIDES.get(key);
            if (ret == null) {
                ret = defaultValue;
            }
            return ret;
        }
        return System.getProperty(key, defaultValue);
    }

    @TestOnly
    static void setOverrides(@Nullable Map<String, String> overrides) {
        OVERRIDES.clear();
        if (overrides != null) {
            OVERRIDES.putAll(overrides);
        }
    }

    @SafeVarargs
    @TestOnly
    static void setOverrides(@NotNull Pair<String, String>... pairs) {
        OVERRIDES.clear();
        for (Pair<String, String> pair : pairs) {
            OVERRIDES.put(pair.first, pair.second);
        }
    }

}
