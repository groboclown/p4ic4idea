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
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FileMappingRepoTest {
    @Test
    public void testGetAllFilesInsensitive() throws Exception {
        final FileMappingRepo repo = new FileMappingRepo(true);

        final File f1 = new File("f1");
        final FilePath fp1 = createFilePath(f1);
        repo.getByDepotLocation("//depot/file1", fp1);

        final File f2 = new File("f2");
        final FilePath fp2 = createFilePath(f2);
        repo.getByDepotLocation("//depot/file2", fp2);

        final File f3 = new File("f3");
        final FilePath fp3 = createFilePath(f3);
        // case insensitive
        repo.getByDepotLocation("//depot/FILE1", fp3);

        final Iterable<P4ClientFileMapping> iterable = repo.getAllFiles();
        // ensure the iterable can be used multiple times
        for (int i = 0; i < 3; i++) {
            List<String> expectedDepotPaths = new ArrayList<String>(Arrays.asList("//depot/file1", "//depot/file2"));
            List<File> expectedFiles = new ArrayList<File>(Arrays.asList(fp2.getIOFile(), fp3.getIOFile()));
            Map<String, File> pairs = new HashMap<String, File>();
            pairs.put("//depot/file1", fp3.getIOFile());
            pairs.put("//depot/file2", fp2.getIOFile());

            final Iterator<P4ClientFileMapping> iter = iterable.iterator();
            assertThat(iter.hasNext(), is(true));

            P4ClientFileMapping next = iter.next();
            assertThat(expectedDepotPaths.remove(next.getDepotPath()), is(true));
            assertThat(expectedFiles.remove(next.getLocalFilePath().getIOFile()), is(true));
            assertThat(next.getLocalFilePath().getIOFile(), is(pairs.get(next.getDepotPath())));

            next = iter.next();
            assertThat(expectedDepotPaths.remove(next.getDepotPath()), is(true));
            assertThat(expectedFiles.remove(next.getLocalFilePath().getIOFile()), is(true));
            assertThat(next.getLocalFilePath().getIOFile(), is(pairs.get(next.getDepotPath())));

            assertThat(iter.hasNext(), is(false));
        }
    }

    @Test
    public void testGetAllFilesSensitive() throws Exception {
        final FileMappingRepo repo = new FileMappingRepo(false);

        final File f1 = new File("f1");
        final FilePath fp1 = createFilePath(f1);
        repo.getByDepotLocation("//depot/file1", fp1);

        final File f2 = new File("f2");
        final FilePath fp2 = createFilePath(f2);
        repo.getByDepotLocation("//depot/file2", fp2);

        final File f3 = new File("f3");
        final FilePath fp3 = createFilePath(f3);
        repo.getByDepotLocation("//depot/FILE1", fp3);

        final Iterable<P4ClientFileMapping> iterable = repo.getAllFiles();
        // ensure the iterable can be used multiple times
        for (int i = 0; i < 3; i++) {
            List<String> expectedDepotPaths = new ArrayList<String>(Arrays.asList("//depot/file1", "//depot/file2", "//depot/FILE1"));
            List<File> expectedFiles = new ArrayList<File>(Arrays.asList(fp1.getIOFile(), fp2.getIOFile(), fp3.getIOFile()));
            Map<String, File> pairs = new HashMap<String, File>();
            pairs.put("//depot/file1", fp1.getIOFile());
            pairs.put("//depot/file2", fp2.getIOFile());
            pairs.put("//depot/FILE1", fp3.getIOFile());

            final Iterator<P4ClientFileMapping> iter = iterable.iterator();

            assertThat(iter.hasNext(), is(true));
            P4ClientFileMapping next = iter.next();
            assertThat(expectedDepotPaths.remove(next.getDepotPath()), is(true));
            assertThat(expectedFiles.remove(next.getLocalFilePath().getIOFile()), is(true));
            assertThat(next.getLocalFilePath().getIOFile(), is(pairs.get(next.getDepotPath())));

            assertThat(iter.hasNext(), is(true));
            next = iter.next();
            assertThat(expectedDepotPaths.remove(next.getDepotPath()), is(true));
            assertThat(expectedFiles.remove(next.getLocalFilePath().getIOFile()), is(true));
            assertThat(next.getLocalFilePath().getIOFile(), is(pairs.get(next.getDepotPath())));

            assertThat(iter.hasNext(), is(true));
            next = iter.next();
            assertThat(expectedDepotPaths.remove(next.getDepotPath()), is(true));
            assertThat(expectedFiles.remove(next.getLocalFilePath().getIOFile()), is(true));
            assertThat(next.getLocalFilePath().getIOFile(), is(pairs.get(next.getDepotPath())));

            assertThat(iter.hasNext(), is(false));
        }
    }

    @Test
    public void testGetByDepotLocation() throws Exception {
        final FileMappingRepo repo = new FileMappingRepo(false);

        final File f1 = new File("f1");
        final FilePath fp1 = createFilePath(f1);
        final P4ClientFileMapping expected = repo.getByDepotLocation("//depot/file1", fp1);

        final P4ClientFileMapping actual = repo.getByDepotLocation("//depot/file1", null);

        assertThat(actual, is(expected));
    }

    @Test
    public void testGetByLocalFilePath() throws Exception {
        final FileMappingRepo repo = new FileMappingRepo(false);

        final File f1 = new File("f1");
        final FilePath fp1 = createFilePath(f1);
        final P4ClientFileMapping expected = repo.getByDepotLocation("//depot/file1", fp1);

        final P4ClientFileMapping actual = repo.getByLocation(fp1);

        assertThat(actual, is(expected));
    }


    private FilePath createFilePath(File f) {
        return new MockFilePath(f);
    }
}
