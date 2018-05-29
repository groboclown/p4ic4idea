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

package net.groboclown.p4.server.impl.values;

import com.perforce.p4java.core.IJobSpec;
import net.groboclown.p4.server.api.values.P4Job;
import net.groboclown.p4.server.api.values.P4JobField;
import net.groboclown.p4.server.api.values.P4JobSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P4JobSpecImpl implements P4JobSpec {
    private final String comments;
    private final List<P4JobField> fields;

    public P4JobSpecImpl(@NotNull IJobSpec src) {
        this.comments = src.getComments();
        List<P4JobField> f = new ArrayList<>(src.getFields().size());
        for (IJobSpec.IJobSpecField field : src.getFields()) {
            f.add(new P4JobFieldImpl(field, src));
        }
        fields = Collections.unmodifiableList(f);
    }

    public P4JobSpecImpl(@Nullable String comment, @NotNull List<P4JobField> fields) {
        this.comments = comment;
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    @Override
    public String getComment() {
        return comments;
    }

    @NotNull
    @Override
    public List<P4JobField> getFields() {
        return fields;
    }

    @NotNull
    @Override
    public Map<P4JobField, Object> mapJobFields(@NotNull P4Job job) {
        Map<P4JobField, Object> ret = new HashMap<>();
        Map<String, Object> details = job.getRawDetails();
        for (P4JobField field : fields) {
            ret.put(field, field.getDataType().convert(details.get(field.getName())));
        }
        return ret;
    }

    @Nullable
    @Override
    public String getJobId(@NotNull P4Job job) {
        for (P4JobField field : fields) {
            if (field.getCode() == P4JobField.JOB_ID_CODE) {
                Object r = field.getDataType().convert(job.getRawDetails().get(field.getName()));
                return r == null ? null : r.toString();
            }
        }
        return job.getJobId();
    }

    @Nullable
    @Override
    public String getJobStatus(@NotNull P4Job job) {
        for (P4JobField field : fields) {
            if (field.getCode() == P4JobField.JOB_STATUS_CODE) {
                Object r = field.getDataType().convert(job.getRawDetails().get(field.getName()));
                return r == null ? null : r.toString();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getUser(@NotNull P4Job job) {
        for (P4JobField field : fields) {
            if (field.getCode() == P4JobField.USER_CODE) {
                Object r = field.getDataType().convert(job.getRawDetails().get(field.getName()));
                return r == null ? null : r.toString();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Date getDateCreated(@NotNull P4Job job) {
        for (P4JobField field : fields) {
            if (field.getCode() == P4JobField.DATE_CREATED_CODE) {
                Object r = field.getDataType().convert(job.getRawDetails().get(field.getName()));
                return r == null
                        ? null
                        : (r instanceof Date ? (Date) r : null);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getDescription(@NotNull P4Job job) {
        for (P4JobField field : fields) {
            if (field.getCode() == P4JobField.DESCRIPTION_CODE) {
                Object r = field.getDataType().convert(job.getRawDetails().get(field.getName()));
                return r == null ? null : r.toString();
            }
        }
        return job.getDescription();
    }
}
