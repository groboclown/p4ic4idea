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
package net.groboclown.p4.server.api;

import com.perforce.p4java.server.IServerAddress;
import net.groboclown.idea.extensions.TemporaryFolder;
import net.groboclown.idea.extensions.TemporaryFolderExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class P4ServerNameTest {
    @Test
    void forPort_null() {
        assertThat(P4ServerName.forPort(null), nullValue());
    }

    @Test
    void forPort_ssl() {
        P4ServerName name = P4ServerName.forPort("ssl://host:1234");
        assertThat("null",
                name,
                notNullValue());
        assertThat("secure",
                name.isSecure(),
                is(true));
        assertThat("connection type",
                name.getServerProtocol(),
                is(IServerAddress.Protocol.P4JRPCNTSSSL));
        assertThat("port",
                name.getServerPort(),
                is("host:1234"));
        assertThat("display",
                name.getDisplayName(),
                is("ssl:host:1234"));
        assertThat("url",
                name.getUrl(),
                is("p4jrpcntsssl://host:1234"));
        assertThat("full port",
                name.getFullPort(),
                is("ssl://host:1234"));
        assertThat("protocol name",
                name.getProtocolName(),
                is("ssl"));
    }

    @Test
    void forPort_justPort() {
        P4ServerName name = P4ServerName.forPort("1667");
        assertThat("null",
                name,
                notNullValue());
        assertThat("secure",
                name.isSecure(),
                is(false));
        assertThat("connection type",
                name.getServerProtocol(),
                is(IServerAddress.Protocol.P4JRPCNTS));
        assertThat("port",
                name.getServerPort(),
                is("localhost:1667"));
        assertThat("display",
                name.getDisplayName(),
                is("localhost:1667"));
        assertThat("url",
                name.getUrl(),
                is("p4jrpcnts://localhost:1667"));
        assertThat("full port",
                name.getFullPort(),
                is("localhost:1667"));
        assertThat("protocol name",
                name.getProtocolName(),
                is("java"));
    }

    @Test
    void forPort_hostPort() {
        P4ServerName name = P4ServerName.forPort("host:1234");
        assertThat("null",
                name,
                notNullValue());
        assertThat("secure",
                name.isSecure(),
                is(false));
        assertThat("connection type",
                name.getServerProtocol(),
                is(IServerAddress.Protocol.P4JRPCNTS));
        assertThat("port",
                name.getServerPort(),
                is("host:1234"));
        assertThat("display",
                name.getDisplayName(),
                is("host:1234"));
        assertThat("url",
                name.getUrl(),
                is("p4jrpcnts://host:1234"));
        assertThat("full port",
                name.getFullPort(),
                is("host:1234"));
        assertThat("protocol name",
                name.getProtocolName(),
                is("java"));
    }

    @Test
    void forPort_simpleProtocolPort() {
        P4ServerName name = P4ServerName.forPort("tcp:host:1234");
        assertThat("null",
                name,
                notNullValue());
        assertThat("secure",
                name.isSecure(),
                is(false));
        assertThat("connection type",
                name.getServerProtocol(),
                is(IServerAddress.Protocol.P4JRPCNTS));
        assertThat("port",
                name.getServerPort(),
                is("host:1234"));
        assertThat("display",
                name.getDisplayName(),
                is("host:1234"));
        assertThat("url",
                name.getUrl(),
                is("p4jrpcnts://host:1234"));
        assertThat("full port",
                name.getFullPort(),
                is("host:1234"));
        assertThat("protocol name",
                name.getProtocolName(),
                is("java"));
    }

    @ExtendWith(TemporaryFolderExtension.class)
    @Test
    void forPort_rsh(TemporaryFolder tmpDir) {
        // This is what the TestServer uses as the protocol.
        String port = "p4jrsh://"
                + tmpDir.newFile("p4d").getAbsolutePath()
                + " -r "
                + tmpDir.newFile("root").getAbsolutePath()
                + ' '
                + " -L log -i --java";
        P4ServerName name = P4ServerName.forPort(port);
        assertNotNull(name);
        assertThat(name.getServerProtocol(), is(IServerAddress.Protocol.P4JRSHNTS));
    }

    @Test
    void equals1() {
        P4ServerName name1 = P4ServerName.forPort("tcp:host:1234");
        P4ServerName name2 = P4ServerName.forPort("host:1234");
        assertThat(name1, is(name2));
    }

}