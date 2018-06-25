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

package net.groboclown.p4.server.impl.connection.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.core.file.IFileAnnotation;
import net.groboclown.p4.server.api.values.P4FileAnnotation;
import net.groboclown.p4.server.api.values.P4RemoteFile;

import java.util.List;

public class FileAnnotationParser {
    private static final Logger LOG = Logger.getInstance(FileAnnotationParser.class);


    public static P4FileAnnotation getFileAnnotation(List<IFileAnnotation> annotations) {
        // FIXME implement file annotation parser
        throw new IllegalStateException("Not implemented");
    }
}
