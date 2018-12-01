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

import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.impl.generic.core.UserSummary;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Caches the user summary data.  User modification is not supported by the
 * plugin, so the fields are read-only.
 */
public class UserSummaryState extends CachedState {
    private final String loginId;
    private final String fullName;
    private final String email;

    private UserSummaryState(@NotNull String loginId, @NotNull String fullName, @NotNull String email) {
        this.loginId = loginId;
        this.fullName = fullName;
        this.email = email;
    }

    public UserSummaryState(@NotNull IUserSummary userSummary) {
        this(userSummary.getLoginName(), userSummary.getFullName(), userSummary.getEmail());
    }

    @NotNull
    public String getLoginId() {
        return loginId;
    }

    @NotNull
    public String getFullName() {
        return fullName;
    }

    @NotNull
    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return loginId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (UserSummaryState.class.equals(o.getClass()))) {
            return false;
        }
        return ((UserSummaryState) o).loginId.equals(loginId);
    }

    @Override
    public int hashCode() {
        return loginId.hashCode();
    }

    @Override
    protected void serialize(@NotNull Element wrapper, @NotNull EncodeReferences ref) {
        serializeDate(wrapper);
        wrapper.setAttribute("l", loginId);
        wrapper.setAttribute("n", fullName);
        wrapper.setAttribute("e", email);
    }


    @Nullable
    protected static UserSummaryState deserialize(@NotNull final Element wrapper, @NotNull final DecodeReferences refs) {
        final String loginId = getAttribute(wrapper, "l");
        final String fullName = getAttribute(wrapper, "n");
        final String email = getAttribute(wrapper, "e");
        if (loginId == null || fullName == null || email == null) {
            return null;
        }
        return new UserSummaryState(loginId, fullName, email);
    }}
