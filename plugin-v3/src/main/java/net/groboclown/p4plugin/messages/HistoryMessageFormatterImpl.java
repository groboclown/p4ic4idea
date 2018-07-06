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

package net.groboclown.p4plugin.messages;

import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IRevisionIntegrationData;
import net.groboclown.p4.server.impl.repository.HistoryMessageFormatter;
import net.groboclown.p4plugin.P4Bundle;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HistoryMessageFormatterImpl implements HistoryMessageFormatter {
    @NotNull
    @Override
    public String format(@NotNull IFileRevisionData data) {
        StringBuilder comment = new StringBuilder();
        if (data.getDescription() != null) {
            comment.append(P4Bundle.message("file-revision.comment", data.getDescription().trim()));
        }
        comment.append(P4Bundle.message("file-revision.location",
                data.getDepotFileName(),
                data.getChangelistId()));
        final List<IRevisionIntegrationData> integrations =
                data.getRevisionIntegrationDataList();
        if (integrations != null && ! integrations.isEmpty()) {
            comment.append(P4Bundle.message("file-revision.integrations.header"));
            for (IRevisionIntegrationData integration : integrations) {
                if (integration.getStartFromRev() <= 0) {
                    comment.append(P4Bundle.message("file-revision.integrations.item-no_start",
                            integration.getFromFile(),
                            integration.getEndFromRev(),
                            integration.getHowFrom()));
                } else {
                    comment.append(P4Bundle.message("file-revision.integrations.item-start",
                            integration.getFromFile(),
                            integration.getStartFromRev(),
                            integration.getEndFromRev(),
                            integration.getHowFrom()));
                }
            }
        }
        return comment.toString();
    }
}
