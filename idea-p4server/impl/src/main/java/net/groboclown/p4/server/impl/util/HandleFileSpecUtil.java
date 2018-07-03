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

package net.groboclown.p4.server.impl.util;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.generic.core.file.PathAnnotations;
import org.jetbrains.annotations.NotNull;

public class HandleFileSpecUtil {
    @NotNull
    public static String getDepotDisplayName(IFileSpec spec) {
        return getDepotDisplayName(spec, true);
    }


    @NotNull
    public static String getDepotDisplayName(IFileSpec spec, boolean stripAnnotations) {
        if (stripAnnotations) {
            return unescapeP4Path(PathAnnotations.stripAnnotations(spec.getDepotPath().getPathString()));
        }
        return unescapeP4Path(spec.getDepotPath().getPathString());
    }


    @NotNull
    public static String getRawDepot(IFileSpec spec, boolean stripAnnotations) {
        if (stripAnnotations) {
            return PathAnnotations.stripAnnotations(spec.getDepotPath().getPathString());
        }
        return spec.getDepotPath().getPathString();
    }


    @NotNull
    private static String unescapeP4Path(@NotNull String path) {
        StringBuilder sb = new StringBuilder(path.length());
        char[] buff = path.toCharArray();
        int pos = 0;
        while (pos < buff.length) {
            char c = buff[pos++];
            if (c == '%' && pos + 2 < buff.length) {
                final char hex1 = buff[pos++];
                final int ihex1 = Character.digit(hex1, 16);
                final char hex2 = buff[pos++];
                final int ihex2 = Character.digit(hex2, 16);
                if (ihex1 < 0 || ihex2 < 0) {
                    pos -= 2;
                    sb.append(c);
                } else {
                    sb.append((char)((ihex1 << 4) + ihex2));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Strips off the revision or changelist suffix from the spec.
     * @param spec the source spec
     * @return the spec without revs or changelist.
     */
    @NotNull
    public static IFileSpec stripAnnotations(@NotNull IFileSpec spec) {
        if (spec.getDepotPathString() != null) {
            return new FileSpec(new com.perforce.p4java.impl.generic.core.file.FilePath(
                    FilePath.PathType.DEPOT,
                    spec.getDepotPath().getPathString(), false));
        }
        if (spec.getLocalPathString() != null) {
            return new FileSpec(new com.perforce.p4java.impl.generic.core.file.FilePath(
                    FilePath.PathType.LOCAL,
                    spec.getLocalPath().getPathString(), false));
        }
        if (spec.getClientPathString() != null) {
            return new FileSpec(new com.perforce.p4java.impl.generic.core.file.FilePath(
                    FilePath.PathType.CLIENT,
                    spec.getClientPath().getPathString(), false));
        }
        if (spec.getOriginalPathString() != null) {
            return new FileSpec(new com.perforce.p4java.impl.generic.core.file.FilePath(
                    FilePath.PathType.CLIENT,
                    spec.getOriginalPath().getPathString(), false));
        }
        throw new IllegalArgumentException("no path information in spec " + spec);
    }
}
