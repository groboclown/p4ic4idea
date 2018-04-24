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

package net.groboclown.p4.server.impl.config.part;

import net.groboclown.p4.server.api.config.part.ConfigPartAdapter;
import net.groboclown.p4.server.api.util.JreSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Declares all the values that are used by default by the p4 command line tool, even if the
 * user never specifies it.  This is not the environment variables or any other settings that
 * the user can change.
 */
public class DefaultConfigPart
        extends ConfigPartAdapter {
    public DefaultConfigPart(@NotNull @Nls(capitalization = Nls.Capitalization.Title) String sourceName) {
        super(sourceName);
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return getAuthTicketFile() != null;
    }

    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     */
    @Nullable
    @Override
    public File getAuthTicketFile() {
        // Cannot use the user.home system property, because there are very exact meanings to
        // the default locations.

        // Windows check
        String userprofile = JreSettings.getEnv("USERPROFILE");
        if (userprofile != null) {
            return getFileAt(userprofile, "p4tickets.txt");
        }
        String home = JreSettings.getEnv("HOME");
        if (home != null) {
            return getFileAt(home, ".p4tickets");
        }
        return null;
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return getTrustTicketFile() != null;
    }

    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     */
    @Nullable
    @Override
    public File getTrustTicketFile() {
        // Windows check
        String userprofile = JreSettings.getEnv("USERPROFILE");
        if (userprofile != null) {
            return getFileAt(userprofile, "p4trust.txt");
        }
        String home = JreSettings.getEnv("HOME");
        if (home != null) {
            return getFileAt(home, ".p4trust");
        }
        return null;
    }

    @NotNull
    File getFileAt(@NotNull String dir, @NotNull String name) {
        return new File(new File(dir), name);
    }
}
