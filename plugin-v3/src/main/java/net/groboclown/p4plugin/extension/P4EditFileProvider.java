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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.EditFileProvider;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.exceptions.VcsInterruptedException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is only called when the file is changed from
 * read-only to writable.
 */
public class P4EditFileProvider implements EditFileProvider {
    private static final Logger LOG = Logger.getInstance(P4EditFileProvider.class);

    public static final String EDIT = "Edit files";


    private final P4Vcs vcs;

    P4EditFileProvider(@NotNull P4Vcs vcs) {
        this.vcs = vcs;
    }


    // This method is called with nearly every keystroke, so it must be very, very
    // performant.
    @Override
    public void editFiles(final VirtualFile[] allFiles) throws VcsException {
        if (allFiles == null || allFiles.length <= 0) {
            return;
        }

        // FIXME
        throw new IllegalStateException("not implemented");
    }

    @Override
    public String getRequestText() {
        return null;
    }

    private void makeWritable(@NotNull final VirtualFile[] allFiles) {
        // FIXME
        throw new IllegalStateException("not implemented");
    }

    private void openForEdit(final VirtualFile[] allFiles) throws VcsInterruptedException {
        // FIXME
        throw new IllegalStateException("not implemented");
    }
}
