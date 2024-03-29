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

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface P4Icons {
    Icon CONNECTED = IconLoader.getIcon("/icons/p4connected.png", P4Icons.class);
    Icon DISCONNECTED = IconLoader.getIcon("/icons/p4disconnected.png", P4Icons.class);
    Icon SWARM = IconLoader.getIcon("/icons/swarm.png", P4Icons.class);

    // TODO make this its own icon
    Icon MIXED = CONNECTED;
    Icon UNKNOWN = DISCONNECTED;
}
