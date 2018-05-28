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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.lang.UrlClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClassLoaderUtil {
    private static final Logger LOG = Logger.getInstance(ClassLoaderUtil.class);


    private ClassLoaderUtil() {
        // unused
    }


    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> Class<T> loadClass(@NotNull String className, @Nullable ClassLoader classLoader)
            throws ClassNotFoundException {
        if (classLoader == null) {
            // Attempt using the classloader that loaded a plugin class, so that the same
            // classloader is used to load the other plugin classes.
            classLoader = ClassLoaderUtil.class.getClassLoader();
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    LOG.debug("Using system classloader");
                    classLoader = ClassLoader.getSystemClassLoader();
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Using thread classloader " + classLoader);
                    if (classLoader instanceof UrlClassLoader) {
                        LOG.debug("ClassLoader URLs = " + ((UrlClassLoader) classLoader).getBaseUrls() +
                                "; " + ((UrlClassLoader) classLoader).getUrls());
                    }
                }
            } else {
                LOG.debug("Using plugin class classloader");
            }
        } else {
            LOG.debug("Using passed-in classloader");
        }
        if (classLoader == null) {
            LOG.debug("No classloader found");
            return (Class<T>) Class.forName(className);
        }
        try {
            return (Class<T>) classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                if (classLoader instanceof UrlClassLoader) {
                    UrlClassLoader url = (UrlClassLoader) classLoader;
                    LOG.debug("No class " + className + " in URLs " + url.getUrls());
                } else {
                    LOG.debug("No class " + className + " in class loader " + classLoader);
                }
            }
            throw e;
        }
    }
}
