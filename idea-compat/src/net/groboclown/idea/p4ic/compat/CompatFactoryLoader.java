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

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class to aid in loading Idea compatible
 * {@link CompatFactory} instances.
 */
public class CompatFactoryLoader {
    private static final Logger LOG = Logger.getInstance(CompatFactoryLoader.class);
    private static final Object sync = new Object();
    private static CompatManager manager;

    // TODO move these into a text file
    private static final String[] FACTORY_CLASSES = {
            "net.groboclown.idea.p4ic.compat.idea135.CompatFactory135",
            "net.groboclown.idea.p4ic.compat.idea140.CompatFactory140"
    };


    @NotNull
    public static CompatManager getInstance() {
        synchronized (sync) {
            if (manager == null) {
                manager = loadCompatFactory(FACTORY_CLASSES).createCompatManager();
            }
        }
        return manager;
    }


    public static boolean isSupported() {
        // FIXME debugging
        return false;

        //try {
        //    // always non-null
        //    getInstance();
        //    return true;
        //} catch (Exception e) {
        //    return false;
        //}
    }


    /**
     * Given a set of {@link CompatFactory} class names, find the one
     * that is best compatible with the currently running IDE.
     *
     * @param classNames list of class names for concrete implementations
     *                   of {@link CompatFactory}.
     * @return best matching factory
     * @throws IllegalStateException if none of the class names are compatible
     *         with this IDE.
     */
    @NotNull
    public static CompatFactory loadCompatFactory(String[] classNames) {
        CompatFactory factory = loadCompatFactory(getApiVersion(), classNames, findClassLoaders());
        if (factory == null) {
            throw new IllegalStateException("IDE version " + getApiVersion() + " not compatible with the P4 plugin");
        }
        return factory;
    }

    @Nullable
    private static CompatFactory loadCompatFactory(@NotNull String apiVersion, @NotNull String[] classNames,
                                                   ClassLoader[] loaders) {
        CompatFactory best = null;
        for (String className: classNames) {
            boolean everFound = false;
            for (ClassLoader loader: loaders) {
                CompatFactory factory = loadClass(className, loader);
                everFound |= factory != null;
                if (isCompatible(apiVersion, factory) && isBetterVersion(best, factory)) {
                    best = factory;
                }
            }
            if (! everFound) {
                LOG.warn("Could not load compatibility class " + className);
            }
        }
        return best;
    }

    @Nullable
    private static CompatFactory loadClass(String className, ClassLoader cl) {
        if (cl == null) {
            return null;
        }

        try {
            Class<?> c = cl.loadClass(className);
            if (CompatFactory.class.isAssignableFrom(c)) {
                return CompatFactory.class.cast(c.newInstance());
            }
            LOG.error("Not a CompatFactory: " + className);
            return null;
        } catch (Exception e) {
            // These are fine - it probably means that the class loader
            // was wrong.
            LOG.debug("CompatFactory can't be loaded: " + className, e);
            return null;
        } catch (NoClassDefFoundError e) {
            LOG.info("CompatFactory can't be loaded: " + className, e);
            return null;
        }
    }

    @NotNull
    private static String getApiVersion() {
        return ApplicationInfo.getInstance().getApiVersion();
    }


    @NotNull
    private static ClassLoader[] findClassLoaders() {
        return new ClassLoader[] {
                Thread.currentThread().getContextClassLoader(),
                CompatFactoryLoader.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
    }



    private static boolean isCompatible(@NotNull String apiVersion, @Nullable CompatFactory factory) {
        /*
        return factory != null &&
                compareIdeaVersionNumbers(
                        apiVersion,
                        factory.getMinCompatibleApiVersion()) >= 0 &&
                compareIdeaVersionNumbers(
                        apiVersion,
                        factory.getMaxCompatibleApiVersion()) <= 0;
        */
        if (factory == null) {
            return false;
        }
        int minCompatibility = compareIdeaVersionNumbers(
                factory.getMinCompatibleApiVersion(),
                apiVersion);
        int maxCompatibility = compareIdeaVersionNumbers(
                factory.getMaxCompatibleApiVersion(),
                apiVersion);
        // we want the min to be greater than the api,
        // and we want the max to be smaller than the api.
        return minCompatibility >= 0 && maxCompatibility <= 0;
    }

    private static boolean isBetterVersion(@Nullable CompatFactory best, @Nullable CompatFactory factory) {
        return factory != null && (best == null || compareIdeaVersionNumbers(
                best.getMinCompatibleApiVersion(),
                factory.getMinCompatibleApiVersion()) > 0);
    }

    private static int compareIdeaVersionNumbers(String first, String second) {
        // Version can be "IC-version.number" (IC meaning the Community edition)
        if (first.indexOf('-') > 0) {
            first = first.substring(first.indexOf('-') + 1);
        }
        if (second.indexOf('-') > 0) {
            second = second.substring(second.indexOf('-') + 1);
        }
        try {
            final int pos1 = first.indexOf('.');
            final int part11;
            final int part12;
            if (pos1 >= 0) {
                part11 = Integer.parseInt(first.substring(0, pos1));
                part12 = Integer.parseInt(first.substring(pos1 + 1));
            } else {
                part11 = Integer.parseInt(first);
                part12 = 0;
            }
            final int pos2 = second.indexOf('.');
            final int part21;
            final int part22;
            if (pos2 >= 0) {
                part21 = Integer.parseInt(second.substring(0, pos2));
                part22 = Integer.parseInt(second.substring(pos2 + 1));
            } else {
                part21 = Integer.parseInt(second);
                part22 = 0;
            }

            if (part11 > part21) {
                return -1;
            } else if (part11 < part21) {
                return 1;
            } else if (part12 > part22) {
                return -1;
            } else if (part12 < part22) {
                return 1;
            } else {
                return 0;
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid IDEA version number (" + first + " vs " + second + ")", e);
        }
    }

}
