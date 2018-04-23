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
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.p4.server.api.config.part.TestableConfigProblem.*;
import static org.junit.jupiter.api.Assertions.*;

class DataPartAdapterTest {

    @Test
    void hasError_default() {
        assertFalse(new DataPartAdapter().hasError());
    }

    @Test
    void hasError_noProblems() {
        assertFalse(new ProblemDataPartAdapter().hasError());
    }

    @Test
    void hasError_oneProblem_NotError() {
        assertFalse(new ProblemDataPartAdapter(
                createWarning()
        ).hasError());
    }

    @Test
    void hasError_oneProblem_Error() {
        assertTrue(new ProblemDataPartAdapter(
                createError()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_NotError() {
        assertFalse(new ProblemDataPartAdapter(
                createWarning(), createWarning(),createWarning(), createWarning()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_FirstError() {
        assertTrue(new ProblemDataPartAdapter(
                createError(), createWarning(),createWarning(), createWarning(), createWarning()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_LastError() {
        assertTrue(new ProblemDataPartAdapter(
                createWarning(), createWarning(),createWarning(), createWarning(), createError()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_AllErrors() {
        assertTrue(new ProblemDataPartAdapter(
                createError(), createError(),createError(), createError()
        ).hasError());
    }


    @Test
    void getRootPath() {
        assertNull(new DataPartAdapter().getRootPath());
    }

    @Test
    void hasServerNameSet() {
        assertFalse(new DataPartAdapter().hasServerNameSet());
    }

    @Test
    void getServerName() {
        assertNull(new DataPartAdapter().getServerName());
    }

    @Test
    void hasClientnameSet() {
        assertFalse(new DataPartAdapter().hasClientnameSet());
    }

    @Test
    void getClientname() {
        assertNull(new DataPartAdapter().getClientname());
    }

    @Test
    void hasUsernameSet() {
        assertFalse(new DataPartAdapter().hasUsernameSet());
    }

    @Test
    void getUsername() {
        assertNull(new DataPartAdapter().getUsername());
    }

    @Test
    void hasPasswordSet() {
        assertFalse(new DataPartAdapter().hasPasswordSet());
    }

    @Test
    void getPlaintextPassword() {
        assertNull(new DataPartAdapter().getPlaintextPassword());
    }

    @Test
    void hasAuthTicketFileSet() {
        assertFalse(new DataPartAdapter().hasAuthTicketFileSet());
    }

    @Test
    void getAuthTicketFile() {
        assertNull(new DataPartAdapter().getAuthTicketFile());
    }

    @Test
    void hasTrustTicketFileSet() {
        assertFalse(new DataPartAdapter().hasTrustTicketFileSet());
    }

    @Test
    void getTrustTicketFile() {
        assertNull(new DataPartAdapter().getTrustTicketFile());
    }

    @Test
    void hasServerFingerprintSet() {
        assertFalse(new DataPartAdapter().hasServerFingerprintSet());
    }

    @Test
    void getServerFingerprint() {
        assertNull(new DataPartAdapter().getServerFingerprint());
    }

    @Test
    void hasClientHostnameSet() {
        assertFalse(new DataPartAdapter().hasClientHostnameSet());
    }

    @Test
    void getClientHostname() {
        assertNull(new DataPartAdapter().getClientHostname());
    }

    @Test
    void hasIgnoreFileNameSet() {
        assertFalse(new DataPartAdapter().hasIgnoreFileNameSet());
    }

    @Test
    void getIgnoreFileName() {
        assertNull(new DataPartAdapter().getIgnoreFileName());
    }

    @Test
    void hasDefaultCharsetSet() {
        assertFalse(new DataPartAdapter().hasDefaultCharsetSet());
    }

    @Test
    void getDefaultCharset() {
        assertNull(new DataPartAdapter().getDefaultCharset());
    }

    @Test
    void hasLoginSsoSet() {
        assertFalse(new DataPartAdapter().hasLoginSsoSet());
    }

    @Test
    void getLoginSso() {
        assertNull(new DataPartAdapter().getLoginSso());
    }

    @Test
    void testToString() {
        assertEquals("DataPartAdapter", new DataPartAdapter().toString());
    }

    @Test
    void reload() {
        assertFalse(new DataPartAdapter().reload());
    }

    @Test
    void getConfigProblems() {
        assertEmpty(new DataPartAdapter().getConfigProblems());
    }

    @Test
    void requiresUserEnteredPassword() {
        assertFalse(new DataPartAdapter().requiresUserEnteredPassword());
    }

    private static class ProblemDataPartAdapter extends DataPartAdapter {
        private final List<ConfigProblem> problems;

        private ProblemDataPartAdapter(ConfigProblem... problems) {
            this.problems = Arrays.asList(problems);
        }

        @Override
        public Collection<ConfigProblem> getConfigProblems() {
            return problems;
        }
    }
}