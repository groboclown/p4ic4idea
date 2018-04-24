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
package net.groboclown.p4.server.impl.config.part;

import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.api.util.JreSettingsExtensions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ResultOfMethodCallIgnored")
class DefaultConfigPartTest {

    @BeforeEach
    void before() {
        JreSettingsExtensions.setOverrides(null);
    }

    @AfterEach
    void after() {
        JreSettingsExtensions.setOverrides(null);
    }

    @Test
    void hasAuthTicketFileSet_null() {
        JreSettingsExtensions.setOverrideValues(
                "USERPROFILE", null,
                "HOME", null
        );
        assertFalse(new DefaultConfigPart("Blah").hasAuthTicketFileSet());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void hasAuthTicketFileSet_no(TemporaryFolder tmpDir) {
        File authTicket = tmpDir.newFile("authTicket.txt");
        authTicket.delete();
        DefaultConfigPart part = new FileDefaultConfigPart(authTicket);

        // Contract: if the value is set, it must be returned, regardless of whether it
        // exists or not..
        assertTrue(part.hasAuthTicketFileSet());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void hasAuthTicketFileSet_yes(TemporaryFolder tmpDir)
            throws IOException {
        File authTicket = tmpDir.newFile("authTicket.txt");
        authTicket.createNewFile();
        DefaultConfigPart part = new FileDefaultConfigPart(authTicket);
        assertTrue(part.hasAuthTicketFileSet());
    }

    @Test
    void getAuthTicketFile_null() {
        JreSettingsExtensions.setOverrideValues(
                "USERPROFILE", null,
                "HOME", null
        );
        assertNull(new DefaultConfigPart("Foo").getAuthTicketFile());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getAuthTicketFile_notExist(TemporaryFolder tmpDir) {
        File authTicket = tmpDir.newFile("authTicket.txt");
        authTicket.delete();
        assertFalse(authTicket.exists());
        DefaultConfigPart part = new FileDefaultConfigPart(authTicket);

        // Contract: if the value is set, it must be returned, regardless of whether it
        // exists or not..
        assertEquals(authTicket, part.getAuthTicketFile());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getAuthTicketFile_exist(TemporaryFolder tmpDir)
            throws IOException {
        File authTicket = tmpDir.newFile("authTicket.txt");
        authTicket.createNewFile();
        DefaultConfigPart part = new FileDefaultConfigPart(authTicket);
        assertEquals(authTicket, part.getAuthTicketFile());
    }

    @Test
    void hasTrustTicketFileSet_null() {
        JreSettingsExtensions.setOverrideValues(
                "USERPROFILE", null,
                "HOME", null
        );
        assertFalse(new DefaultConfigPart("Bar").hasTrustTicketFileSet());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void hasTrustTicketFileSet_no(TemporaryFolder tmpDir) {
        File ticket = tmpDir.newFile("ticket.txt");
        ticket.delete();
        DefaultConfigPart part = new FileDefaultConfigPart(ticket);

        // Contract: if the value is set, it must be returned, regardless of whether it
        // exists or not..
        assertTrue(part.hasTrustTicketFileSet());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void hasTrustTicketFileSet_yes(TemporaryFolder tmpDir)
            throws IOException {
        File ticket = tmpDir.newFile("ticket.txt");
        ticket.createNewFile();
        DefaultConfigPart part = new FileDefaultConfigPart(ticket);
        assertTrue(part.hasTrustTicketFileSet());
    }

    @Test
    void getTrustTicketFile_null() {
        JreSettingsExtensions.setOverrideValues(
                "USERPROFILE", null,
                "HOME", null
        );
        assertNull(new DefaultConfigPart("Tuna").getTrustTicketFile());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getTrustTicketFile_notExist(TemporaryFolder tmpDir) {
        File ticket = tmpDir.newFile("ticket.txt");
        ticket.delete();
        DefaultConfigPart part = new FileDefaultConfigPart(ticket);

        // Contract: if the value is set, it must be returned, regardless of whether it
        // exists or not..
        assertEquals(ticket, part.getTrustTicketFile());
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void getTrustTicketFile_exist(TemporaryFolder tmpDir)
            throws IOException {
        File ticket = tmpDir.newFile("ticket.txt");
        ticket.createNewFile();
        DefaultConfigPart part = new FileDefaultConfigPart(ticket);
        assertEquals(ticket, part.getTrustTicketFile());
    }

    private static class FileDefaultConfigPart
            extends DefaultConfigPart {
        private final File ret;

        FileDefaultConfigPart(@NotNull File ret) {
            super("Blah");
            this.ret = ret;
        }

        @Override
        @NotNull
        File getFileAt(@NotNull String dir, @NotNull String name) {
            return ret;
        }
    }
}