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

package net.groboclown.idea.p4ic.util;

import com.intellij.ui.JBColor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.awt.*;

public class ColorUtilTest {
    private boolean isSetupDark;


    @Test
    public void lightenMax() {
        JBColor c = ColorUtil.lightenColor(new Color(255, 255, 255));
        JBColor.setDark(true);
        assertThat("Dark color for white - R",
                c.getRed(), is(255 - 32));
        assertThat("Dark color for white - G",
                c.getGreen(), is(255 - 32));
        assertThat("Dark color for white - B",
                c.getBlue(), is(255 - 32));
        assertThat("Dark color for white - A",
                c.getAlpha(), is(255));

        JBColor.setDark(false);
        assertThat("Light color for white - R",
                c.getRed(), is(255));
        assertThat("Light color for white - G",
                c.getGreen(), is(255));
        assertThat("Light color for white - B",
                c.getBlue(), is(255));
        assertThat("Light color for white - A",
                c.getAlpha(), is(255));
    }



    @Test
    public void lightenMin() {
        JBColor c = ColorUtil.lightenColor(new Color(0, 0, 0));
        JBColor.setDark(true);
        assertThat("Dark color for black - R",
                c.getRed(), is(0));
        assertThat("Dark color for black - G",
                c.getGreen(), is(0));
        assertThat("Dark color for black - B",
                c.getBlue(), is(0));
        assertThat("Dark color for black - A",
                c.getAlpha(), is(255));

        JBColor.setDark(false);
        assertThat("Light color for black - R",
                c.getRed(), is(32));
        assertThat("Light color for black - G",
                c.getGreen(), is(32));
        assertThat("Light color for black - B",
                c.getBlue(), is(32));
        assertThat("Light color for black - A",
                c.getAlpha(), is(255));
    }


    @Before
    public void setup() {
        isSetupDark = false; // FIXME 2017.1 ! JBColor.isBright();
    }

    @After
    public void teardown() {
        JBColor.setDark(isSetupDark);
    }
}
