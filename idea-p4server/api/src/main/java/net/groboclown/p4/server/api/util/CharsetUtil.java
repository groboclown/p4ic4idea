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

package net.groboclown.p4.server.api.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import net.groboclown.p4.server.api.config.ClientConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Utility for using the best-case charset name, for use when adding, editing, or moving
 * a file.
 *
 * Based on inspection of {@link com.perforce.p4java.impl.mapbased.rpc.func.client.ClientSendFile}
 * and {@link com.perforce.p4java.impl.mapbased.rpc.sys.RpcInputStream}, this should in most cases
 * return {@literal null}.  Only when the user explicitly wants to use the IDE's encoding or
 * client encoding should those be used instead.
 */
public class CharsetUtil {
    public enum CharsetPreference {
        SERVER(1),
        IDE(2),
        CLIENT_CONFIG(3);

        private final int index;
        CharsetPreference(int index) {
            this.index = index;
        }

        public int getValue() {
            return this.index;
        }

        public static CharsetPreference fromValue(int value) {
            switch (value) {
                case 2:
                    return IDE;
                case 3:
                    return CLIENT_CONFIG;
                default:
                    return SERVER;
            }
        }
    }


    /**
     * Finds the most applicable charset for a file add, edit, or move.
     */
    @Nullable
    public static String getBestCharSet(@Nullable Project project, @Nullable FilePath filePath,
            @NotNull ClientConfig config, @NotNull CharsetPreference preference) {
        if (preference == CharsetPreference.IDE && filePath != null) {
            return filePath.getCharset(project).name();
        }
        if (preference == CharsetPreference.CLIENT_CONFIG) {
            return config.getCharSetName();
        }

        // In all other cases, use the server's defined charset.
        return null;
    }
}
