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

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.util.JreSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class EnvCompositePartTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    /**
     * Validate that changes to the ENV are only picked up on reload.
     */
    @Test
    void reload_port() {
        VirtualFile root = MockVirtualFileSystem.createTree("/my/root/file", "data").get("/my/root/file").getParent();
        JreSettings.setOverrides(new Pair<>("P4PORT", "my-port:1234"));
        EnvCompositePart part = new EnvCompositePart(root);
        assertEquals("my-port:1234", part.getRawPort());

        JreSettings.setOverrides(new Pair<>("P4PORT", "your-port:4321"));
        assertEquals("my-port:1234", part.getRawPort());
        part.reload();
        assertEquals("your-port:4321", part.getRawPort());
    }

    @Test
    void reload_enviro() {
        Map<String, MockVirtualFile> tree = MockVirtualFileSystem.createTree(
                "/my/root/file", "data",
                "/my/root/config", "P4PORT=my-port:1234",
                "/my/root/config2", "P4PORT=another:23");
        VirtualFile root = tree.get("/my/root/file").getParent();
        MockVirtualFile configFile = tree.get("/my/root/config");
        MockVirtualFile configFile2 = tree.get("/my/root/config2");
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root")).thenReturn(root);
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root/config")).thenReturn(configFile);
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root/config2")).thenReturn(configFile2);

        // Work around for tests running on windows, where "/my" isn't recognized as an absolute path.
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root/my/root/config")).thenReturn(configFile);
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root\\/my/root/config")).thenReturn(configFile);
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root/my/root/config2")).thenReturn(configFile2);
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root\\/my/root/config2")).thenReturn(configFile2);

        JreSettings.setOverrides(
                new Pair<>("P4ENVIRO", configFile.getPath()),

                // Explicitly set all the values that the testing environment might have that would
                // interfere with this test.
                new Pair<>("P4CONFIG", ""),
                new Pair<>("P4PORT", ""));

        EnvCompositePart part = new EnvCompositePart(root);
        assertEquals("my-port:1234", part.getRawPort());

        configFile.setContents("P4PORT=your-port:4321", Charset.forName("UTF-8"));
        assertEquals("my-port:1234", part.getRawPort());
        part.reload();
        assertEquals("your-port:4321", part.getRawPort());

        JreSettings.setOverrides(new Pair<>("P4ENVIRO", configFile2.getPath()));
        assertEquals("your-port:4321", part.getRawPort());
        part.reload();
        assertEquals("another:23", part.getRawPort());
    }

    @BeforeAll
    static void beforeClass() {
        WinRegDataPart.setAvailable(false);
    }

    @AfterAll
    static void afterClass() {
        WinRegDataPart.setAvailable(true);

        // Clear out the overrides.
        JreSettings.setOverrides();
    }
}