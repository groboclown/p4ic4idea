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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigPartAdapterTest {

    @Test
    void hasError_default() {
        assertFalse(new ConfigPartAdapter("Blah").hasError());
    }

    @Test
    void hasError_noProblems() {
        assertFalse(new ProblemConfigPartAdapter().hasError());
    }

    @Test
    void hasError_oneProblem_NotError() {
        assertFalse(new ProblemConfigPartAdapter(
                createWarning()
        ).hasError());
    }

    @Test
    void hasError_oneProblem_Error() {
        assertTrue(new ProblemConfigPartAdapter(
                createError()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_NotError() {
        assertFalse(new ProblemConfigPartAdapter(
                createWarning(), createWarning(),createWarning(), createWarning()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_FirstError() {
        assertTrue(new ProblemConfigPartAdapter(
                createError(), createWarning(),createWarning(), createWarning(), createWarning()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_LastError() {
        assertTrue(new ProblemConfigPartAdapter(
                createWarning(), createWarning(),createWarning(), createWarning(), createError()
        ).hasError());
    }

    @Test
    void hasError_multipleProblems_AllErrors() {
        assertTrue(new ProblemConfigPartAdapter(
                createError(), createError(),createError(), createError()
        ).hasError());
    }


    @Test
    void getSourceName() {
        assertEquals("Blah", new ConfigPartAdapter("Blah").getSourceName());
    }

    @Test
    void hasServerNameSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasServerNameSet());
    }

    @Test
    void getServerName() {
        assertNull(new ConfigPartAdapter("Blah").getServerName());
    }

    @Test
    void hasClientnameSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasClientnameSet());
    }

    @Test
    void getClientname() {
        assertNull(new ConfigPartAdapter("Blah").getClientname());
    }

    @Test
    void hasUsernameSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasUsernameSet());
    }

    @Test
    void getUsername() {
        assertNull(new ConfigPartAdapter("Blah").getUsername());
    }

    @Test
    void hasPasswordSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasPasswordSet());
    }

    @Test
    void getPlaintextPassword() {
        assertNull(new ConfigPartAdapter("Blah").getPlaintextPassword());
    }

    @Test
    void hasAuthTicketFileSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasAuthTicketFileSet());
    }

    @Test
    void getAuthTicketFile() {
        assertNull(new ConfigPartAdapter("Blah").getAuthTicketFile());
    }

    @Test
    void hasTrustTicketFileSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasTrustTicketFileSet());
    }

    @Test
    void getTrustTicketFile() {
        assertNull(new ConfigPartAdapter("Blah").getTrustTicketFile());
    }

    @Test
    void hasServerFingerprintSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasServerFingerprintSet());
    }

    @Test
    void getServerFingerprint() {
        assertNull(new ConfigPartAdapter("Blah").getServerFingerprint());
    }

    @Test
    void hasClientHostnameSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasClientHostnameSet());
    }

    @Test
    void getClientHostname() {
        assertNull(new ConfigPartAdapter("Blah").getClientHostname());
    }

    @Test
    void hasIgnoreFileNameSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasIgnoreFileNameSet());
    }

    @Test
    void getIgnoreFileName() {
        assertNull(new ConfigPartAdapter("Blah").getIgnoreFileName());
    }

    @Test
    void hasDefaultCharsetSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasDefaultCharsetSet());
    }

    @Test
    void getDefaultCharset() {
        assertNull(new ConfigPartAdapter("Blah").getDefaultCharset());
    }

    @Test
    void hasLoginSsoSet() {
        assertFalse(new ConfigPartAdapter("Blah").hasLoginSsoSet());
    }

    @Test
    void getLoginSso() {
        assertNull(new ConfigPartAdapter("Blah").getLoginSso());
    }

    @Test
    void testToString() {
        assertEquals("ConfigPartAdapter", new ConfigPartAdapter("Blah").toString());
    }

    @Test
    void reload() {
        assertTrue(new ConfigPartAdapter("Blah").reload());
    }

    @Test
    void getConfigProblems() {
        assertEmpty(new ConfigPartAdapter("Blah").getConfigProblems());
    }

    @Test
    void requiresUserEnteredPassword() {
        assertFalse(new ConfigPartAdapter("Blah").requiresUserEnteredPassword());
    }

    private static class ProblemConfigPartAdapter
            extends ConfigPartAdapter {
        private final List<ConfigProblem> problems;

        private ProblemConfigPartAdapter(ConfigProblem... problems) {
            super("Blah");
            this.problems = Arrays.asList(problems);
        }

        @Nonnull
        @Override
        public Collection<ConfigProblem> getConfigProblems() {
            return problems;
        }
    }
}