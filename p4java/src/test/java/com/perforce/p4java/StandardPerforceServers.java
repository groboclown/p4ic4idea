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

package com.perforce.p4java;

import com.perforce.test.CaseSensitiveTestServer;
import com.perforce.test.ServerRule;
import com.perforce.test.TestServer;

import java.util.Properties;

/**
 * Standard Perforce servers
 */
public class StandardPerforceServers {
    /**
     * Standard P4JavaTestCase server
     * p4java://eng-p4java-vm.perforce.com:20132
     */
    public static ServerRule createP4Java20132() {
        // FIXME construct a forwarding replica server.
        return new ServerRule(
                new CaseSensitiveTestServer(),
                new ServerRule.P4dVersion("r17.1"),
                new ServerRule.InitializeWith(
                        StandardPerforceServers.class,
                        "data/server-20132/depot.tar.gz",
                        null
                ),
                new ServerRule.RunAsync()
        );
    }

    /**
     * Standard P4JavaTestCase port.
     */
    public static final String PORT_P4JAVA_20132 = "p4java://localhost:" + TestServer.DEFAULT_P4_PORT;
    public static final String PORT_NTS_20132 = "p4jrpcnts://localhost:" + TestServer.DEFAULT_P4_PORT;


    public static ServerRule createP4Java20101() {
        return new ServerRule(
                new CaseSensitiveTestServer(),
                new ServerRule.P4dVersion("r17.1"),
                new ServerRule.InitializeWith(
                        StandardPerforceServers.class,
                        "data/server-20101/depot.tar.gz",
                        null
                ),
                new ServerRule.RunAsync()
        );
    }



    public static Properties getStandardUserProperties() {
        Properties props = getDefaultProperties();
        props.setProperty(PropertyDefs.USER_NAME_KEY, "luser");
        props.setProperty(PropertyDefs.PASSWORD_KEY, "password1");
        return props;
    }

    public static Properties getSuperUserProperties() {
        Properties props = getDefaultProperties();
        props.setProperty(PropertyDefs.USER_NAME_KEY, "lsuper");
        props.setProperty(PropertyDefs.PASSWORD_KEY, "password2");
        return props;
    }

    public static Properties getDefaultProperties() {
        Properties props = new Properties();
        return props;
    }
}
