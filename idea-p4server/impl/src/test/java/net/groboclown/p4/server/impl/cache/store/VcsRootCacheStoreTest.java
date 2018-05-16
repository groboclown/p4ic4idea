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
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.api.config.part.ConfigPart;
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

import static org.junit.jupiter.api.Assertions.*;

class VcsRootCacheStoreTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getState() {
        VirtualFile root = MockVirtualFileSystem.createRoot();
        ClientNameConfigPart part1 = new ClientNameConfigPart("s1");
        part1.setClientname("c1");
        EnvCompositePart part2 = new EnvCompositePart(root);
        FileConfigPart part3 = new FileConfigPart(root, null);
        RequirePasswordDataPart part4 = new RequirePasswordDataPart();
        ServerFingerprintDataPart part5 = new ServerFingerprintDataPart("s5");
        part5.setServerFingerprint("f-print");
        SimpleDataPart part6 = new SimpleDataPart(root, "s6", null);
        MultipleConfigPart part7 = new MultipleConfigPart("s7", Arrays.asList(part5, part6));
        VcsRootCacheStore original = new VcsRootCacheStore(root);
        original.setConfigParts(Arrays.asList(part1, part2, part3, part4, part7));

        VcsRootCacheStore.State state = original.getState();
        Element serialized = XmlSerializer.serialize(state);
        VcsRootCacheStore.State restoredState = XmlSerializer.deserialize(serialized, VcsRootCacheStore.State.class);
        VcsRootCacheStore restored = new VcsRootCacheStore(restoredState, null);

        // FIXME add tests
    }

}
