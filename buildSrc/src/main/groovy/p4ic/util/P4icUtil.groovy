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

package p4ic.util

import org.gradle.api.GradleException
import org.gradle.api.Project

import javax.annotation.Nonnull
import javax.annotation.Nullable

class P4icUtil {
    @Nonnull
    static File libDir(@Nonnull Project project, String... sub) {
        File ret = project.file(project.rootDir)
        ret = new File(ret, "lib")
        for (String s: sub) {
            ret = new File(ret, s)
        }
        return ret
    }

    @Nonnull
    static File checkIsDir(@Nullable File f) {
        if (f == null || !f.exists() || !f.isDirectory()) {
            throw new GradleException("`" + f + "` is not a directory")
        }
        return f
    }
}
