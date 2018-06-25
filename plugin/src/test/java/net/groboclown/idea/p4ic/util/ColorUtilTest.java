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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.awt.*;
import java.util.Properties;

public class ColorUtilTest {
    private boolean isSetupDark;
    private static String origHeadlessValue = null;


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

    /*
    private static boolean isHeadless() {
        return
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .isHeadlessInstance();
    }
    */

    // These tests need to run in explicit headless mode to prevent AWT code from
    // trying to do too much.
    @BeforeClass
    public static void beforeClass() {
        origHeadlessValue = System.getProperty("java.awt.headless");
        System.setProperty("java.awt.headless", "true");
    }

    @AfterClass
    public static void afterClass() {
        if (origHeadlessValue == null) {
            Properties props = System.getProperties();
            if (props.contains("java.awt.headless")) {
                props.remove("java.awt.headless");
            }
            System.setProperties(props);
        } else {
            System.setProperty("java.awt.headless", origHeadlessValue);
        }
    }

    @Before
    public void setup() {
        isSetupDark = !JBColor.isBright();
    }

    @After
    public void teardown() {
        JBColor.setDark(isSetupDark);
    }
}
