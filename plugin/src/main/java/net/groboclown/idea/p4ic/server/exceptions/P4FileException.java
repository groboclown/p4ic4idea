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
package net.groboclown.idea.p4ic.server.exceptions;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class P4FileException extends P4Exception {
    public P4FileException(Throwable t) {
        super(t);
    }

    public P4FileException(@NotNull VirtualFile vf, Throwable t) {
        super(vf.getPath(), t);
    }

    public P4FileException(@NotNull FilePath fp, Throwable t) {
        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        super(fp.getIOFile().getAbsolutePath(), t);
    }

    public P4FileException(@NotNull String message, @NotNull VirtualFile vf, Throwable t) {
        super(message + ": " + vf.getPath(), t);
    }

    public P4FileException(@NotNull String message, @NotNull FilePath fp, Throwable t) {
        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        super(message + ": " + fp.getIOFile().getAbsolutePath(), t);
    }


    public P4FileException(@NotNull VirtualFile vf) {
        super(vf.getPath());
    }

    public P4FileException(@NotNull FilePath fp) {
        // Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
        // use this instead: getIOFile().getAbsolutePath()
        super(fp.getIOFile().getAbsolutePath());
    }

    public P4FileException(@NotNull String message, @NotNull VirtualFile vf) {
        super(message + ": " + vf.getPath());
    }

    public P4FileException(@NotNull String message, @NotNull FilePath fp) {
        super(message + ": " + fp.getPath());
    }

    public P4FileException(String s) {
        super(s);
    }
}
