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

package net.groboclown.idea.p4ic.v2.ui.warning;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class WarningUI {
    public static void showWarnings(@NotNull Collection<WarningMessage> warnings) {
        final Map<Project, List<WarningMessage>> sorted = sortWarningsByProject(warnings);
        for (Entry<Project, List<WarningMessage>> entry : sorted.entrySet()) {
            showWarningPanel(entry.getKey(), entry.getValue());
        }
    }

    private static void showWarningPanel(@NotNull final Project project, final List<WarningMessage> warnings) {
        //final AbstractVcsHelper helper = AbstractVcsHelper.getInstance(project);
        //helper.showErrors(getErrorGroups(warnings), VcsBundle.message("message.title.annotate"));

        new WarningViewHandler(project).showWarnings(warnings);
    }



    @NotNull
    private static Map<Project, List<WarningMessage>> sortWarningsByProject(@NotNull Collection<WarningMessage> warnings) {
        Map<Project, List<WarningMessage>> ret = new HashMap<Project, List<WarningMessage>>();
        for (WarningMessage warning : warnings) {
            List<WarningMessage> list = ret.get(warning.getProject());
            if (list == null) {
                list = new ArrayList<WarningMessage>();
                ret.put(warning.getProject(), list);
            }
            list.add(warning);
        }
        return ret;
    }
}
