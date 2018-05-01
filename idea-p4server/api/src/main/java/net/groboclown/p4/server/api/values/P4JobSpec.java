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
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the information in the server's job spec.
 */
public interface P4JobSpec {
    String getComment();

    @NotNull
    List<P4JobField> getFields();

    /**
     *
     * @param job the job to map its fields
     * @return the mapping of the internal {@link #getFields()} to the job's {@link P4Job#getRawDetails()} values.
     *      If the job does not have a field, then either the preset or null is its value.
     */
    @NotNull
    Map<P4JobField, Object> mapJobFields(@NotNull P4Job job);

    /**
     *
     * @param job job details
     * @return the correctly mapped job ID for the job object, based on its raw details.
     */
    @Nullable
    String getJobId(@NotNull P4Job job);

    @Nullable
    String getJobStatus(@NotNull P4Job job);

    @Nullable
    String getUser(@NotNull P4Job job);

    @Nullable
    Date getDateCreated(@NotNull P4Job job);

    @Nullable
    String getDescription(@NotNull P4Job job);
}
