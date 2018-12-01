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

package net.groboclown.idea.p4ic.compat;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

// Can't use P4Bundle, because it's in a child project.
public class CompatBundle {
    private static Reference<ResourceBundle> ourBundle;

    @NonNls
    private static final String BUNDLE =
            //P4Bundle.class.getName();
            "net.groboclown.idea.p4ic.compat.CompatBundle";

    private CompatBundle() {
    }

    public static String message(
            @NonNls
            @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    public static String getString(
            @NonNls
            @PropertyKey(resourceBundle = BUNDLE) String key) {
        return getBundle().getString(key);
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
