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
package net.groboclown.p4plugin.extension;

import com.intellij.openapi.util.Pair;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.ClientConfigRoot;
import net.groboclown.p4.server.api.cache.IdeChangelistMap;
import net.groboclown.p4.server.api.cache.IdeFileMap;
import net.groboclown.p4plugin.PluginSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class P4ChangelistListenerTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    final PluginSetup vcs = new PluginSetup();

    /**
     * Verify that a newly created IDE changelist does not trigger the creation or association of a Perforce
     * changelist.
     */
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void offlineCreateNewIdeChangelist(TemporaryFolder tmp)
            throws InterruptedException {
        List<Throwable> errors = new ArrayList<>();
        vcs.idea.useInlineThreading(errors);

        // Setup offline mode
        ClientConfigRoot root = vcs.addClientConfigRoot(tmp, "client");
        assertNotNull(root.getClientRootDir());
        vcs.goOffline(root);
        assertFalse(vcs.registry.isOnline(root.getClientConfig().getClientServerRef()));

        // Add the pending add action.
        MockLocalChangeList changeList =
                vcs.addIdeChangelist("Test change", "A test change", false);

        // Run the test.
        P4ChangelistListener listener = new P4ChangelistListener(vcs.idea.getMockProject());
        listener.changeListAdded(changeList);

        Pair<IdeChangelistMap, IdeFileMap>
                cacheState = vcs.cacheComponent.getServerOpenedCache();
        assertEmpty(cacheState.first.getP4ChangesFor(changeList));

        // TODO any additional validations?
    }

    @Test
    void moveFileToNewChangelist() {
        // FIXME implement
        // This depends upon the "perform" functionality generating an event to cache the
        // new changelist before the request to pull the just-cached changelist.
    }
}