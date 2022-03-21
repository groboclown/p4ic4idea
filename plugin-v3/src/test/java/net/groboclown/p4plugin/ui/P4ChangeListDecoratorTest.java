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
package net.groboclown.p4plugin.ui;

import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.idea.mock.MockLocalChangeList;
import net.groboclown.p4.server.api.RootedClientConfig;
import net.groboclown.p4.server.api.values.P4ChangelistId;
import net.groboclown.p4plugin.PluginSetup;
import net.groboclown.p4plugin.mock.MockColoredTreeCellRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import static net.groboclown.idea.ExtAsserts.assertEmpty;
import static org.junit.jupiter.api.Assertions.*;

class P4ChangeListDecoratorTest {
    @SuppressWarnings("WeakerAccess")
    @RegisterExtension
    final PluginSetup vcs = new PluginSetup();


    /**
     * Validate that when offline and no corresponding P4 changelist, an IDE change list is not
     * decorated.
     */
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void offlineNoP4ChangeDecorateChangeList(TemporaryFolder tmp) {
        MockLocalChangeList changeList =
                vcs.addIdeChangelist("new change", "a test change", false);
        P4ChangeListDecorator decorator = new P4ChangeListDecorator(vcs.getMockProject());
        MockColoredTreeCellRenderer renderer = new MockColoredTreeCellRenderer();
        decorator.decorateChangeList(changeList, renderer, false, false, false);
        assertEmpty(renderer.appendedText);
    }


    /**
     * Validate that when offline and no corresponding P4 changelist, an IDE change list is not
     * decorated.
     */
    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void offlineNoP4ChangeDecorateChangeListWithDefault(TemporaryFolder tmp) {

        // Setup offline mode
        RootedClientConfig root = vcs.addClientConfigRoot(tmp, "client");
        assertNotNull(root.getClientRootDir());
        vcs.goOffline(root);
        assertFalse(vcs.registry.isOnline(root.getClientConfig().getClientServerRef()));

        P4ChangelistId defaultP4Changelist = vcs.addDefaultChangelist(root);
        MockLocalChangeList changeList =
                vcs.addIdeChangelist("new change", "a test change", false);
        P4ChangeListDecorator decorator = new P4ChangeListDecorator(vcs.getMockProject());
        MockColoredTreeCellRenderer renderer = new MockColoredTreeCellRenderer();
        decorator.decorateChangeList(changeList, renderer, false, false, false);
        assertEmpty(renderer.appendedText);
    }
}
