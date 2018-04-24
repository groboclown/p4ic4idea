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

package net.groboclown.p4.server.api.config;

import net.groboclown.p4.server.api.config.part.ConfigPart;
import net.groboclown.p4.server.api.util.EqualUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Describes a problem discovered in the configuration.  This might also be a problem from
 * a server error, such as an SSL issue, or a client workspace not existing, or a user
 * is incorrect.  This is intended to be consumed by an end-user through a GUI.
 */
@Immutable
public class ConfigProblem implements Comparable<ConfigProblem> {
    private final ConfigPart source;
    private final String message;
    private final boolean error;

    public ConfigProblem(@Nullable ConfigPart source,
            @NotNull
            @Nls(capitalization = Nls.Capitalization.Sentence)
            String message,
            boolean error) {
        this.source = source;
        this.message = message;
        this.error = error;
    }

    /**
     * The component that contains the error.  It should only be null if no
     * specific component is the source of the problem.
     *
     * @return the source of the issue.
     */
    @Nullable
    public ConfigPart getSource() {
        return source;
    }

    /**
     * User-readable message describing
     *
     * @return the user-readable descriptive message
     */
    @NotNull
    @Nls(capitalization = Nls.Capitalization.Sentence)
    public String getMessage() {
        return message;
    }

    /**
     *
     * @return true if an error, false if a warning.
     */
    public boolean isError() {
        return error;
    }

    @Override
    public String toString() {
        return "problem(" + getSource() + ": " + getMessage() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigProblem)) {
            return false;
        }
        ConfigProblem that = (ConfigProblem) o;
        return isError() == that.isError() &&
                // Not perfect: the source can change, which changes the
                // equality.  But this will do.  As long as the source object
                // has correct equality / hashCode semantics, and it isn't used
                // as the key for a hash table when the source changes, this
                // will work.
                EqualUtil.isEqual(getSource(), that.getSource()) &&
                getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        int code = getMessage().hashCode() + (isError() ? 100 : 101);
        if (getSource() != null) {
            code += getSource().hashCode();
        }
        return code;
    }

    /**
     * Allow for a natural grouping of the config problems, so that more important
     * items are first.
     *
     * @param that
     * @return
     */
    @Override
    public int compareTo(@Nonnull ConfigProblem that) {
        // Error state is most important
        if (that.isError() && !isError()) {
            return 1;
        }
        if (isError() && !that.isError()) {
            return -1;
        }

        // Source of the problem is next important.
        if (getSource() == null) {
            if (that.getSource() != null) {
                return -1;
            }
        } else if (that.getSource() == null) {
            return 1;
        } else {
            int cmp = getSource().getSourceName().compareTo(that.getSource().getSourceName());
            if (cmp != 0) {
                return cmp;
            }
        }

        // Finally, the message text.
        return getMessage().compareTo(that.getMessage());
    }
}
