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

package net.groboclown.idea.p4ic.config.part;

import net.groboclown.idea.p4ic.config.ConfigProblem;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * All the values that are used by default, even if no environment variable
 * for the value is set.  This will always be at the base of the configuration
 * web; the user will never specify it.  Because of that, it will not marshal
 * to a real tag.
 */
public class DefaultDataPart extends DataPartAdapter {
    @NotNull
    @Override
    public Element marshal() {
        throw new IllegalStateException("should never be called");
    }

    @Override
    public boolean reload() {
        // Do nothing
        return true;
    }

    @NotNull
    @Override
    public Collection<ConfigProblem> getConfigProblems() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasAuthTicketFileSet() {
        return getAuthTicketFile() != null && getAuthTicketFile().exists();
    }

    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     */
    @Nullable
    @Override
    public File getAuthTicketFile() {
        // TODO look at using System.getProperty("user.home") instead.

        // Windows check
        String userprofile = System.getenv("USERPROFILE");
        if (userprofile != null) {
            return new File(userprofile +
                    File.separator + "p4tickets.txt");
        }
        return new File(System.getenv("HOME") + File.separator + ".p4tickets");
    }

    @Override
    public boolean hasTrustTicketFileSet() {
        return getTrustTicketFile() != null && getTrustTicketFile().exists();
    }

    /**
     * See <a href="http://www.perforce.com/perforce/doc.current/manuals/cmdref/P4TICKETS.html">P4TICKETS environment
     * variable help</a>
     */
    @Nullable
    @Override
    public File getTrustTicketFile() {
        // TODO look at using System.getProperty("user.home") instead.

        // Windows check
        String userprofile = System.getenv("USERPROFILE");
        if (userprofile != null) {
            return new File(userprofile +
                    File.separator + "p4trust.txt");
        }
        return new File(System.getenv("HOME") + File.separator + ".p4trust");
    }
}
