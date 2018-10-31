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

package net.groboclown.p4.server.api.commands.changelist;

import com.perforce.p4java.core.file.IFileSpec;
import net.groboclown.p4.server.api.P4CommandRunner;
import net.groboclown.p4.server.api.repository.P4RepositoryLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class ListSubmittedChangelistsQuery implements P4CommandRunner.ClientQuery<ListSubmittedChangelistsResult> {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("YYYY/MM/dd:HH:mm:ss");
    private final P4RepositoryLocation location;
    private final int maxCount;
    private final boolean onlyChangesWithShelvedFilesFilter;
    private final String username;
    private final String clientName;
    private final String specFilter;

    public static class Filter {
        private boolean onlyChangesWithShelvedFilesFilter;
        private Long changesAfterChangelistFilter;
        private Long changesBeforeChangelistFilter;
        private Date changesAfterDateFilter;
        private Date changesBeforeDateFilter;
        private String username;
        private String clientName;

        public boolean isOnlyChangesWithShelvedFilesFilter() {
            return onlyChangesWithShelvedFilesFilter;
        }

        public void setOnlyChangesWithShelvedFilesFilter(boolean onlyChangesWithShelvedFilesFilter) {
            this.onlyChangesWithShelvedFilesFilter = onlyChangesWithShelvedFilesFilter;
        }

        public Long getChangesAfterChangelistFilter() {
            return changesAfterChangelistFilter;
        }

        public void setChangesAfterChangelistFilter(Long changesAfterChangelistFilter) {
            this.changesAfterChangelistFilter = changesAfterChangelistFilter;
        }

        public Long getChangesBeforeChangelistFilter() {
            return changesBeforeChangelistFilter;
        }

        public void setChangesBeforeChangelistFilter(Long changesBeforeChangelistFilter) {
            this.changesBeforeChangelistFilter = changesBeforeChangelistFilter;
        }

        public Date getChangesAfterDateFilter() {
            return changesAfterDateFilter;
        }

        public void setChangesAfterDateFilter(Date changesAfterDateFilter) {
            this.changesAfterDateFilter = changesAfterDateFilter;
        }

        public Date getChangesBeforeDateFilter() {
            return changesBeforeDateFilter;
        }

        public void setChangesBeforeDateFilter(Date changesBeforeDateFilter) {
            this.changesBeforeDateFilter = changesBeforeDateFilter;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }
    }


    public ListSubmittedChangelistsQuery(@NotNull P4RepositoryLocation location, @Nullable Filter filter,
            int maxCount) {
        this.location = location;
        this.maxCount = maxCount;
        if (filter == null) {
            this.onlyChangesWithShelvedFilesFilter = false;
            this.username = null;
            this.clientName = null;
            this.specFilter = null;
        } else {
            this.onlyChangesWithShelvedFilesFilter = filter.isOnlyChangesWithShelvedFilesFilter();
            this.username = filter.username;
            this.clientName = filter.clientName;

            // TODO disconnect this very specific Perforce logic into a more Perforce-centric location
            final StringBuilder changelistRange = new StringBuilder();
            boolean first = appendIfNotNull('>', filter.getChangesAfterChangelistFilter(),
                    Object::toString,
                    changelistRange, true);
            first = appendIfNotNull('<', filter.getChangesBeforeChangelistFilter(),
                    Object::toString,
                    changelistRange, first);
            first = appendIfNotNull('>', filter.getChangesAfterDateFilter(),
                    (o) -> DATE_FORMAT.format((Date) o),
                    changelistRange, first);
            appendIfNotNull('<', filter.getChangesBeforeDateFilter(),
                    (o) -> DATE_FORMAT.format((Date) o),
                    changelistRange, first);

            if (changelistRange.length() <= 0) {
                this.specFilter = null;
            } else {
                this.specFilter = changelistRange.toString();
            }

        }
    }

    private static boolean appendIfNotNull(char prefix, Object value,
            Function<Object, String> convert, StringBuilder buff, boolean first) {
        if (value != null) {
            if (first) {
                first = false;
                buff.append('@');
            } else {
                buff.append(',');
            }
            buff.append(prefix).append(convert.apply(value));
        }
        return first;
    }

    @NotNull
    @Override
    public Class<? extends ListSubmittedChangelistsResult> getResultType() {
        return ListSubmittedChangelistsResult.class;
    }

    @Override
    public P4CommandRunner.ClientQueryCmd getCmd() {
        return P4CommandRunner.ClientQueryCmd.LIST_SUBMITTED_CHANGELISTS;
    }

    @NotNull
    public P4RepositoryLocation getLocation() {
        return location;
    }

    @Nullable
    public String getSpecFilter() {
        return specFilter;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public boolean isOnlyChangesWithShelvedFilesFilter() {
        return onlyChangesWithShelvedFilesFilter;
    }

    @Nullable
    public String getUsernameFilter() {
        return username;
    }

    @Nullable
    public String getClientNameFilter() {
        return clientName;
    }
}
