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

package net.groboclown.idea.p4ic.v2.server.cache.state;

import net.groboclown.idea.p4ic.v2.server.cache.UpdateAction;
import net.groboclown.idea.p4ic.v2.server.cache.UpdateGroup;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map.Entry;

/**
 * Pending updates are descriptive, not prescriptive.  The reconcilers handle
 * changing the state based on updates and states.
 */
public class PendingUpdateState extends CachedState {
    private final UpdateAction action;
    private final Set<String> ids;
    private final Map<String, Object> parameters;

    public PendingUpdateState(@NotNull final UpdateAction action, @NotNull final Set<String> ids,
            @NotNull Map<String, Object> parameters) {
        this.action = action;
        this.ids = Collections.unmodifiableSet(ids);
        final HashMap<String, Object> p = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry: parameters.entrySet()) {
            if (action.isValidParameterName(entry.getKey()) && entry.getValue() != null) {
                p.put(entry.getKey(), entry.getValue());
            } else {
                // No need for bundle - this is an internal error
                throw new IllegalArgumentException("Invalid parameters for action " + action + ": " + parameters);
            }
        }
        this.parameters = Collections.unmodifiableMap(p);
    }

    @NotNull
    public UpdateAction getUpdateAction() {
        return action;
    }

    @NotNull
    public UpdateGroup getUpdateGroup() {
        return action.getGroup();
    }

    /** the objects that this update relates to; these must be unique per UpdateGroup per client */
    @NotNull
    public Set<String> getObjectIds() {
        return ids;
    }

    @NotNull
    public Map<String, Object> getParameters() {
        return parameters;
    }


    @Override
    protected void serialize(@NotNull Element wrapper, @NotNull EncodeReferences refs) {
        wrapper.setAttribute("a", action.toString());
        for (String id: ids) {
            Element el = new Element("i");
            wrapper.addContent(el);
            el.setAttribute("k", id);
        }
        for (Entry<String, Object> entry : parameters.entrySet()) {
            Element el = new Element("p");
            wrapper.addContent(el);
            el.setAttribute("p", entry.getKey());
            el.setAttribute("v", action.serialize(entry.getKey(), entry.getValue()));
        }
    }

    public static PendingUpdateState deserialize(@NotNull Element wrapper, @NotNull DecodeReferences refs) {
        String actionStr = getAttribute(wrapper, "a");
        UpdateAction action = null;
        if (actionStr == null) {
            return null;
        }
        try {
            action = UpdateAction.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Set<String> ids = new HashSet<String>();
        for (Element el: wrapper.getChildren("i")) {
            String id = getAttribute(el, "k");
            if (id != null) {
                ids.add(id);
            }
        }
        Map<String, Object> params = new HashMap<String, Object>();
        for (Element el: wrapper.getChildren("p")) {
            String key = getAttribute(el, "p");
            Object val = action.deserialize(key, getAttribute(el, "v"));
            if (key != null && val != null) {
                params.put(key, val);
            }
        }
        return new PendingUpdateState(action, ids, params);
    }
}
