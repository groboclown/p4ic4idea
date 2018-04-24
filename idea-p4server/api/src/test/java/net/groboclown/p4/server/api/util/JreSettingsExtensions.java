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

import net.groboclown.p4.server.api.util.JreSettings;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class JreSettingsExtensions
        implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    @Override
    public void afterTestExecution(ExtensionContext extensionContext)
            throws Exception {
        setOverrides(null);
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext)
            throws Exception {
        setOverrides(null);
    }


    public static void setOverrides(@Nullable Map<String, String> overrides) {
        JreSettings.setOverrides(overrides);
    }


    public static void setOverrideValues(String... keyValuePairs) {
        assert keyValuePairs.length % 2 == 0 : "must be in the format key, value, key, value, ...";
        Map<String, String> overrides = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            overrides.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        setOverrides(overrides);
    }
}
