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
package net.groboclown.idea.p4ic.p4javaapi;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerAddress;
import com.perforce.p4java.server.ServerFactory;
import net.groboclown.idea.p4ic.config.P4Config;

import java.net.URISyntaxException;
import java.util.Properties;

public class P4Util {
    /**
     * Hard coded connection information for testing
     *
     * @return
     */
    static IOptionsServer connect() throws P4JavaException, URISyntaxException {
        final Properties properties = new Properties();
        properties.setProperty(PropertyDefs.PROG_NAME_KEY, "IntelliJ Perforce Community Plugin");
        properties.setProperty(PropertyDefs.PROG_VERSION_KEY, "1");

        properties.setProperty(PropertyDefs.IGNORE_FILE_NAME_KEY, P4Config.P4_IGNORE_FILE);
        properties.setProperty(PropertyDefs.ENABLE_PROGRESS, "1");
        properties.setProperty(PropertyDefs.ENABLE_TRACKING, "1");
        properties.setProperty(PropertyDefs.WRITE_IN_PLACE_KEY, "1");
        properties.setProperty(PropertyDefs.USER_NAME_KEY, "user");
        properties.setProperty(PropertyDefs.PASSWORD_KEY, "test");

        final String url = IServerAddress.Protocol.P4JAVA.toString() + "://" + "localhost:1666";
        IOptionsServer server = ServerFactory.getOptionsServer(url, properties);
        server.connect();
        server.login("test", new LoginOptions(false, true));
        return server;
    }


    static IClient loadClient(IOptionsServer server) throws ConnectionException, AccessException, RequestException {
        IClient ret = server.getClient("user_client");
        server.setCurrentClient(ret);
        return ret;
    }
}
