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

package p4ic.ext

import org.junit.Test
import org.junit.Assert

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class IdeaVersionTest {
    @Test
    void testMatches_exact() {
        Set<String> values = asSet("a.jar", "b")
        assertEquals("a.jar", IdeaVersion.matches("a.jar", values))
    }

    @Test
    void testMatches_strippedExt() {
        Set<String> values = asSet("a", "b")
        assertEquals("a", IdeaVersion.matches("a.jar", values))
    }

    @Test
    void testMatches_strippedSnapshot() {
        Set<String> values = asSet("a", "b")
        assertEquals("a", IdeaVersion.matches("a-SNAPSHOT.jar", values))
    }

    @Test
    void testMatches_strippedPatched() {
        Set<String> values = asSet("a", "b")
        assertEquals("a", IdeaVersion.matches("a-patched.jar", values))
    }

    @Test
    void testMatches_strippedVersion1() {
        Set<String> values = asSet("abc", "b")
        assertEquals("abc", IdeaVersion.matches("abc-1.jar", values))
    }

    @Test
    void testMatches_strippedVersion2() {
        Set<String> values = asSet("abc", "b")
        assertEquals("abc", IdeaVersion.matches("abc-1.2.jar", values))
    }

    @Test
    void testMatches_strippedVersion3() {
        Set<String> values = asSet("abc", "b")
        assertEquals("abc", IdeaVersion.matches("abc-12.20.313.jar", values))
    }

    @Test
    void testMatches_strippedSnapshotVersion() {
        Set<String> values = asSet("a", "b")
        assertEquals("a", IdeaVersion.matches("a-1.2-SNAPSHOT.jar", values))
    }

    @Test
    void testMatches_idea18Name() {
        Set<String> values = asSet("a-b-c")
        assertEquals("a-b-c", IdeaVersion.matches("intellij.a.b.c.jar", values))
    }

    @Test
    void testMatches_idea18PlatformName() {
        Set<String> values = asSet("a-b-c")
        assertEquals("a-b-c", IdeaVersion.matches("intellij.platform.a.b.c.jar", values))
    }

    @Test
    void testMatches_NoMatch() {
        Set<String> values = asSet("abc", "b")
        assertNull(IdeaVersion.matches("a-1.2-SNAPSHOT.jar", values))
    }


    private static Set<String> asSet(String... v) {
        Set<String> ret = new HashSet<>()
        for (String s : v) {
            ret.add(s)
        }
        return ret
    }


    private static void assertSetContains(Set<String> actual, String... expected) {
        String[] actualA = actual.toArray(new String[0])
        Arrays.sort(expected)
        Arrays.sort(actualA)
        Assert.assertArrayEquals(expected, actualA)
    }
}
