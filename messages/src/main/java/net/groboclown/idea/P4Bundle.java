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
package net.groboclown.idea;

import com.intellij.CommonBundle;
import com.intellij.openapi.application.ApplicationNamesInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public class P4Bundle {
    private static Reference<ResourceBundle> ourBundle;

    @NonNls
    public static final String BUNDLE =
            //P4Bundle.class.getName();
            "net.groboclown.idea.P4Bundle";

    private P4Bundle() {
    }

    @NonNls
    public static String message(
            @Nls
            @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    @NonNls
    public static String getString(
            @Nls
            @PropertyKey(resourceBundle = BUNDLE) String key) {
        return getBundle().getString(key);
    }

    @NonNls
    public static String applicationName() {
        return ApplicationNamesInfo.getInstance().getFullProductName();
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = null;
        if (ourBundle != null) bundle = ourBundle.get();
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            ourBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }
}
