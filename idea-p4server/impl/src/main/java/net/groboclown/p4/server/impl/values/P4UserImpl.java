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

import com.perforce.p4java.core.IUserSummary;
import net.groboclown.p4.server.api.values.P4User;
import org.jetbrains.annotations.NotNull;

public class P4UserImpl implements P4User {
    private final String username;
    private final String email;
    private final String fullName;

    public P4UserImpl(@NotNull IUserSummary summary) {
        this.username = summary.getLoginName();
        this.email = summary.getEmail();
        this.fullName = summary.getFullName();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getFullName() {
        return fullName;
    }
}
