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

package net.groboclown.p4.server.api.commands.file;

import com.intellij.openapi.vcs.FilePath;
import com.perforce.p4java.exception.P4JavaException;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class GetFileContentsQuery implements P4CommandRunner.ClientQuery<GetFileContentsResult> {
    private final String depotPath;
    private final FilePath localPath;
    private final int rev;

    public GetFileContentsQuery(@NotNull String depotPath) {
        this.depotPath = depotPath;
        this.localPath = null;
        this.rev = -1;
    }

    public GetFileContentsQuery(@NotNull FilePath localPath, int rev) {
        this.depotPath = null;
        this.localPath = localPath;
        this.rev = rev;
    }

    @Override
    public String toString() {
        if (depotPath != null) {
            return "GetFileContents(" + depotPath + ")";
        } else {
            return "GetFileContents(" + localPath + "#" + rev + ")";
        }
    }

    @NotNull
    @Override
    public Class<? extends GetFileContentsResult> getResultType() {
        return GetFileContentsResult.class;
    }

    @Override
    public P4CommandRunner.ClientQueryCmd getCmd() {
        return P4CommandRunner.ClientQueryCmd.GET_FILE_CONTENTS;
    }

    public <R> R when(
            ClientConfig config,
            DepotFunction<R> depotFile,
            LocalFunction<R> localFile)
            throws P4JavaException, IOException {
        if (depotPath != null) {
            return depotFile.apply(depotPath);
        }
        return localFile.apply(config.getClientname(), localPath, rev);
    }


    public interface DepotFunction<R> {
        R apply(String s) throws P4JavaException, IOException;
    }

    public interface LocalFunction<R> {
        R apply(String clientname, FilePath local, int rev) throws P4JavaException, IOException;
    }
}
