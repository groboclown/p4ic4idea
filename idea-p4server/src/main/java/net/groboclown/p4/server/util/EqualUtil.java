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

package net.groboclown.p4.server.util;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;

public class EqualUtil {
    public static boolean isEqual(@Nullable Object a, @Nullable Object b) {
        // Short circuit a.equals(b) if a == b.  This also keeps us from
        // needing a conditional branch with the ?: operator.
        // if a == b, then it's false if a != null and b == null, or b != null and a == null.
        // So, if a == null and a != b, then b must be != null, so that part evaluates to false.
        return (a == b || (a != null && a.equals(b)));
    }


    public static boolean isArrayEqual(@Nullable Object[] a, @Nullable Object[] b) {
        return Arrays.deepEquals(a, b);
    }


    public static boolean isSameFile(@Nullable File a, @Nullable File b) {
        return FileUtil.filesEqual(a, b);
    }
}
