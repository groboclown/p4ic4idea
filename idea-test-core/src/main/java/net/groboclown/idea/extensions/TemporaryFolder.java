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

package net.groboclown.idea.extensions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

/**
 * Simple temporary folder for test cases to write files into.
 */
public class TemporaryFolder {
    private Path rootFolder;

    /**
     * Creates a new file object under the temporary folder.  It isn't
     * created first, so the user can turn it into a directory or file.
     *
     * @param name name of the element in the temporary folder.
     * @return the new file object
     */
    public File newFile(String name) {
        return new File(rootFolder.toFile(), name);
    }

    void prepare() throws IOException {
        rootFolder = Files.createTempDirectory("test");
    }

    void cleanUp() {
        try {
            Files.walkFileTree( rootFolder, new DeleteAllVisitor() );
        } catch( IOException ioe ) {
            throw new RuntimeException( ioe );
        }
    }

    private static class DeleteAllVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile( Path file, BasicFileAttributes attributes ) throws IOException {
            Files.delete( file );
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory( Path directory, IOException exception ) throws IOException {
            Files.delete( directory );
            return CONTINUE;
        }
    }
}
