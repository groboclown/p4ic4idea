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

package net.groboclown.p4.server.api.config.part;

import net.groboclown.p4.server.api.config.ConfigProblem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static net.groboclown.p4.server.api.config.part.TestableConfigProblem.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositePartTest {

    @Test
    void hasError_noProblems() {
        CompositePart part = new TestableCompositePart();
        assertFalse(part.hasError());
    }

    @Test
    void hasError_oneProblem_NotError() {
        CompositePart part = new TestableCompositePart(createWarning());
        assertFalse(part.hasError());
    }

    @Test
    void hasError_oneProblem_Error() {
        CompositePart part = new TestableCompositePart(createError());
        assertTrue(part.hasError());
    }

    @Test
    void hasError_multipleProblems_NotError() {
        CompositePart part = new TestableCompositePart(
                createWarning(), createWarning(),createWarning(), createWarning());
        assertFalse(part.hasError());
    }

    @Test
    void hasError_multipleProblems_FirstError() {
        CompositePart part = new TestableCompositePart(
                createError(), createWarning(),createWarning(), createWarning(), createWarning());
        assertTrue(part.hasError());
    }

    @Test
    void hasError_multipleProblems_LastError() {
        CompositePart part = new TestableCompositePart(
                createWarning(), createWarning(),createWarning(), createWarning(), createError());
        assertTrue(part.hasError());
    }

    @Test
    void hasError_multipleProblems_AllErrors() {
        CompositePart part = new TestableCompositePart(
                createError(), createError(),createError(), createError());
        assertTrue(part.hasError());
    }

    static class TestableCompositePart extends CompositePart {
        private final List<ConfigProblem> problems;

        TestableCompositePart(ConfigProblem... problems) {
            this.problems = Arrays.asList(problems);
        }

        @NotNull
        @Override
        public List<ConfigPart> getConfigParts() {
            return Collections.emptyList();
        }

        @Override
        public boolean reload() {
            return false;
        }

        @NotNull
        @Override
        public Collection<ConfigProblem> getConfigProblems() {
            return problems;
        }
    }

}