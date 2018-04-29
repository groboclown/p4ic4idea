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

package net.groboclown.p4plugin.ui;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ColorUtil {
    @Nullable
    public static JBColor lightenColor(@Nullable Color color) {
        if (color == null) {
            return null;
        }

        return new JBColor(
                new Color(
                        lighten(color.getRed(), true),
                        lighten(color.getGreen(), true),
                        lighten(color.getBlue(), true)
                ), new Color(
                        lighten(color.getRed(), false),
                        lighten(color.getGreen(), false),
                        lighten(color.getBlue(), false)
                )
        );
    }


    private static int lighten(int base, boolean isLight) {
        int newVal;
        if (isLight) {
            // light color scheme, so lighten it (to make it not stand out as much).
            newVal = base + 32;
        } else {
            newVal = base - 32;
        }
        // Issue #168 - make sure the maximum value is 255, not 256.
        return Math.max(0, Math.min(255, newVal));
    }

}
