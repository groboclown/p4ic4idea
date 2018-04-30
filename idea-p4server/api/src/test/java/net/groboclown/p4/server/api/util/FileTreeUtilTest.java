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
package net.groboclown.p4.server.api.util;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.idea.extensions.IdeaLightweightExtension;
import net.groboclown.idea.mock.MockVirtualFile;
import net.groboclown.idea.mock.MockVirtualFileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static net.groboclown.idea.ExtAsserts.assertSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unfortunately, due to the need to mock out much of the underlying IDE API, this tests
 * the mock API more than the utility class.
 */
class FileTreeUtilTest {
    // Used for setting up the underlying services.
    @SuppressWarnings("unused")
    @RegisterExtension
    IdeaLightweightExtension idea = new IdeaLightweightExtension();

    @Test
    void getTree_FilePath() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents"
        );

        FilePath five = vfs.get("one/two/three/four/five.txt").asFilePath();
        List<FilePath> tree =
                FileTreeUtil.getTree(five);
        assertSize(6, tree);
        assertEquals(five, tree.get(0));
        assertEquals(five.getParentPath(), tree.get(1));
        assertEquals(five.getParentPath().getParentPath(), tree.get(2));
        assertEquals(five.getParentPath().getParentPath().getParentPath(), tree.get(3));
        assertEquals(five.getParentPath().getParentPath().getParentPath().getParentPath(), tree.get(4));
        assertEquals(five.getParentPath().getParentPath().getParentPath().getParentPath().getParentPath(), tree.get(5));
        assertNotNull(tree.get(5));
    }

    @Test
    void getTreeTo_FilePath_FilePath() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents"
        );

        FilePath five = vfs.get("one/two/three/four/five.txt").asFilePath();
        FilePath parent = five.getParentPath().getParentPath().getParentPath(); // "one/two"
        List<FilePath> tree = FileTreeUtil.getTreeTo(five, parent);
        assertSize(4, tree);
        assertEquals(five, tree.get(0));
        assertEquals(five.getParentPath(), tree.get(1));
        assertEquals(five.getParentPath().getParentPath(), tree.get(2));
        assertEquals(parent, tree.get(3));
    }

    @Test
    void getTreeTo_VirtualFile_VirtualFile() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents"
        );

        VirtualFile five = vfs.get("one/two/three/four/five.txt");
        VirtualFile parent = five.getParent().getParent().getParent(); // "one/two"
        List<VirtualFile> tree = FileTreeUtil.getTreeTo(five, parent);
        assertSize(4, tree);
        assertEquals(five, tree.get(0));
        assertEquals(five.getParent(), tree.get(1));
        assertEquals(five.getParent().getParent(), tree.get(2));
        assertEquals(parent, tree.get(3));
    }

    @Test
    void getPathDepth_FilePath_FilePath() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents",
                "other/file.txt", "contents"
        );

        FilePath five = vfs.get("one/two/three/four/five.txt").asFilePath();
        FilePath other = vfs.get("other/file.txt").asFilePath();
        assertEquals(
                0,
                FileTreeUtil.getPathDepth(five, five)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five, (FilePath) null)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five.getParentPath(), five)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five, other)
        );
        assertEquals(
                1,
                FileTreeUtil.getPathDepth(five, five.getParentPath())
        );
        assertEquals(
                2,
                FileTreeUtil.getPathDepth(five, five.getParentPath().getParentPath())
        );
        assertEquals(
                3,
                FileTreeUtil.getPathDepth(five, five.getParentPath().getParentPath().getParentPath())
        );
        assertEquals(
                4,
                FileTreeUtil.getPathDepth(five, five.getParentPath().getParentPath().getParentPath().getParentPath())
        );
        assertEquals(
                5,
                FileTreeUtil.getPathDepth(
                        five,
                        five.getParentPath().getParentPath().getParentPath().getParentPath().getParentPath())
        );
    }

    @Test
    void getPathDepth_VirtualFile_VirtualFile() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents",
                "other/file.txt", "contents"
        );

        VirtualFile five = vfs.get("one/two/three/four/five.txt");
        VirtualFile other = vfs.get("other/file.txt");
        assertEquals(
                0,
                FileTreeUtil.getPathDepth(five, five)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five, null)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five.getParent(), five)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five, other)
        );
        assertEquals(
                1,
                FileTreeUtil.getPathDepth(five, five.getParent())
        );
        assertEquals(
                2,
                FileTreeUtil.getPathDepth(five, five.getParent().getParent())
        );
        assertEquals(
                3,
                FileTreeUtil.getPathDepth(five, five.getParent().getParent().getParent())
        );
        assertEquals(
                4,
                FileTreeUtil.getPathDepth(five, five.getParent().getParent().getParent().getParent())
        );
        assertEquals(
                5,
                FileTreeUtil.getPathDepth(
                        five,
                        five.getParent().getParent().getParent().getParent().getParent())
        );
    }

    @Test
    void getPathDepth_FilePath_VirtualFile() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "one/two/three/four/five/six.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents",
                "other/file.txt", "contents"
        );

        FilePath five = vfs.get("one/two/three/four/five.txt").asFilePath();
        VirtualFile other = vfs.get("other/file.txt");
        VirtualFile p4 = vfs.get("one/two/three/four/five.txt").getParent();
        VirtualFile p3 = p4.getParent();
        VirtualFile p2 = p3.getParent();
        VirtualFile p1 = p2.getParent();
        VirtualFile p0 = p1.getParent();
        assertEquals(
                0,
                FileTreeUtil.getPathDepth(five, five)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five, (VirtualFile) null)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(vfs.get("one/two/three/four/five/six.txt").asFilePath(),
                        five)
        );
        assertEquals(
                -1,
                FileTreeUtil.getPathDepth(five, other)
        );
        assertEquals(
                1,
                FileTreeUtil.getPathDepth(five, p4)
        );
        assertEquals(
                2,
                FileTreeUtil.getPathDepth(five, p3)
        );
        assertEquals(
                3,
                FileTreeUtil.getPathDepth(five, p2)
        );
        assertEquals(
                4,
                FileTreeUtil.getPathDepth(five, p1)
        );
        assertEquals(
                5,
                FileTreeUtil.getPathDepth(five, p0)
        );
    }


    @Test
    void isSameOrUnder() {
        Map<String, MockVirtualFile> vfs = MockVirtualFileSystem.createTree(
                "one/two/three/four/five.txt", "contents",
                "root.txt", "contents",
                "one/root.txt", "contents",
                "two/root.txt", "contents",
                "other/file.txt", "contents"
        );
        FilePath five = vfs.get("one/two/three/four/five.txt").asFilePath();
        FilePath other = vfs.get("other/file.txt").asFilePath();

        assertFalse(FileTreeUtil.isSameOrUnder(five, other));
        assertFalse(FileTreeUtil.isSameOrUnder(other, five));
        assertFalse(FileTreeUtil.isSameOrUnder(null, five));
        assertTrue(FileTreeUtil.isSameOrUnder(five.getParentPath(), five));
        assertTrue(FileTreeUtil.isSameOrUnder(five.getParentPath().getParentPath(), five));
        assertTrue(FileTreeUtil.isSameOrUnder(five.getParentPath().getParentPath().getParentPath(), five));
        assertTrue(FileTreeUtil.isSameOrUnder(five.getParentPath().getParentPath().getParentPath().getParentPath(), five));
    }
}