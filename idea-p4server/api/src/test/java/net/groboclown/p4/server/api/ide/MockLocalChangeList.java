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

package net.groboclown.p4.server.api.ide;

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class MockLocalChangeList extends LocalChangeList {
    private String name ="cl";
    private boolean isDefault = false;

    @Override
    public Collection<Change> getChanges() {
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public MockLocalChangeList withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public void setName(@NotNull String s) {
        name = s;
    }

    @Nullable
    @Override
    public String getComment() {
        return null;
    }

    @Override
    public void setComment(@Nullable String s) {

    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public MockLocalChangeList withIsDefault(boolean b) {
        isDefault = b;
        return this;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void setReadOnly(boolean b) {

    }

    @Nullable
    @Override
    public Object getData() {
        return null;
    }

    @Override
    public LocalChangeList copy() {
        return null;
    }
}
