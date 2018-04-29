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
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class JreSettingsTest {

    @Test
    void getEnv() {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            JreSettings.setOverrides((Map<String, String>) null);
            assertEquals(entry.getValue(), JreSettings.getEnv(entry.getKey()));
            JreSettings.setOverrides(new Pair<>(entry.getKey(), "--" + entry.getValue()));
            assertEquals("--" + entry.getValue(), JreSettings.getEnv(entry.getKey()));
            JreSettings.setOverrides(new Pair<>(entry.getKey(), null));
            assertNull(JreSettings.getEnv(entry.getKey()));
            assertEquals("abc", JreSettings.getEnv(entry.getKey(), "abc"));
        }

        JreSettings.setOverrides((Map<String, String>) null);

        int i = 0;
        String notExist;
        do {
            i++;
            notExist = "not-exist-" + i;
        } while (env.containsKey(notExist));
        assertNull(JreSettings.getEnv(notExist));
        assertEquals("abc", JreSettings.getEnv(notExist, "abc"));
    }

    @Test
    void getProperty() {
        Properties props = System.getProperties();
        for (String key : props.stringPropertyNames()) {
            JreSettings.setOverrides((Map<String, String>) null);
            String value = props.getProperty(key);
            assertEquals(value, JreSettings.getProperty(key));
            JreSettings.setOverrides(new Pair<>(key, "--" + value));
            assertEquals("--" + value, JreSettings.getProperty(key));
            JreSettings.setOverrides(new Pair<>(key, null));
            assertNull(JreSettings.getProperty(key));
            assertEquals("abc", JreSettings.getProperty(key, "abc"));
        }

        JreSettings.setOverrides((Map<String, String>) null);

        int i = 0;
        String notExist;
        do {
            i++;
            notExist = "not-exist-" + i;
        } while (props.getProperty(notExist) != null);
        assertNull(JreSettings.getProperty(notExist));
        assertEquals("abc", JreSettings.getProperty(notExist, "abc"));
    }
}
