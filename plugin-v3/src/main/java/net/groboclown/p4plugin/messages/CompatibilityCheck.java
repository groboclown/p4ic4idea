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
import com.intellij.openapi.util.BuildNumber;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;


/**
 * Reports a warning to the user if they are using an IDE version that will be
 * removed in the next version.
 */
public class CompatibilityCheck {
    private static final Logger LOG = Logger.getInstance(CompatibilityCheck.class);

    // Baseline is the API version, which is compatible across Android Studio and the JetBrains tools.
    // Android uses a "4.0" style numbering for major / minor, while JetBrains uses "2018.2" format.
    private static final int DEPRECATED_SUPPORT__BASELINE = 182;


    public static void checkCompatibility(@NotNull Project project, String majorVersion, String minorVersion,
            BuildNumber build) {
        if (!isIdeaVersionValid(majorVersion, minorVersion, build)) {
            UserMessage.showNotification(project, UserMessage.WARNING,
                    P4Bundle.message("ide.compatibility.message",
                            majorVersion, minorVersion, build.getProductCode(), build.getBaselineVersion(),
                            DEPRECATED_SUPPORT__BASELINE),
                    P4Bundle.message("ide.compatibility.title"),
                    NotificationType.WARNING);
        }
    }


    static boolean isIdeaVersionValid(String majorStr, String minorStr, BuildNumber build) {
        LOG.warn("IDE version: " + majorStr + "." + minorStr + "; product version " +
                build.getProductCode() + "-" + build.getBaselineVersion());
        if (build.getBaselineVersion() > DEPRECATED_SUPPORT__BASELINE) {
            return true;
        }

        // It's been noticed that the minor version can include a patch.  That's probably an IDE bug,
        // but we still need to account for it.
        // That said, the baseline version of the build is sufficient to check.
        return false;
    }
}
