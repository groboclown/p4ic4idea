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

package net.groboclown.p4.server.api;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Loads the plugin version from the plugin.xml file.
 */
public class P4PluginVersion {
    private static final Logger LOG = Logger.getInstance(P4PluginVersion.class);

    private static volatile String version;

    public static String getPluginVersion() {
        if (version == null) {
            synchronized (P4PluginVersion.class) {
                version = loadPluginVersion();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Running plugin version " + version);
            }
        }
        return version;
    }


    @NotNull
    private static String loadPluginVersion() {
        ClassLoader cl = getClassLoader();
        if (cl == null) {
            // Can't find the version.
            return "1";
        }
        try {
            final InputStream res = cl.getResourceAsStream("p4ic-version.txt");
            if (res == null) {
                return "3";
            }
            try {
                StringBuilder sb = new StringBuilder();
                byte[] buff = new byte[4096];
                int len;
                while ((len = res.read(buff)) > 0) {
                    // TODO encoding
                    sb.append(new String(buff, 0, len));
                }
                return sb.toString().trim();
            } finally {
                res.close();
            }
        } catch (Exception e) {
            LOG.info("Cannot read p4ic-version.txt", e);
            return "2";
        }
    }

    @Nullable
    private static ClassLoader getClassLoader() {
        // Do not fetch the class loader from the thread context; we want
        // the plugin's class loader, not whatever context this is running
        // in.
        final ClassLoader ret = P4PluginVersion.class.getClassLoader();
        if (ret != null) {
            return ret;
        }
        return null;
    }
}
