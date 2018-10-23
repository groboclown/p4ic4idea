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
package net.groboclown.p4plugin.util;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.vcsUtil.VcsUtil;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import net.groboclown.p4.server.impl.values.P4RemoteFileImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteFileUtilTest {
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void testSplitDepotPaths_simple() {
        assertEquals(
                Collections.singletonList("depot"),
                RemoteFileUtil.splitDepotPaths("//depot")
        );
    }

    @Test
    void testSplitDepotPaths_many() {
        assertEquals(
                Arrays.asList("depot", "a", "b", "c", "d", "e", "f", "g"),
                RemoteFileUtil.splitDepotPaths("//depot//a/b////c/d/e/f/g/")
        );
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void testSplitFilePath_simple(TemporaryFolder tmpDir) {
        FilePath src = VcsUtil.getFilePath(tmpDir.newFile("a/test.txt"));
        FilePath dest = VcsUtil.getFilePath(tmpDir.newFile("b/test.txt"));
        P4RemoteFileImpl local = new P4RemoteFileImpl("//depot/x/y/a/test.txt", "//depot/x/y/a/test.txt", null);
        P4RemoteFileImpl remote = new P4RemoteFileImpl("//depot/x/y/b/test.txt", "//depot/x/y/b/test.txt", null);

        assertEquals(
                dest,
                RemoteFileUtil.createRelativePath(src, local, remote)
        );
    }
}
