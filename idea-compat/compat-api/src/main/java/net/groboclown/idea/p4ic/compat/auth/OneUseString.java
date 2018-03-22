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

package net.groboclown.idea.p4ic.compat.auth;

import org.jetbrains.annotations.Nullable;

public class OneUseString {
    private final boolean nullValue;
    private char[] value;
    private boolean used = false;

    public OneUseString() {
        value = null;
        nullValue = true;
    }

    /**
     * Directly stores the char array.  It will be blanked out
     * as soon as the {@link #use(WithString)} method is called.
     *
     * @param value will directly be stored.
     */
    public OneUseString(char[] value) {
        this.value = value;
        nullValue = value == null;
    }

    public OneUseString(String value) {
        this.value = value.toCharArray();
        nullValue = false;
    }

    public synchronized <T> T use(WithString<T> block) {
        checkAvailable();
        try {
            return block.with(value);
        } finally {
            if (value != null) {
                for (int i = 0; i < value.length; i++) {
                    value[i] = 0;
                }
            }
            value = null;
        }
    }

    public synchronized <T, H extends Throwable> T use(WithStringThrows<T, H> block) throws H {
        checkAvailable();
        try {
            return block.with(value);
        } finally {
            if (value != null) {
                for (int i = 0; i < value.length; i++) {
                    value[i] = 0;
                }
            }
            value = null;
        }
    }

    public boolean isNullValue() {
        return nullValue;
    }

    public interface WithString<T> {
        T with(@Nullable char[] value);
    }

    public interface WithStringThrows<T, H extends Throwable> {
        T with(@Nullable char[] value) throws H;
    }

    private void checkAvailable() {
        if (used) {
            throw new IllegalStateException("Already used string");
        }
        used = true;
    }
}
