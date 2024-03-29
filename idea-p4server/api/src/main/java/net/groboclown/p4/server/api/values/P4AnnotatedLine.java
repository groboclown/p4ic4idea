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

package net.groboclown.p4.server.api.values;

import com.perforce.p4java.core.file.IFileRevisionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public interface P4AnnotatedLine {
    @Nullable
    P4ChangelistId getChangelist();

    @Nullable
    String getAuthor();

    @Nullable
    Date getDate();

    @Nullable
    String getComment();

    @NotNull
    IFileRevisionData getRevisionData();

    @NotNull
    P4RemoteFile getDepotPath();

    int getLineNumber();

    @NotNull
    P4FileRevision getRev();

    int getRevNumber();
}
