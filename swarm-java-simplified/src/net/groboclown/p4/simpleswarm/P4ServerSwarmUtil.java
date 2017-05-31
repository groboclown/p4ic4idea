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

package net.groboclown.p4.simpleswarm;

import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;

import java.net.URI;
import java.util.List;

/**
 * Loads swarm information from the p4d server.
 */
public class P4ServerSwarmUtil {
    public static URI getSwarmURI(IOptionsServer server)
            throws P4JavaException {
        final GetPropertyOptions opts = new GetPropertyOptions(
                false, "P4.Swarm.URL", 1, null, null, null, null, 1
        );
        List<IProperty> res = server.getProperty(opts);
        if (res.isEmpty()) {

            return null;
        }
        return URI.create(res.get(0).getValue());
    }

    public static String getTicket(IOptionsServer server, String password)
            throws P4JavaException {
        // need an all-host ticket
        final LoginOptions opts = new LoginOptions(true, true);
        final StringBuffer ticket = new StringBuffer();
        server.login(password, ticket, opts);
        return ticket.toString();
    }
}