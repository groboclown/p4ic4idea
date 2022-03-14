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

import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.P4ServerName;
import net.groboclown.p4.server.api.config.ConfigProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static net.groboclown.idea.ExtMatchers.containsAll;
import static net.groboclown.idea.ExtMatchers.hasSize;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createError;
import static net.groboclown.p4.server.api.config.part.ConfigProblemUtil.createWarning;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MultipleConfigPartTest {

    @Test
    void emptyList() {
        MultipleConfigPart part = new MultipleConfigPart("Blah", Collections.emptyList());
        assertEquals("Blah", part.getSourceName());

        //noinspection EqualsWithItself,SimplifiableJUnitAssertion
        assertTrue(part.equals(part));
        //noinspection SimplifiableJUnitAssertion
        assertTrue(part.equals(new MultipleConfigPart("Blah", Collections.emptyList())));

        // Show that reload doesn't throw an error.
        part.reload();

        assertEquals("Blah".hashCode() + Collections.emptyList().hashCode(), part.hashCode());
        assertEquals("MultipleConfigPart(Blah):" + part.getInstanceIndex(), part.toString());
        assertEmpty(part.getConfigProblems());
        assertFalse(part.hasError());
        assertFalse(part.hasServerNameSet());
        assertNull(part.getServerName());
        assertFalse(part.hasClientnameSet());
        assertNull(part.getClientname());
        assertFalse(part.hasUsernameSet());
        assertNull(part.getUsername());
        assertFalse(part.hasPasswordSet());
        assertNull(part.getPlaintextPassword());
        assertFalse(part.requiresUserEnteredPassword());
        assertFalse(part.hasAuthTicketFileSet());
        assertNull(part.getAuthTicketFile());
        assertFalse(part.hasTrustTicketFileSet());
        assertNull(part.getTrustTicketFile());
        assertFalse(part.hasServerFingerprintSet());
        assertNull(part.getServerFingerprint());
        assertFalse(part.hasClientnameSet());
        assertNull(part.getClientHostname());
        assertFalse(part.hasIgnoreFileNameSet());
        assertNull(part.getIgnoreFileName());
        assertFalse(part.hasDefaultCharsetSet());
        assertNull(part.getDefaultCharset());
        assertFalse(part.hasLoginSsoSet());
        assertNull(part.getLoginSso());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void one(TemporaryFolder tmpDir) {
        File authTicket = tmpDir.newFile("authTicket");
        File trustTicket = tmpDir.newFile("trustTicket");
        MockConfigPart mockPart = new MockConfigPart()
                .withServerName("abc")
                .withUsername("luser1")
                .withClientname("client1")
                .withPassword("pass1")
                .withAuthTicketFile(authTicket)
                .withServerFingerprint("abcdef1234567890")
                .withTrustTicketFile(trustTicket)
                .withClientHostname("host1")
                .withConfigProblems(createWarning(), createError())
                .withDefaultCharset("UTF-8")
                .withIgnoreFileName(".p4ignore1")
                .withLoginSso("my-login-sso.sh")
                .withRequiresUserEnteredPassword(true);
        MultipleConfigPart part = new MultipleConfigPart("Blah2", Collections.singletonList(mockPart));
        assertEquals("Blah2", part.getSourceName());

        //noinspection EqualsWithItself,SimplifiableJUnitAssertion
        assertTrue(part.equals(part));
        //noinspection SimplifiableJUnitAssertion
        assertTrue(part.equals(new MultipleConfigPart("Blah2", Collections.singletonList(mockPart))));

        // Show that reload doesn't throw an error.
        part.reload();

        assertEquals("Blah2".hashCode() + Collections.singletonList(mockPart).hashCode(), part.hashCode());
        assertEquals("MultipleConfigPart(Blah2):" + part.getInstanceIndex(), part.toString());
        assertThat(part.getConfigProblems(), hasSize(2));
        assertThat(part.getConfigProblems(), containsAll(mockPart.getConfigProblems()));
        assertTrue(part.hasError());
        assertTrue(part.hasServerNameSet());
        assertEquals(P4ServerName.forPort("abc"), part.getServerName());
        assertTrue(part.hasClientnameSet());
        assertEquals("client1", part.getClientname());
        assertTrue(part.hasUsernameSet());
        assertEquals("luser1", part.getUsername());
        assertTrue(part.hasPasswordSet());
        assertEquals("pass1", part.getPlaintextPassword());
        assertTrue(part.requiresUserEnteredPassword());
        assertTrue(part.hasAuthTicketFileSet());
        assertEquals(authTicket, part.getAuthTicketFile());
        assertTrue(part.hasTrustTicketFileSet());
        assertEquals(trustTicket, part.getTrustTicketFile());
        assertTrue(part.hasServerFingerprintSet());
        assertEquals("abcdef1234567890", part.getServerFingerprint());
        assertTrue(part.hasClientnameSet());
        assertEquals("host1", part.getClientHostname());
        assertTrue(part.hasIgnoreFileNameSet());
        assertEquals(".p4ignore1", part.getIgnoreFileName());
        assertTrue(part.hasDefaultCharsetSet());
        assertEquals("UTF-8", part.getDefaultCharset());
        assertTrue(part.hasLoginSsoSet());
        assertEquals("my-login-sso.sh", part.getLoginSso());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void several(TemporaryFolder tmpDir) {
        File authTicket = tmpDir.newFile("authTicket");
        File trustTicket = tmpDir.newFile("trustTicket");
        MockConfigPart mockPartFirst = new MockConfigPart()
                .withServerName("abc")
                .withClientname("client1")
                .withPassword("pass1")
                .withServerFingerprint("abcdef1234567890")
                .withClientHostname("host1")
                .withConfigProblems(createWarning(), createWarning())
                .withIgnoreFileName(".p4ignore1")
                .withRequiresUserEnteredPassword(true);
        MockConfigPart mockPartLast = new MockConfigPart()
                .withUsername("luser1")
                .withClientname("client1")
                .withAuthTicketFile(authTicket)
                .withServerFingerprint("--abcdef1234567890")
                .withTrustTicketFile(trustTicket)
                .withConfigProblems(createWarning(), createError())
                .withDefaultCharset("UTF-8")
                .withLoginSso("my-login-sso.sh");
        MultipleConfigPart part = new MultipleConfigPart("Blah2", Arrays.asList(mockPartFirst, mockPartLast));
        assertEquals("Blah2", part.getSourceName());

        //noinspection EqualsWithItself,SimplifiableJUnitAssertion
        assertTrue(part.equals(part));
        //noinspection SimplifiableJUnitAssertion
        assertTrue(part.equals(new MultipleConfigPart("Blah2", Arrays.asList(mockPartFirst, mockPartLast))));

        // Show that reload doesn't throw an error.
        part.reload();

        //assertEquals("Blah2".hashCode() + Collections.singletonList(mockPart).hashCode(), part.hashCode());
        assertEquals("MultipleConfigPart(Blah2):" + part.getInstanceIndex(), part.toString());

        assertThat(part.getConfigProblems(), hasSize(4));
        List<ConfigProblem> allProblems = new ArrayList<>(mockPartFirst.getConfigProblems());
        allProblems.addAll(mockPartLast.getConfigProblems());
        assertThat(part.getConfigProblems(), containsAll(
                allProblems
        ));
        assertTrue(part.hasError());

        // Ensure that changing the problems after construction will report different config problems.
        mockPartLast.withConfigProblems();
        assertThat(part.getConfigProblems(), hasSize(2));
        assertThat(part.getConfigProblems(), containsAll(mockPartFirst.getConfigProblems()));
        assertFalse(part.hasError());

        assertTrue(part.hasServerNameSet());
        assertEquals(P4ServerName.forPort("abc"), part.getServerName());
        assertTrue(part.hasClientnameSet());
        assertEquals("client1", part.getClientname());
        assertTrue(part.hasUsernameSet());
        assertEquals("luser1", part.getUsername());
        assertTrue(part.hasPasswordSet());
        assertEquals("pass1", part.getPlaintextPassword());
        assertTrue(part.requiresUserEnteredPassword());
        assertTrue(part.hasAuthTicketFileSet());
        assertEquals(authTicket, part.getAuthTicketFile());
        assertTrue(part.hasTrustTicketFileSet());
        assertEquals(trustTicket, part.getTrustTicketFile());
        assertTrue(part.hasServerFingerprintSet());
        assertEquals("abcdef1234567890", part.getServerFingerprint());
        assertTrue(part.hasClientnameSet());
        assertEquals("host1", part.getClientHostname());
        assertTrue(part.hasIgnoreFileNameSet());
        assertEquals(".p4ignore1", part.getIgnoreFileName());
        assertTrue(part.hasDefaultCharsetSet());
        assertEquals("UTF-8", part.getDefaultCharset());
        assertTrue(part.hasLoginSsoSet());
        assertEquals("my-login-sso.sh", part.getLoginSso());
    }
}