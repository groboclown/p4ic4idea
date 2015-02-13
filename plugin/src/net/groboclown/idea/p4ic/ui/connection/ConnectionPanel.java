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
package net.groboclown.idea.p4ic.ui.connection;

import net.groboclown.idea.p4ic.config.ManualP4Config;
import net.groboclown.idea.p4ic.config.P4Config;
import net.groboclown.idea.p4ic.server.exceptions.P4DisconnectedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface ConnectionPanel {
    public boolean isModified(@NotNull P4Config config);

    /**
     *
     * @return name in the drop-down picker
     */
    public String getName();

    public String getDescription();

    /**
     *
     * @return the corresponding authentication method that this panel corresponds to.
     */
    public P4Config.ConnectionMethod getConnectionMethod();

    public void loadSettingsIntoGUI(@NotNull P4Config config);

    public void saveSettingsToConfig(@NotNull ManualP4Config config);

    /*
    @Nullable
    public P4Config loadChildConfig(@NotNull P4Config config)
            throws P4DisconnectedException;
    */
}
