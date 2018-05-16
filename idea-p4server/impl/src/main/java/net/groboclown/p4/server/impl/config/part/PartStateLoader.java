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

package net.groboclown.p4.server.impl.config.part;

import com.intellij.openapi.vfs.VirtualFile;
import net.groboclown.p4.server.api.config.part.ConfigPart;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class PartStateLoader {
    public static ConfigPart load(@NotNull Class<?> partClass,
            @NotNull VirtualFile root, @NotNull String sourceName, @NotNull Map<String, String> values) {
        if (! ConfigStateProvider.class.isAssignableFrom(partClass) ||
                ! ConfigPart.class.isAssignableFrom(partClass)) {
            throw new IllegalArgumentException(partClass + " is not a ConfigStateProvider");
        }
        try {
            Constructor<?> ctor = partClass.getConstructor(String.class, VirtualFile.class, Map.class);
            return (ConfigPart) ctor.newInstance(sourceName, root, values);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }
}
