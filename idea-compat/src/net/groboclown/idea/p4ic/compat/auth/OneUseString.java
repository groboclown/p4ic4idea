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

public class OneUseString {
    private char[] value;

    /**
     * Directly stores the char array.  It will be blanked out
     * as soon as the {@link #use(WithString)} method is called.
     *
     * @param value will directly be stored.
     */
    public OneUseString(char[] value) {
        this.value = value;
    }

    public OneUseString(String value) {
        this.value = value.toCharArray();
    }

    public <T> void use(WithString<T> block) {
        try {
            block.with(value);
        } finally {
            for (int i = 0; i < value.length; i++) {
                value[i] = 0;
            }
            value = null;
        }
    }

    public <T, H extends Throwable> void use(WithStringThrows<T, H> block) throws H {
        try {
            block.with(value);
        } finally {
            for (int i = 0; i < value.length; i++) {
                value[i] = 0;
            }
            value = null;
        }
    }

    public interface WithString<T> {
        T with(char[] value);
    }

    public interface WithStringThrows<T, H extends Throwable> {
        T with(char[] value) throws H;
    }
}
