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
package net.groboclown.idea.p4ic.background;

import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.compat.UICompat;
import net.groboclown.idea.p4ic.ui.ErrorDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class that runs tasks in the background.  The background tasks MUST ONLY be fire-and-forget, otherwise
 * really bad deadlocks can happen.  Do not use futures to retrieve results from the calling thread!
 */
public class Background {
    private Background() {}

    // TODO this is no longer needed.  The IntelliJ threading model is setup so that the commands are (generally)
    // run in the correct thread.  It's a rare need to have something be explicitly pushed off into another
    // thread.
    public static void runInBackground(@NotNull final Project project, @NotNull final String title, @NotNull PerformInBackgroundOption option,
                                       @NotNull final ER runner) {
        ProgressIndicator indicator = UICompat.getInstance().getGlobalProgressIndicator();
        if (indicator == null) {
            indicator = new DelegatingProgressIndicator();
        }
        try {
            runner.run(indicator);
        } catch (Exception e) {
            ErrorDialog.logError(project, title, e);
        }
    }


    public static interface ER {
        void run(@NotNull ProgressIndicator indicator) throws Exception;
    }
}
