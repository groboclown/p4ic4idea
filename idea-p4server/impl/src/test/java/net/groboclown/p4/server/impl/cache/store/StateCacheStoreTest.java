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

import com.intellij.util.xmlb.XmlSerializer;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import net.groboclown.p4.server.impl.config.part.ClientNameConfigPart;
import net.groboclown.p4.server.impl.config.part.ServerFingerprintDataPart;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StateCacheStoreTest {
    @Test
    void serialize() {
        VcsRootCacheStore store = new VcsRootCacheStore(MockVirtualFileSystem.createRoot());
        ClientNameConfigPart part1 = new ClientNameConfigPart("source");
        part1.setClientname("client-name-1");
        ServerFingerprintDataPart part2 = new ServerFingerprintDataPart("source1");
        part2.setServerFingerprint("fingerprint-1");
        store.setConfigParts(Arrays.asList(part1, part2));

        Element serialized = XmlSerializer.serialize(store.getState());
        assertNotNull(serialized);

        VcsRootCacheStore.State restored = XmlSerializer.deserialize(serialized, VcsRootCacheStore.State.class);
        assertNotNull(restored);
        assertEquals("/my/local/file", restored.rootDirectory);
        assertSize(2, restored.configParts);
        assertNotNull(restored.configParts.get(0));
        assertThat(restored.configParts.get(0), instanceOf(ClientNameConfigPart.class));
        assertNotNull(restored.configParts.get(1));
        assertThat(restored.configParts.get(1), instanceOf(ServerFingerprintDataPart.class));
    }
}