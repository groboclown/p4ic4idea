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

package net.groboclown.p4plugin.revision;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import net.groboclown.p4.server.api.commands.HistoryContentLoader;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

public class ContentRevisionUtil {
    @NotNull
    public static Charset getNonNullCharset(@Nullable Charset charset) {
        return charset == null
                ? EncodingManager.getInstance().getDefaultCharset()
                : charset;
    }


    @Nullable
    public static String getContent(@NotNull ClientConfig clientConfig,
            @NotNull HistoryContentLoader loader, @NotNull FilePath file, int rev, @NotNull Charset charset)
            throws VcsException {
        try {
            byte[] ret = loader.loadContentForLocal(clientConfig, file, rev);
            if (ret == null) {
                return null;
            }
            // TODO Look at using CharsetUtil.extractCharsetFromFileContent
            // Or maybe use EncodingManager.getEncoding(file, true);
            return new String(ret, charset);
        } catch (IOException e) {
            throw new VcsException(e);
        }
    }
}
