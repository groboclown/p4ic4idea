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

import com.perforce.p4java.core.file.IFileSpec;
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
    private final Set<IFileSpec> add = new HashSet<>();
    private final Set<IFileSpec> delete = new HashSet<>();
    private final Set<IFileSpec> edit = new HashSet<>();
    private final List<IFileSpec> messages = new ArrayList<>();
    private final List<IFileSpec> skipped = new ArrayList<>();

    // Note: everything in move is in one of the other collections.
    private final Map<IFileSpec, IFileSpec> moveMap = new HashMap<>();

    public OpenFileStatus(Collection<IFileSpec> status) {
        final Map<String, IFileSpec> integrateMap = new HashMap<>();

        for (IFileSpec spec : status) {
            if (spec == null) {
                continue;
            }
            if (spec.getStatusMessage() != null) {
                messages.add(spec);
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

        for (IFileSpec spec : delete) {
            if (integrateMap.containsKey(spec.getDepotPath().getPathString())) {
                moveMap.put(spec, integrateMap.get(spec.getDepotPath().getPathString()));
            }
        }
    }

    public Set<IFileSpec> getAdd() {
        return add;
    }

    public boolean hasAdd() {
        return !add.isEmpty();
    }

    public Set<IFileSpec> getDelete() {
        return delete;
    }

    public boolean hasDelete() {
        return !delete.isEmpty();
    }

    public Set<IFileSpec> getEdit() {
        return edit;
    }

    public boolean hasEdit() {
        return !edit.isEmpty();
    }

    public int getOpenedCount() {
        return add.size() + delete.size() + edit.size();
    }

    public boolean hasOpen() {
        return getOpenedCount() > 0;
    }

    public List<IFileSpec> getOpen() {
        List<IFileSpec> ret = new ArrayList<>();
        ret.addAll(add);
        ret.addAll(delete);
        ret.addAll(edit);
        return ret;
    }

    public List<IFileSpec> getMessages() {
        return messages;
    }

    public List<IFileSpec> getSkipped() {
        return skipped;
    }

    /**
     * If the messages contain an error, then throw an exception.
     */
    public void throwIfError()
            throws RequestException {
        for (IFileSpec message : messages) {
            IServerMessage msg = message.getStatusMessage();
            if (msg != null && msg.isError()) {
                throw new RequestException(msg);
            }
        }
    }

    /**
     *
     * @return mapping of from (deleted) spec to new location (added/edited).
     */
    public Map<IFileSpec, IFileSpec> getMoveMap() {
        return moveMap;
    }
}
