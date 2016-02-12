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
package net.groboclown.idea.p4ic.v2.server.cache.state;

import com.intellij.openapi.vcs.FilePath;
import net.groboclown.idea.p4ic.mock.MockFilePath;
import net.groboclown.idea.p4ic.v2.server.cache.FileUpdateAction;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class P4FileUpdateStateTest {

    @Test
    public void testEquals() throws Exception {
        final FileMappingRepo repo = new FileMappingRepo(false);
        FilePath f1 = createFilePath("a1");
        P4FileUpdateState fus1 = new P4FileUpdateState(
                repo.getByLocation(f1), 1, FileUpdateAction.DELETE_FILE, true);
        P4FileUpdateState fus2 = new P4FileUpdateState(
                repo.getByLocation(f1), 2, FileUpdateAction.DELETE_FILE, true);
        assertThat(fus1, is(fus2));
    }

    @Test
    public void testHashCode() throws Exception {
        P4FileUpdateState s1 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                1, FileUpdateAction.ADD_EDIT_FILE, true);
        P4FileUpdateState s2 = new P4FileUpdateState(
                new P4ClientFileMapping(null, createFilePath("a.txt")),
                2, FileUpdateAction.DELETE_FILE, true);
        assertThat(s1.hashCode(), is(s2.hashCode()));
    }

    private FilePath createFilePath(String f) {
        return createFilePath(new File(f));
    }
    private FilePath createFilePath(File f) {
        return new MockFilePath(f);
    }
}
