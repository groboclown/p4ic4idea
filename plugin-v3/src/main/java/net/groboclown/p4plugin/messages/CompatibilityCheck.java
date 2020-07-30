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

package net.groboclown.p4plugin.messages;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;


/**
 * Reports a warning to the user if they are using an IDE version that will be
 * removed in the next version.
 */
public class CompatibilityCheck {
    private static final Logger LOG = Logger.getInstance(CompatibilityCheck.class);

    // These major version / minor version use the "2018.2" format, rather than the "182.0" format.
    private static final int DEPRECATED_SUPPORT__MAJOR = 2018;
    private static final int DEPRECATED_SUPPORT__MINOR = 2;
    private static final String MINIMUM_VERSION = DEPRECATED_SUPPORT__MAJOR + "." + DEPRECATED_SUPPORT__MINOR;


    public static void checkCompatibility(@NotNull Project project, String majorVersion, String minorVersion) {
        if (!isIdeaVersionValid(majorVersion, minorVersion)) {
            UserMessage.showNotification(project, UserMessage.WARNING,
                    P4Bundle.message("ide.compatibility.message", majorVersion, minorVersion, MINIMUM_VERSION),
                    P4Bundle.message("ide.compatibility.title"),
                    NotificationType.WARNING);
        }
    }


    static boolean isIdeaVersionValid(String majorStr, String minorStr) {
        LOG.warn("IDE version: " + majorStr + "." + minorStr);
        try {
            int major = Integer.parseInt(majorStr);
            int minor = Integer.parseInt(minorStr);
            if (major > DEPRECATED_SUPPORT__MAJOR) {
                // It's a supported version for a while.
                return true;
            }
            if (major == DEPRECATED_SUPPORT__MINOR && minor >= DEPRECATED_SUPPORT__MINOR) {
                // It's a supported version for a while
                return true;
            }
            return false;
        } catch (NumberFormatException e) {
            LOG.error("Invalid IDE version numbers: " + majorStr + "." + minorStr);
            return false;
        }
    }
}
