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

package net.groboclown.p4.server.api.config.part;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.ConfigProblem;
import org.jetbrains.annotations.Nullable;

class TestableConfigProblem
        extends ConfigProblem {
    private final boolean error;

    private TestableConfigProblem(boolean error) {
        this.error = error;
    }

    static ConfigProblem createError() {
        return new TestableConfigProblem(true);
    }

    static ConfigProblem createWarning() {
        return new TestableConfigProblem(false);
    }

    @Nullable
    @Override
    public VirtualFile getRootPath() {
        return null;
    }

    @Nullable
    @Override
    public ConfigPart getSource() {
        return null;
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public boolean isError() {
        return error;
    }
}
