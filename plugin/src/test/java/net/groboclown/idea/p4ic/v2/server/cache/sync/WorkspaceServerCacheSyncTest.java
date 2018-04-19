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

package net.groboclown.idea.p4ic.v2.server.cache.sync;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class WorkspaceServerCacheSyncTest {
    @Test
    public void testSimpleMappingCheck1() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client1/a/b/...");
        assertThat(
                matcher,
                notNullValue());
        assertThat(
                matcher.getPath(),
                is(new File("a/b").getPath()));
    }

    @Test
    public void testSimpleMappingCheck2() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client2/a/b/....java");
        assertThat(
                matcher,
                notNullValue());
        assertThat(
                matcher.getPath(),
                is(new File("a/b").getPath()));
    }

    @Test
    public void testSimpleMappingCheck3() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client3/a/b/.../x*.java");
        assertThat(
                matcher,
                notNullValue());
        assertThat(
                matcher.getPath(),
                is(new File("a/b").getPath()));
    }

    @Test
    public void testSimpleMappingCheck4() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client4/a/.../b/...");
        assertThat(
                matcher,
                notNullValue());
        assertThat(
                matcher.getPath(),
                is(new File("a").getPath()));
    }

    @Test
    public void testSimpleMappingCheck5() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client5/...");
        assertThat(
                matcher,
                nullValue());
    }

    @Test
    public void testSimpleMappingCheck6() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client5/a/*/...");
        assertThat(
                matcher,
                nullValue());
    }

    @Test
    public void testSimpleMappingCheck7() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client5/a/b.../...");
        assertThat(
                matcher,
                nullValue());
    }

    @Test
    public void testSimpleMappingCheck8() {
        final File matcher = WorkspaceServerCacheSync.getSimpleMatchDirectory("//client5/a/b...");
        assertThat(
                matcher,
                nullValue());
    }
}
