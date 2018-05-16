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

package net.groboclown.p4.server.impl.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClassLoaderUtil {
    private ClassLoaderUtil() {
        // unused
    }


    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> Class<T> loadClass(@NotNull String className, @Nullable ClassLoader classLoader)
            throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
        }
        if (classLoader == null) {
            return (Class<T>) Class.forName(className);
        }
        return (Class<T>) classLoader.loadClass(className);
    }
}
