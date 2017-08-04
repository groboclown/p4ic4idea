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

import com.intellij.openapi.diagnostic.Logger;
import com.perforce.p4java.core.IUserSummary;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class UserSummaryStateList
        implements Iterable<UserSummaryState> {
    private static final Logger LOG = Logger.getInstance(UserSummaryStateList.class);

    private final Map<String, UserSummaryState> users;
    private final Object sync = new Object();

    private UserSummaryStateList(@NotNull Collection<UserSummaryState> users) {
        this.users = new HashMap<String, UserSummaryState>();
        for (UserSummaryState user : users) {
            this.users.put(user.getLoginId(), user);
        }
    }

    public UserSummaryStateList() {
        this.users = new HashMap<String, UserSummaryState>();
    }

    /**
     * Clear out the cached state
     */
    void flush() {
        synchronized (sync) {
            users.clear();
        }
    }

    @NotNull
    @Override
    public Iterator<UserSummaryState> iterator() {
        return copy().values().iterator();
    }

    @NotNull
    public Map<String, UserSummaryState> copy() {
        synchronized (sync) {
            return new HashMap<String, UserSummaryState>(users);
        }
    }

    public void add(@NotNull UserSummaryState user) {
        synchronized (sync) {
            users.put(user.getLoginId(), user);
        }
    }

    public void setUsers(@NotNull Collection<IUserSummary> newUsers) {
        synchronized (sync) {
            users.clear();
            for (IUserSummary newUser : newUsers) {
                UserSummaryState user = new UserSummaryState(newUser);
                user.setUpdated();
                this.users.put(user.getLoginId(), user);
            }
            LOG.debug("Loaded users from P4: " + users);
        }
    }

    @Nullable
    public UserSummaryState get(final String userLoginId) {
        synchronized (sync) {
            return users.get(userLoginId);
        }
    }

    public boolean remove(@NotNull final String userLoginId) {
        synchronized (sync) {
            return users.remove(userLoginId) != null;
        }
    }

    public void serialize(@NotNull Element wrapper, @NotNull EncodeReferences refs) {
        for (UserSummaryState userSummaryState : this) {
            Element child = new Element("u");
            wrapper.addContent(child);
            userSummaryState.serialize(child, refs);
        }
    }

    // Allow for nullable for possible future expansion.
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static UserSummaryStateList deserialize(@NotNull Element usersEl, @NotNull DecodeReferences refs) {
        Set<UserSummaryState> users = new HashSet<UserSummaryState>();
        for (Element element : usersEl.getChildren("u")) {
            UserSummaryState state = UserSummaryState.deserialize(element, refs);
            if (state != null) {
                users.add(state);
            }
        }
        return new UserSummaryStateList(users);
    }

    @NotNull
    public Date getLastUpdated() {
        Date date = null;
        synchronized (sync) {
            for (UserSummaryState state : users.values()) {
                Date userDate = state.getLastUpdated();
                if (date == null) {
                    date = userDate;
                } else if (date.after(userDate)) {
                    date = userDate;
                }
            }
        }
        return date == null ? new Date(1) : date;
    }
}
