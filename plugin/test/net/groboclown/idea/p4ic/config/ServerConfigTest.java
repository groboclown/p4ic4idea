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

package net.groboclown.idea.p4ic.config;

import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.project.Project;
import net.groboclown.idea.p4ic.config.part.DataPart;
import net.groboclown.idea.p4ic.config.part.SimpleDataPart;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ServerConfigTest {
    @Test
    public void testIsSameServer_null() {
        ServerConfig config = ServerConfig.createFrom(createPart1());
        assertThat(config.isSameServer(null), is(false));
    }

    @Test
    public void testIsSameServer_sameSource() {
        DataPart part1 = createPart1();
        ServerConfig config = ServerConfig.createFrom(part1);
        assertThat(config.isSameServer(part1), is(true));
    }

    @Test
    public void testIsSameServer_sameValues() {
        ServerConfig config = ServerConfig.createFrom(createPart1());
        assertThat(config.isSameServer(createPart1()), is(true));
    }

    @Test
    public void testIsSameServer_altValues1() {
        ServerConfig config = ServerConfig.createFrom(createPart1());
        assertThat(config.isSameServer(createPart1_alt()), is(true));
    }

    @Test
    public void testIsSameServer_altValues2() {
        ServerConfig config = ServerConfig.createFrom(createPart2a());
        assertThat(config.isSameServer(createPart2b()), is(true));
    }

    @Test
    public void testIsSameServer_altValues3() {
        ServerConfig config = ServerConfig.createFrom(createPart2b());
        assertThat(config.isSameServer(createPart2a()), is(true));
    }


    private DataPart createPart1() {
        Project p = DummyProject.getInstance();
        SimpleDataPart ret = new SimpleDataPart(p, Collections.<String, String>emptyMap());
        ret.setServerName("host:1234");
        ret.setClientname("abc");
        ret.setLoginSsoFile("c/b/c");
        ret.setDefaultCharset("latin1");
        ret.setClientHostname("localhost");
        ret.setIgnoreFilename(".p4ignore");
        ret.setTrustTicketFile("a/b/c");
        ret.setAuthTicketFile("d/e/f");
        ret.setUsername("user");
        return ret;
    }

    private DataPart createPart1_alt() {
        Project p = DummyProject.getInstance();
        SimpleDataPart ret = new SimpleDataPart(p, Collections.<String, String>emptyMap());
        ret.setServerName("tcp:host:1234");
        ret.setClientname("abc");
        ret.setLoginSsoFile("c/b/c");
        ret.setDefaultCharset("latin1");
        ret.setClientHostname("localhost");
        ret.setIgnoreFilename(".p4ignore");
        ret.setTrustTicketFile("a/b/c");
        ret.setAuthTicketFile("d/e/f");
        ret.setUsername("user");
        return ret;
    }

    private DataPart createPart2a() {
        Project p = DummyProject.getInstance();
        SimpleDataPart ret = new SimpleDataPart(p, Collections.<String, String>emptyMap());
        ret.setServerName("perforce:1666");
        ret.setUsername("user");
        return ret;
    }

    private DataPart createPart2b() {
        Project p = DummyProject.getInstance();
        SimpleDataPart ret = new SimpleDataPart(p, Collections.<String, String>emptyMap());
        ret.setServerName("perforce:1666");
        ret.setUsername("user");
        ret.setClientname("dev");
        return ret;
    }
}
