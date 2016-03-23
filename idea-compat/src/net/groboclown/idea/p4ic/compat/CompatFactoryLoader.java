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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Utility class to aid in loading Idea compatible
 * {@link CompatFactory} instances.
 */
public class CompatFactoryLoader {
    private static final Logger LOG = Logger.getInstance(CompatFactoryLoader.class);
    private static final Object sync = new Object();

    // Used for unit test situations, when we don't have access
    // to the application api version.
    private static final String DEFAULT_API_VERSION = "135.1286";
    private static String cachedApiVersion;

    private static CompatManager manager;


    @NotNull
    public static CompatManager getInstance() {
        synchronized (sync) {
            if (manager == null) {
                manager = loadCompatFactory().createCompatManager();
            }
        }
        return manager;
    }


    public static boolean isSupported() {
        try {
            // always non-null
            getInstance();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Given a set of {@link CompatFactory} class names, find the one
     * that is best compatible with the currently running IDE.
     *
     * @return best matching factory
     * @throws IllegalStateException if none of the class names are compatible
     *         with this IDE.
     */
    @NotNull
    public static CompatFactory loadCompatFactory() {
        CompatFactory factory = loadCompatFactory(getApiVersion(), findClassLoaders());
        if (factory == null) {
            throw new IncompatibleApiVersionException(getApiVersion());
        }
        return factory;
    }

    @Nullable
    private static CompatFactory loadCompatFactory(@NotNull String apiVersion, ClassLoader[] loaders) {
        CompatFactory best = null;
        for (ClassLoader loader: loaders) {
            try {
                for (CompatFactory factory : ServiceLoader.load(CompatFactory.class, loader)) {
                    if (isCompatible(apiVersion, factory) && isBetterVersion(best, factory)) {
                        best = factory;
                    }
                }
            } catch (ServiceConfigurationError e) {
                LOG.error(e);
            } catch (LinkageError e) {
                // bug #113
                // The IDE classes aren't matching up with the expected signatures,
                // the classes are compiled with an incompatible JRE,
                // or any number of other issues that shouldn't stop
                // the plugin from loading.
                LOG.warn(e);
            }
        }
        return best;
    }


    @NotNull
    private static String getApiVersion() {
        /*
        if (cachedApiVersion == null) {
            // Allow for the async duplicate load time impact,
            // as it's relatively cheap anyway.
            try {
                cachedApiVersion = ApplicationInfo.getInstance().getApiVersion();
            } catch (NullPointerException e) {
                LOG.error("Could not load the current IDE API version; assuming " + DEFAULT_API_VERSION, e);
                cachedApiVersion = DEFAULT_API_VERSION;
            }
        }
        return cachedApiVersion;
        */
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
        if (factory == null) {
            return false;
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            // Unit test sets the API version to "999.SNAPSHOT"
            return true;
        }


        int minCompatibility = compareIdeaVersionNumbers(
                factory.getMinCompatibleApiVersion(),
                apiVersion);
        int maxCompatibility = compareIdeaVersionNumbers(
                factory.getMaxCompatibleApiVersion(),
                apiVersion);
        // we want the min to be greater or equal than the api,
        // and we want the max to be smaller than the api.
        return minCompatibility >= 0 && maxCompatibility < 0;
    }

    private static boolean isBetterVersion(@Nullable CompatFactory best, @Nullable CompatFactory factory) {
        return factory != null && (best == null || compareIdeaVersionNumbers(
                best.getMinCompatibleApiVersion(),
                factory.getMinCompatibleApiVersion()) > 0);
    }

    private static int compareIdeaVersionNumbers(String first, String second) {
        // Version can be "IC-version.number.number" (IC meaning the Community edition)
        StringBuilder s1 = new StringBuilder(first);
        StringBuilder s2 = new StringBuilder(second);
        final int pos1 = s1.indexOf("-");
        if (pos1 >= 0)
        {
            s1.delete(0, pos1 + 1);
        }
        final int pos2 = s2.indexOf("-");
        if (pos2 >= 0) {
            s2.delete(0, pos2 + 1);
        }
        try {
            // Note: use of or ("||") here - the "next version"
            // call will strip out the consumed version number,
            // and if there is nothing left in the string, it
            // will return Integer.MIN_VALUE as the version.
            // This means we can avoid string remainder checks
            // at the end.
            while (s1.length() > 0 || s2.length() > 0)
            {
                final int part1 = nextVersion(s1);
                final int part2 = nextVersion(s2);

                if (part1 > part2)
                {
                    return -1;
                }
                if (part1 < part2)
                {
                    return 1;
                }
            }
            // They are the same.
            return 0;
        } catch (NumberFormatException e) {
            throw new IncompatibleApiVersionException(first, second, e);
        }
    }


    private static int nextVersion(StringBuilder part)
            throws NumberFormatException
    {
        if (part == null || part.length() <= 0)
        {
            // Nothing left in the version part.  Return
            // a number that will mean that this comes before
            // any other patch number.
            return Integer.MIN_VALUE;
        }
        final int pos = part.indexOf(".");
        final int ret;
        if (pos >= 0)
        {
            ret = Integer.parseInt(part.substring(0, pos));
            part.delete(0, pos + 1);
        }
        else
        {
            ret = Integer.parseInt(part.toString());
            part.delete(0, part.length());
        }
        return ret;
    }
}
