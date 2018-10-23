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

package net.groboclown.p4.server.api.values;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

// supposed to be immutable, but for testing...
public class MockP4RemoteFile implements P4RemoteFile {
    private String depotPath;
    private String display;

    public MockP4RemoteFile() {

    }

    public MockP4RemoteFile(String depotPath) {
        this.depotPath = depotPath;
        this.display = depotPath;
    }

    @NotNull
    @Override
    public String getDepotPath() {
        return depotPath;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return display;
    }

    @NotNull
    @Override
    public Optional<String> getLocalPath() {
        return Optional.empty();
    }

    public MockP4RemoteFile withDepotPath(String s) {
        depotPath = s;
        display = s;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof P4RemoteFile) {
            return ((P4RemoteFile) o).getDepotPath().equals(getDepotPath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return depotPath.hashCode();
    }
}
