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

package net.groboclown.idea.p4ic.extension;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.p4ic.ProjectRule;
import net.groboclown.idea.p4ic.changes.P4ChangeListMapping;
import net.groboclown.idea.p4ic.config.Client;
import net.groboclown.idea.p4ic.config.MockP4ConfigProject;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class P4VcsTest {
    @Rule
    public final ProjectRule projectRule = new ProjectRule("project");


    @Test
    public void testMapFilePathToClient_simple() throws Exception {
        final Project project = projectRule.getProject();
        MockP4ConfigProject.Setup setup1 = MockP4ConfigProject.mkSetup(project.getBaseDir());
        MockP4ConfigProject config = new MockP4ConfigProject(project, setup1);
        P4Vcs vcs = new P4Vcs(project, config, new P4ChangeListMapping(project));
        final Map<Client, List<FilePath>> mapping = vcs.mapFilePathToClient(Arrays.asList(
                VcsUtil.getFilePath(project.getBaseDir())));

        // TODO finish
    }
}
