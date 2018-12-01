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

package net.groboclown.idea.p4ic.v2.historic_compat;

import com.intellij.openapi.components.*;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Early versions of the plugin could stored passwords in an
 * insecure way.  The current version uses the IDEA password
 * manager to avoid this issue.  However, without an explicit
 * cleanup of the old contents, the old issue will still
 * linger on the user's computer.
 */
@State(
        // The old, historic name.  It must remain this for
        // the cleanup to work correctly.
        name = "PerforcePasswordStore",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/perforce.xml")
        }
)
public class ClearPasswordStoreService implements ApplicationComponent, PersistentStateComponent<Element> {
    @Override
    public void initComponent() {
        // do nothing
    }

    @Override
    public void disposeComponent() {
        // do nothing
    }

    @NotNull
    @Override
    public String getComponentName() {
        // Kept for historic reasons.
        return "PerforcePasswordStore";
    }

    @Nullable
    @Override
    public Element getState() {
        // Return the cleared out storage XML element.
        return new Element("storage");
    }

    @Override
    public void loadState(final Element state) {
        // do nothing
    }
}
