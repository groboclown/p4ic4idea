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

package net.groboclown.p4.server.impl.ignore;

import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IgnoreFileSetTest {
    private Map<String, MockVirtualFile> tree;
    private IgnoreFileSet rootIgnoreFileSet;
    private IgnoreFileSet xIgnoreFileSet;

    @Before
    public void before()
            throws IOException {
        tree = MockVirtualFileSystem.createTree(
                "/.ignore",
                    "\n"
                    + "# Ignore file\n"
                    + "\n"
                    + "!/a/b/\n"
                    + "a/b**def.jp*g\n",
                "/a/b/c.txt",
                    "my-file-text",
                "/a/b/c/def.jpeg",
                    "def.jpeg contents",
                "/a/bed/adef.jpeg",
                    "adef.jpeg contents",
                "/a/bdef.jpeg",
                    "bdef.jpeg contents",
                "/a/a/badef.jpeg",
                    "badef.jpeg contents",

                "/x/.ignore",
                    "\n"
                    + "# Ignore file\n"
                    + "\n"
                    + "!/a/b/\n"
                    + "a/b**def.jp*g\n",
                "/x/a/b/c.txt",
                    "my-file-text",
                "/x/a/b/c/def.jpeg",
                    "def.jpeg contents",
                "/x/a/bed/adef.jpeg",
                    "adef.jpeg contents",
                "/x/a/bdef.jpeg",
                    "bdef.jpeg contents",
                "/x/a/a/badef.jpeg",
                    "badef.jpeg contents"
        );
        rootIgnoreFileSet = IgnoreFileSet.create(tree.get("/.ignore"));
        xIgnoreFileSet = IgnoreFileSet.create(tree.get("/x/.ignore"));
    }

    @Test
    public void testIsCovered_Null() {
        assertThat(rootIgnoreFileSet.isCoveredByIgnoreFile(null), is(false));
    }

    @Test
    public void testIsCovered_Yes_ExplitNotIgnored() {
        assertThat(rootIgnoreFileSet.isCoveredByIgnoreFile(tree.get("/a/b/c.txt")), is(true));
    }

    @Test
    public void testIsCovered_Yes_Ignored() {
        assertThat(rootIgnoreFileSet.isCoveredByIgnoreFile(tree.get("/a/a/badef.jpeg")), is(true));
    }

    @Test
    public void testIsCovered_Yes_NotListed() {
        assertThat(rootIgnoreFileSet.isCoveredByIgnoreFile(tree.get("/x/a/b/c.txt")), is(true));
    }

    @Test
    public void testIsCovered_OtherPath_No() {
        assertThat(xIgnoreFileSet.isCoveredByIgnoreFile(tree.get("/a/b/c.txt")), is(false));
    }

    @Test
    public void testIsIgnored_Covered_ExplitNotIgnored() {
        assertThat(rootIgnoreFileSet.isIgnored(tree.get("/a/b/c/def.jpeg")), is(false));
    }

    @Test
    public void testIsIgnored_Covered_Ignored() {
        assertThat(rootIgnoreFileSet.isIgnored(tree.get("/x/a/b/c/def.jpeg")), is(true));
    }

    @Test
    public void testIsIgnored_Covered_NotListed() {
        assertThat(rootIgnoreFileSet.isIgnored(tree.get("/x/a/b/c.txt")), is(false));
    }

    @Test
    public void testIsIgnored_OtherPath_No() {
        assertThat(xIgnoreFileSet.isIgnored(tree.get("/a/b/c.txt")), is(false));
    }
}
