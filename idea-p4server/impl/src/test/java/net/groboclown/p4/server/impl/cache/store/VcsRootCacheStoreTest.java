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
package net.groboclown.p4.server.impl.cache.store;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.config.part.MultipleConfigPart;
import net.groboclown.p4.server.impl.config.part.ClientNameConfigPart;
import net.groboclown.p4.server.impl.config.part.EnvCompositePart;
import net.groboclown.p4.server.impl.config.part.FileConfigPart;
import net.groboclown.p4.server.impl.config.part.RequirePasswordDataPart;
import net.groboclown.p4.server.impl.config.part.ServerFingerprintDataPart;
import net.groboclown.p4.server.impl.config.part.SimpleDataPart;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class VcsRootCacheStoreTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void serializeRestore_two() {
        MockVirtualFile localDir = MockVirtualFileSystem.createTree("/my/local/file", "data").get("/my/local/file");
        when(idea.getMockLocalFilesystem().findFileByPath("/my/local/file")).thenReturn(localDir);

        VcsRootCacheStore store = new VcsRootCacheStore(localDir);
        ClientNameConfigPart part1 = new ClientNameConfigPart("source");
        part1.setClientname("client-name-1");
        ServerFingerprintDataPart part2 = new ServerFingerprintDataPart("source1");
        part2.setServerFingerprint("fingerprint-1");
        store.setConfigParts(Arrays.asList(part1, part2));

        VcsRootCacheStore.State originalState = store.getState();
        Element serialized = XmlSerializer.serialize(originalState);
        assertNotNull(serialized);

        VcsRootCacheStore.State restored = XmlSerializer.deserialize(serialized, VcsRootCacheStore.State.class);
        assertNotNull(restored);
        assertEquals("/my/local/file", restored.rootDirectory);
        assertSize(2, restored.configParts);
        assertNotNull(restored.configParts.get(0));
        assertEquals(restored.configParts.get(0).className, ClientNameConfigPart.class.getName());
        assertNull(restored.configParts.get(0).children);
        assertNotNull(restored.configParts.get(1));
        assertEquals(restored.configParts.get(1).className, ServerFingerprintDataPart.class.getName());
        assertNull(restored.configParts.get(1).children);

        VcsRootCacheStore loaded = new VcsRootCacheStore(restored, getClass().getClassLoader());
        assertEquals(localDir, loaded.getRootDirectory());
        assertSize(2, loaded.getConfigParts());

        assertThat(loaded.getConfigParts().get(0), instanceOf(ClientNameConfigPart.class));
        ClientNameConfigPart part1Restored = (ClientNameConfigPart) loaded.getConfigParts().get(0);
        assertEquals("client-name-1", part1Restored.getClientname());

        assertThat(loaded.getConfigParts().get(1), instanceOf(ServerFingerprintDataPart.class));
        ServerFingerprintDataPart part2Restored = (ServerFingerprintDataPart) loaded.getConfigParts().get(1);
        assertEquals("fingerprint-1", part2Restored.getServerFingerprint());
    }

    @Test
    void serializeRestore_none() {
        MockVirtualFile localDir = MockVirtualFileSystem.createTree("/my/local/file", "data").get("/my/local/file");
        when(idea.getMockLocalFilesystem().findFileByPath("/my/local/file")).thenReturn(localDir);

        VcsRootCacheStore store = new VcsRootCacheStore(localDir);

        VcsRootCacheStore.State originalState = store.getState();
        Element serialized = XmlSerializer.serialize(originalState);
        assertNotNull(serialized);

        VcsRootCacheStore.State restored = XmlSerializer.deserialize(serialized, VcsRootCacheStore.State.class);
        assertNotNull(restored);
        assertEquals("/my/local/file", restored.rootDirectory);
        assertSize(0, restored.configParts);

        VcsRootCacheStore loaded = new VcsRootCacheStore(restored, getClass().getClassLoader());
        assertEquals(localDir, loaded.getRootDirectory());
        assertSize(0, loaded.getConfigParts());
    }

    @Test
    void serializeRestore_complex() {
        Map<String, MockVirtualFile> tree = MockVirtualFileSystem.createTree(
                "/my/root/file", "data",
                "/my/root/config", "config data");
        VirtualFile root = tree.get("/my/root/file").getParent();
        MockVirtualFile configFile = tree.get("/my/root/config");
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root")).thenReturn(root);
        when(idea.getMockLocalFilesystem().findFileByPath("/my/root/config")).thenReturn(configFile);

        ClientNameConfigPart part1 = new ClientNameConfigPart("s1");
        part1.setClientname("c1");
        EnvCompositePart part2 = new EnvCompositePart(root);
        FileConfigPart part3 = new FileConfigPart(root, configFile);
        RequirePasswordDataPart part4 = new RequirePasswordDataPart();
        ServerFingerprintDataPart part5 = new ServerFingerprintDataPart("s5");
        part5.setServerFingerprint("f-print");
        Map<String, String> part6Props = new HashMap<>();
        part6Props.put("port", "my-port:1234");
        SimpleDataPart part6 = new SimpleDataPart(root, "s6", part6Props);
        MultipleConfigPart part7 = new MultipleConfigPart("S7", Arrays.asList(part5, part6));
        VcsRootCacheStore original = new VcsRootCacheStore(root);
        original.setConfigParts(Arrays.asList(part1, part2, part3, part4, part7));

        VcsRootCacheStore.State originalState = original.getState();
        Element serialized = XmlSerializer.serialize(originalState);
        assertNotNull(serialized);

        VcsRootCacheStore.State restored = XmlSerializer.deserialize(serialized, VcsRootCacheStore.State.class);
        assertNotNull(restored);
        assertEquals("/my/root", restored.rootDirectory);
        assertSize(5, restored.configParts);


        VcsRootCacheStore loaded = new VcsRootCacheStore(restored, getClass().getClassLoader());
        assertEquals(root, loaded.getRootDirectory());
        assertSize(5, loaded.getConfigParts());

        assertThat(loaded.getConfigParts().get(0), instanceOf(ClientNameConfigPart.class));
        assertEquals("c1", loaded.getConfigParts().get(0).getClientname());

        assertThat(loaded.getConfigParts().get(1), instanceOf(EnvCompositePart.class));
        // Nothing to check in the env.

        assertThat(loaded.getConfigParts().get(2), instanceOf(FileConfigPart.class));
        FileConfigPart part3Restored = (FileConfigPart) loaded.getConfigParts().get(2);
        // Need to construct the file object so that Windows paths are created in the same way.
        assertEquals(configFile, part3Restored.getConfigFile());

        assertThat(loaded.getConfigParts().get(3), instanceOf(RequirePasswordDataPart.class));
        // Nothing to check in require password

        assertThat(loaded.getConfigParts().get(4), instanceOf(MultipleConfigPart.class));
        MultipleConfigPart part7Restored = (MultipleConfigPart) loaded.getConfigParts().get(4);
        assertSize(2, part7Restored.getChildren());

        assertThat(part7Restored.getChildren().get(0), instanceOf(ServerFingerprintDataPart.class));
        assertEquals("f-print", part7Restored.getChildren().get(0).getServerFingerprint());

        assertThat(part7Restored.getChildren().get(1), instanceOf(SimpleDataPart.class));
        SimpleDataPart part6Restored = (SimpleDataPart) part7Restored.getChildren().get(1);
        assertEquals("my-port:1234", part6Restored.getRawServerName());
    }
}
