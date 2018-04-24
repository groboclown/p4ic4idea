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
package net.groboclown.p4.server.api.config;

import net.groboclown.p4.server.api.MockConfigPart;
import org.junit.jupiter.api.Test;

import static net.groboclown.idea.ExtMatchers.greaterThan;
import static net.groboclown.idea.ExtMatchers.lessThan;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ConfigProblemTest {
    @Test
    void getters_1() {
        MockConfigPart part = new MockConfigPart();
        ConfigProblem cp = new ConfigProblem(part, "Foo bar", true);
        assertEquals("Foo bar", cp.getMessage());
        assertEquals(part, cp.getSource());
        assertTrue(cp.isError());
        assertEquals(
                "problem(" + part + ": Foo bar)",
                cp.toString());
    }

    @Test
    void getters_2() {
        MockConfigPart part = new MockConfigPart();
        ConfigProblem cp = new ConfigProblem(part, "Too bad", false);
        assertFalse(cp.isError());
        assertEquals("Too bad", cp.getMessage());
        assertEquals(part, cp.getSource());
        assertEquals(
                "problem(" + part + ": Too bad)",
                cp.toString());
        part.withUsername("u");
        assertEquals(part, cp.getSource());
        assertEquals(
                "problem(" + part + ": Too bad)",
                cp.toString());
    }

    @Test
    void compareTo_err1() {
        MockConfigPart part = new MockConfigPart();
        ConfigProblem cpF1 = new ConfigProblem(part, "Abc", false);
        ConfigProblem cpT1 = new ConfigProblem(part, "Abc", true);
        ConfigProblem cpF2 = new ConfigProblem(part, "Abc", false);
        ConfigProblem cpT2 = new ConfigProblem(part, "Abc", true);

        assertEquals(
                1,
                cpF1.compareTo(cpT1)
        );
        assertEquals(
                -1,
                cpT1.compareTo(cpF1)
        );
        assertEquals(
                0,
                cpT1.compareTo(cpT2)
        );
        assertEquals(
                0,
                cpT2.compareTo(cpT1)
        );
        assertEquals(
                0,
                cpF2.compareTo(cpF1)
        );
    }

    @Test
    void compareTo_err2() {
        MockConfigPart part = new MockConfigPart();
        ConfigProblem cpF1 = new ConfigProblem(part, "Abc", false);
        ConfigProblem cpF2 = new ConfigProblem(part, "Def", false);
        ConfigProblem cpT1 = new ConfigProblem(part, "Abc", true);
        ConfigProblem cpT2 = new ConfigProblem(part, "Def", true);

        assertThat(cpF1.compareTo(cpF2), lessThan(0));
        assertThat(cpF2.compareTo(cpF1), greaterThan(0));
        assertThat(cpT1.compareTo(cpT2), lessThan(0));
        assertThat(cpT2.compareTo(cpT1), greaterThan(0));
    }
}