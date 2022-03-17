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

package net.groboclown.idea.mock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility class to help out with reflective field setting.
 */
public class FieldHelper {
    public static void setStaticField(@NotNull Class<?> clazz, @NotNull String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (! Modifier.isStatic(field.getModifiers())) {
                throw new RuntimeException("Not static field " + clazz.getName() + '.' + fieldName);
            }
            assertTrue(field.trySetAccessible());
            removeFinalFrom(field);
            field.set(null, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    public static <T> void setTypedStaticField(@NotNull Class<?> clazz, @NotNull Class<? super T> fieldClassType,
            T value) {
        try {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType().equals(fieldClassType) &&
                        Modifier.isStatic(field.getModifiers())) {
                    assertTrue(field.trySetAccessible());
                    removeFinalFrom(field);
                    field.set(null, value);
                    return;
                }
            }
            throw new RuntimeException("No field typed as " + value.getClass());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    public static void setStaticField(@NotNull String className, @NotNull String fieldName, Object value) {
        setStaticField(loadClass(className), fieldName, value);
    }


    public static <T> void setTypedStaticField(@NotNull String className, @NotNull Class<? super T> fieldClassType,
            T value) {
        setTypedStaticField(loadClass(className), fieldClassType, value);
    }

    @NotNull
    public static Class<?> loadClass(@NotNull String className) {
        try {
            return FieldHelper.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public static void setInstanceField(@NotNull Object owner, @NotNull String fieldName, Object value) {
        try {
            Field field = getInstanceField(owner, fieldName);
            assertTrue(field.trySetAccessible());
            removeFinalFrom(field);
            field.set(owner, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    public static Field getInstanceField(@NotNull Object owner, @NotNull String fieldName)
            throws NoSuchFieldException {
        Class<?> search = owner.getClass();
        while (search != null && !search.equals(Object.class)) {
            Field field = search.getDeclaredField(fieldName);
            if (! Modifier.isStatic(field.getModifiers())) {
                return field;
            }
            search = search.getSuperclass();
        }
        throw new RuntimeException("Not instance field " + owner.getClass() + '#' + fieldName);
    }


    private static void removeFinalFrom(@NotNull Field field)
            throws NoSuchFieldException, IllegalAccessException {
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        assertTrue(modifiersField.trySetAccessible());
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
}
