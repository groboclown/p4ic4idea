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

package net.groboclown.p4.server.impl.connection.impl;

import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServerMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenFileStatus {
    private final Set<IExtendedFileSpec> add = new HashSet<>();
    private final Set<IExtendedFileSpec> delete = new HashSet<>();
    private final Set<IExtendedFileSpec> edit = new HashSet<>();
    private final Set<IExtendedFileSpec> filesWithMessage = new HashSet<>();
    private final List<IServerMessage> messages = new ArrayList<>();
    private final List<IExtendedFileSpec> skipped = new ArrayList<>();

    // Note: everything in move is in one of the other collections.
    private final Map<IExtendedFileSpec, IExtendedFileSpec> moveMap = new HashMap<>();

    public OpenFileStatus(Collection<IExtendedFileSpec> status) {
        final Map<String, IExtendedFileSpec> integrateMap = new HashMap<>();

        for (IExtendedFileSpec spec : status) {
            if (spec == null) {
                continue;
            }
            if (spec.getStatusMessage() != null) {
                if (spec.getDepotPath() != null || spec.getClientPath() != null
                        || spec.getOriginalPathString() != null) {
                    filesWithMessage.add(spec);
                }
                messages.add(spec.getStatusMessage());
            } else if (spec.getAction() == null) {
                skipped.add(spec);
            } else {
                switch (spec.getAction()) {
                    case ADD:
                    case ADDED:
                    case ADD_EDIT:
                        add.add(spec);
                        break;

                    case EDIT:
                        edit.add(spec);
                        break;

                    case DELETE:
                    case DELETED:
                        delete.add(spec);
                        break;

                    case BRANCH:
                        add.add(spec);
                        if (spec.getFromFile() != null) {
                            integrateMap.put(spec.getFromFile(), spec);
                        }
                        break;

                    case INTEGRATE:
                        edit.add(spec);
                        if (spec.getFromFile() != null) {
                            integrateMap.put(spec.getFromFile(), spec);
                        }
                        break;

                    default:
                        skipped.add(spec);
                        break;
                }
            }
        }

        for (IExtendedFileSpec spec : delete) {
            if (integrateMap.containsKey(spec.getDepotPath().getPathString())) {
                moveMap.put(spec, integrateMap.get(spec.getDepotPath().getPathString()));
            }
        }
    }

    public Set<IExtendedFileSpec> getAdd() {
        return add;
    }

    public boolean hasAdd() {
        return !add.isEmpty();
    }

    public Set<IExtendedFileSpec> getDelete() {
        return delete;
    }

    public boolean hasDelete() {
        return !delete.isEmpty();
    }

    public Set<IExtendedFileSpec> getEdit() {
        return edit;
    }

    public boolean hasEdit() {
        return !edit.isEmpty();
    }

    public boolean hasAddEdit() {
        return !edit.isEmpty() || !add.isEmpty();
    }

    public int getOpenedCount() {
        return add.size() + delete.size() + edit.size();
    }

    public boolean hasOpen() {
        return getOpenedCount() > 0;
    }

    public List<IExtendedFileSpec> getOpen() {
        List<IExtendedFileSpec> ret = new ArrayList<>();
        ret.addAll(add);
        ret.addAll(delete);
        ret.addAll(edit);
        return ret;
    }

    public List<IServerMessage> getMessages() {
        return messages;
    }

    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    /**
     *
     * @return files not categorized because they had an info/warning/error associated with them.
     */
    public Set<IExtendedFileSpec> getFilesWithMessages() {
        return filesWithMessage;
    }

    public List<IExtendedFileSpec> getSkipped() {
        return skipped;
    }

    public boolean hasSkipped() {
        return !skipped.isEmpty();
    }

    /**
     * If the messages contain an error, then throw an exception.
     */
    public void throwIfError()
            throws RequestException {
        for (IServerMessage msg : messages) {
            if (msg != null && msg.isError()) {
                throw new RequestException(msg);
            }
        }
    }

    /**
     *
     * @return mapping of from (deleted) spec to new location (added/edited).
     */
    public Map<IExtendedFileSpec, IExtendedFileSpec> getMoveMap() {
        return moveMap;
    }

    @Override
    public String toString() {
        return "Status{ add: " + add + "; edit: " + edit + "; delete: " + delete +
                "; skipped: " + skipped + "; messages: " + messages + " }";
    }
}
