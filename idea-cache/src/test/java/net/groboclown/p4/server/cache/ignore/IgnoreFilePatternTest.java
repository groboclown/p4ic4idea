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
package net.groboclown.p4.server.cache.ignore;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class IgnoreFilePatternTest {

    @Test
    public void testPatternPositive_simpleOne() {
        IgnoreFilePattern pattern = new IgnoreFilePattern(IgnoreFilePattern.createPattern("/"), false);
        assertThat(pattern.isIgnoreMatchType(), is(true));
        assertThat(pattern.matches(Collections.singletonList("a")), is(true));
    }

    @Test
    public void testPatternPositive_simpleTwo() {
        IgnoreFilePattern pattern = new IgnoreFilePattern(IgnoreFilePattern.createPattern("/"), false);
        assertThat(pattern.isIgnoreMatchType(), is(true));
        assertThat(pattern.matches(Arrays.asList("a", "b")), is(true));
    }

    @Test
    public void testPatternPositive_complexOne() {
        IgnoreFilePattern pattern = new IgnoreFilePattern(IgnoreFilePattern.createPattern("a/**b.jp*g"), false);
        assertThat(pattern.isIgnoreMatchType(), is(true));
        assertThat(pattern.matches(Arrays.asList("a", "b")), is(false));
        assertThat(pattern.matches(Arrays.asList("a", "b.j")), is(false));
        assertThat(pattern.matches(Arrays.asList("a", "b.jp")), is(false));
        assertThat(pattern.matches(Arrays.asList("a", "b.jpeg")), is(true));
        assertThat(pattern.matches(Arrays.asList("a", "c", "x", "b.jpeg")), is(true));
        assertThat(pattern.matches(Arrays.asList("a", "c", "x", "abb.jpeg")), is(true));
        assertThat(pattern.matches(Arrays.asList("a", "c", "x", "bq.jpeg")), is(false));
        assertThat(pattern.matches(Arrays.asList("c", "x", "abb.jpeg")), is(false));
    }


    @Test
    public void testCreatePattern_Empty() {
        try {
            IgnoreFilePattern.createPattern("");
            fail("did not throw IAE");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("pattern cannot be empty"));
        }
    }

    @Test
    public void testCreatePattern_Slash() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/");
        assertThat(res, instanceOf(IgnoreFilePattern.StrictStarStarPart.class));
        assertThat(res.match("a"), isMatch().isLast().hasNext());
        assertThat(res.match(""), isMatch().isLast().hasNext());
    }

    @Test
    public void testCreatePattern_StarStar() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("**");
        assertThat(res, instanceOf(IgnoreFilePattern.StrictStarStarPart.class));
        assertThat(res.match("a"), isMatch().isLast().hasNext());
        assertThat(res.match(""), isMatch().isLast().hasNext());

        IgnoreFilePattern.PathNameMatchResult match = res.match("a");
        assertThat(match.next, is(res));
    }

    @Test
    public void testCreatePattern_SlashStarStar() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/**");
        assertThat(res, instanceOf(IgnoreFilePattern.StrictStarStarPart.class));
        assertThat(res.match("a"), isMatch().isLast().hasNext());
        assertThat(res.match(""), isMatch().isLast().hasNext());

        IgnoreFilePattern.PathNameMatchResult match = res.match("a");
        assertThat(match.next, is(res));
    }

    @Test
    public void testCreatePattern_SlashAStar() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/a*");
        assertThat(res, instanceOf(IgnoreFilePattern.StarMatchPart.class));
        assertThat(res.match("a"), isMatch().isLast());
        assertThat(res.match("ab"), isMatch().isLast());
        assertThat(res.match("b"), notMatch());
        assertThat(res.match("ba"), notMatch());
        assertThat(res.match(""), notMatch());
    }

    @Test
    public void testCreatePattern_SlashStarA() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/*a");
        assertThat(res, instanceOf(IgnoreFilePattern.StarMatchPart.class));
        assertThat(res.match("a"), isMatch().isLast());
        assertThat(res.match("ba"), isMatch().isLast());
        assertThat(res.match("b"), notMatch());
        assertThat(res.match("ab"), notMatch());
        assertThat(res.match(""), notMatch());
    }

    @Test
    public void testCreatePattern_SlashAStarB() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/a*b");
        assertThat(res, instanceOf(IgnoreFilePattern.StarMatchPart.class));
        assertThat(res.match("a"), notMatch());
        assertThat(res.match("ab"), isMatch().isLast());
        assertThat(res.match("b"), notMatch());
        assertThat(res.match("ab"), isMatch().isLast());
        assertThat(res.match("acb"), isMatch().isLast());
        assertThat(res.match("abc"), notMatch());
        assertThat(res.match(""), notMatch());
    }

    @Test
    public void testCreatePattern_SlashAStarBStarC() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/a*b*c");
        assertThat(res, instanceOf(IgnoreFilePattern.StarMatchPart.class));
        assertThat(res.match("a"), notMatch());
        assertThat(res.match("b"), notMatch());
        assertThat(res.match("c"), notMatch());
        assertThat(res.match("ab"), notMatch());
        assertThat(res.match("bc"), notMatch());
        assertThat(res.match("aeb"), notMatch());
        assertThat(res.match("bec"), notMatch());
        assertThat(res.match("acb"), notMatch());
        assertThat(res.match("bac"), notMatch());
        assertThat(res.match("acb"), notMatch());
        assertThat(res.match("abc"), isMatch().isLast());
        assertThat(res.match("aabbcc"), isMatch().isLast());
        assertThat(res.match("aebec"), isMatch().isLast());
        assertThat(res.match(""), notMatch());
    }

    // This is the first place where we actually call recursively, but it still isn't
    // getting into the super complex cases.
    @Test
    public void testCreatePattern_SlashAStarBStarCStarD() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/a*b*c*d");
        assertThat(res, instanceOf(IgnoreFilePattern.StarMatchPart.class));
        assertThat(res.match("a"), notMatch());
        assertThat(res.match("b"), notMatch());
        assertThat(res.match("c"), notMatch());
        assertThat(res.match("ab"), notMatch());
        assertThat(res.match("bc"), notMatch());
        assertThat(res.match("aeb"), notMatch());
        assertThat(res.match("bec"), notMatch());
        assertThat(res.match("acb"), notMatch());
        assertThat(res.match("bac"), notMatch());
        assertThat(res.match("acb"), notMatch());
        assertThat(res.match("abd"), notMatch());
        assertThat(res.match("abc"), notMatch());
        assertThat(res.match("abcde"), notMatch());
        assertThat(res.match("bcd"), notMatch());
        assertThat(res.match("ad"), notMatch());
        assertThat(res.match("abed"), notMatch());
        assertThat(res.match("abcd"), isMatch().isLast());
        assertThat(res.match("aabbccdd"), isMatch().isLast());
        assertThat(res.match("aebeced"), isMatch().isLast());
        assertThat(res.match(""), notMatch());
    }

    @Test
    public void testCreatePattern_SlashAStarBStarCStarDStarE() {
        IgnoreFilePattern.PathPart res = IgnoreFilePattern.createPattern("/a*b*c*d*e");
        assertThat(res, instanceOf(IgnoreFilePattern.StarMatchPart.class));
        assertThat(res.match("a"), notMatch());
        assertThat(res.match("b"), notMatch());
        assertThat(res.match("c"), notMatch());
        assertThat(res.match("ab"), notMatch());
        assertThat(res.match("bc"), notMatch());
        assertThat(res.match("aeb"), notMatch());
        assertThat(res.match("bec"), notMatch());
        assertThat(res.match("acb"), notMatch());
        assertThat(res.match("bac"), notMatch());
        assertThat(res.match("acb"), notMatch());
        assertThat(res.match("abd"), notMatch());
        assertThat(res.match("abc"), notMatch());
        assertThat(res.match("abcd"), notMatch());
        assertThat(res.match("bcd"), notMatch());
        assertThat(res.match("ad"), notMatch());
        assertThat(res.match("abed"), notMatch());
        assertThat(res.match("abcde"), isMatch().isLast());
        assertThat(res.match("aabbcbcdcddee"), isMatch().isLast());
        assertThat(res.match("aebecedefe"), isMatch().isLast());
        assertThat(res.match(""), notMatch());
    }

    @Test
    public void testCreatePattern_DoublePath() {
        IgnoreFilePattern.PathPart res1 = IgnoreFilePattern.createPattern("/a/b/");
        assertThat(res1, instanceOf(IgnoreFilePattern.ExactMatchPart.class));
        assertThat(res1.match("a"), isMatch().requiresMore().hasNext());
        assertThat(res1.match("b"), notMatch());

        IgnoreFilePattern.PathPart res2 = res1.match("a").next;
        assertThat(res2, instanceOf(IgnoreFilePattern.ExactMatchPart.class));
        assertThat(res2.match("a"), notMatch());
        assertThat(res2.match("b"), isMatch().requiresMore().hasNext());

        IgnoreFilePattern.PathPart res3 = res2.match("b").next;
        assertThat(res3, instanceOf(IgnoreFilePattern.StrictStarStarPart.class));
        assertThat(res3.match("a"), isMatch().isLast().hasNext());
        assertThat(res3.match("b"), isMatch().isLast().hasNext());
    }

    @Test
    public void testParseLine_DoublePath() {
        IgnoreFilePattern pattern = IgnoreFilePattern.parseLinePattern("/a/b/");
        assertNotNull(pattern);
        assertThat(
                pattern.matches(Arrays.asList("a", "b", "c")),
                is(true)
        );
    }


    private static PatternMatch notMatch() {
        return new PatternMatch();
    }


    private static PatternMatch isMatch() {
        return new PatternMatch().isMatch();
    }


    static class PatternMatch extends BaseMatcher<IgnoreFilePattern.PathNameMatchResult> {
        private boolean isMatch = false;
        private boolean requiresMore = false;
        private boolean isLast = false;
        private boolean hasNext = false;

        PatternMatch isMatch() {
            isMatch = true;
            return this;
        }
        PatternMatch requiresMore() {
            requiresMore = true;
            return this;
        }
        PatternMatch isLast() {
            isLast = true;
            return this;
        }
        PatternMatch hasNext() {
            hasNext = true;
            return this;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof IgnoreFilePattern.PathNameMatchResult)) {
                return false;
            }
            IgnoreFilePattern.PathNameMatchResult res = (IgnoreFilePattern.PathNameMatchResult) o;
            return res.isMatch == isMatch &&
                    res.requiresMore == requiresMore &&
                    res.isLastElementMatch == isLast &&
                    (hasNext ? res.next != null : res.next == null);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("isMatch? ").appendValue(isMatch)
                    .appendText(" requiresMore? ").appendValue(requiresMore)
                    .appendText(" isLast? ").appendValue(isLast)
                    .appendText(" hasNext? ").appendValue(hasNext);
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            description.appendText("was ");
            if (!(item instanceof IgnoreFilePattern.PathNameMatchResult)) {
                description.appendValue(item);
            } else {
                IgnoreFilePattern.PathNameMatchResult res = (IgnoreFilePattern.PathNameMatchResult) item;
                description.appendText("isMatch? ").appendValue(res.isMatch)
                        .appendText(" requiresMore? ").appendValue(res.requiresMore)
                        .appendText(" isLast? ").appendValue(res.isLastElementMatch)
                        .appendText(" next ").appendValue(res.next);
            }
        }
    }
}