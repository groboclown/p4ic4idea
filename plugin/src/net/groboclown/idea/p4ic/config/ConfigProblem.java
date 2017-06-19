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

package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.p4ic.P4Bundle;
import net.groboclown.idea.p4ic.config.part.ConfigPart;
import net.groboclown.idea.p4ic.config.part.DataPart;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigProblem {
    private static final Logger LOG = Logger.getInstance(ConfigProblem.class);

    private final boolean isError;
    private final String message;
    private final Object[] args;
    private final ConfigPart source;


    public ConfigProblem(@NotNull ConfigPart source, boolean isError, @Nls @NotNull String message, Object... args) {
        this.source = source;
        this.message = message;
        this.args = args;
        this.isError = isError;
    }

    public ConfigProblem(@Nullable ConfigPart source, @NotNull Exception ex) {
        this.source = source;
        this.message = "configproblem.exception";
        this.args = new Object[] { ex.getMessage(), ex.getClass().getName(), ex.getClass().getSimpleName() };
        this.isError = true;
        LOG.info("ConfigProblem from " + source, ex);
    }

    @Nullable
    public VirtualFile getRootPath() {
        if (source != null && source instanceof DataPart) {
            return ((DataPart) source).getRootPath();
        }
        return null;
    }

    @Nullable
    public ConfigPart getSource() {
        return source;
    }

    @NonNls
    public String getMessage() {
        return P4Bundle.message(message, args);
    }

    public boolean isError() {
        return isError;
    }

    @Override
    public String toString() {
        return "problem(" + source + ": " + message + " -> " + getMessage() + ")";
    }
}
