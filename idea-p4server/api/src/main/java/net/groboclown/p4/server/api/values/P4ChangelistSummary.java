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

package net.groboclown.p4.server.api.values;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;

/**
 * Reflects high-level information about a changelist.
 */
@Immutable
public interface P4ChangelistSummary {
    @NotNull
    P4ChangelistId getChangelistId();

    /**
     *
     * @return the comment for the changelist.  If the server query was limited, then this might be truncated.
     */
    @NotNull
    String getComment();

    boolean isDeleted();

    boolean isSubmitted();

    boolean isOnServer();

    /**
     * Are there shelved files in this changelist?  Can potentially return "false" if the command that
     * created this object didn't receive any information about it.
     *
     * @return true if the command that generated this object discovered that shelved files are associated
     *      with this changelist.
     */
    boolean hasShelvedFiles();

    /**
     *
     * @return the workspace ID of the assigned client.
     */
    @NotNull
    String getClientname();

    /**
     *
     * @return the user who owns the changelist.
     */
    @NotNull
    String getUsername();
}
