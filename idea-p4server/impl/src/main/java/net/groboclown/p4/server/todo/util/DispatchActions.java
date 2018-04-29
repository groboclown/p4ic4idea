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

package net.groboclown.p4.server.todo.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.todo.FileOperationErrorManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Performs operations that, if run within the dispatch thread, require special handling.
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class DispatchActions {
    /**
     * Perform a write action.  If the current thread is the event dispatch thread,
     * then the run action will block until all other write actions complete.
     *
     * @param run function to run that contains a write action.
     */
    public static void writeAction(@NotNull Runnable run) {
        final Application app = ApplicationManager.getApplication();
        if (app.isDispatchThread() && !app.isWriteAccessAllowed()) {
            app.runWriteAction(run);
        } else {
            run.run();
        }
    }

    public static <T> T writeAction(@NotNull Computable<T> comp) {
        final Application app = ApplicationManager.getApplication();
        if (app.isDispatchThread() && !app.isWriteAccessAllowed()) {
            return app.runWriteAction(comp);
        } else {
            return comp.compute();
        }
    }

    public static void readAction(@NotNull Runnable run) {
        final Application app = ApplicationManager.getApplication();
        if (app.isDispatchThread() && !app.isReadAccessAllowed()) {
            app.runReadAction(run);
        } else {
            run.run();
        }
    }

    public static <T> T readAction(@NotNull Computable<T> comp) {
        final Application app = ApplicationManager.getApplication();
        if (app.isDispatchThread() && !app.isReadAccessAllowed()) {
            return app.runReadAction(comp);
        } else {
            return comp.compute();
        }
    }

    public static boolean setWritable(@NotNull final FilePath fp, @NotNull final FileOperationErrorManager errMgr) {
        if (fp.getVirtualFile() != null) {
            return setWritable(fp.getVirtualFile(), errMgr);
        }
        return writeAction(() -> {
            boolean ret = fp.getIOFile().setWritable(true);
            if (! ret) {
                errMgr.warnCouldNotMakeWritable(fp);
            }
            return ret;
        });
    }

    public static boolean setWritable(@NotNull final VirtualFile vf, @NotNull final FileOperationErrorManager errMgr) {
        if (! vf.isInLocalFileSystem()) {
            return false;
        }
        return writeAction(() -> {
            try {
                vf.setWritable(true);
                return true;
            } catch (IOException e) {
                errMgr.warnCouldNotMakeWritable(vf);
                return false;
            }
        });
    }

    public static void setWritable(@NotNull final VirtualFile[] files, @NotNull final FileOperationErrorManager errMgr) {
        writeAction(() -> {
            for (VirtualFile vf: files) {
                try {
                    vf.setWritable(true);
                } catch (IOException e) {
                    errMgr.warnCouldNotMakeWritable(vf);
                }
            }
        });
    }
}
