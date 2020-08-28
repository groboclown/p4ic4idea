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

import com.intellij.openapi.util.BuildNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityCheckTest {
    @Test
    void checkAllOldVersionParsing() {
        // pulled from http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html
        assertTrue(CompatibilityCheck.isIdeaVersionValid("2018", "2",
                new BuildNumber("CI", 182, 2122)));
        assertFalse(CompatibilityCheck.isIdeaVersionValid("2017", "3",
                new BuildNumber("AI", 173, 22331)));
        assertFalse(CompatibilityCheck.isIdeaVersionValid("2018", "1",
                new BuildNumber("CI", 181, 22331)));
    }
}
