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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class P4IntegrateEnvironment
        implements UpdateEnvironment {
    @Override
    public void fillGroups(UpdatedFiles updatedFiles) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @NotNull
    @Override
    public UpdateSession updateDirectories(@NotNull FilePath[] filePaths, UpdatedFiles updatedFiles,
            ProgressIndicator progressIndicator, @NotNull Ref<SequentialUpdatesContext> ref)
            throws ProcessCanceledException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Nullable
    @Override
    public Configurable createConfigurable(Collection<FilePath> collection) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public boolean validateOptions(Collection<FilePath> collection) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }
}
