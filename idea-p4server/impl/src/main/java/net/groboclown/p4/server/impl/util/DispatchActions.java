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

package net.groboclown.p4.server.impl.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

/**
 * Performs operations that, if run within the dispatch thread, require special handling.
 */
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
}
