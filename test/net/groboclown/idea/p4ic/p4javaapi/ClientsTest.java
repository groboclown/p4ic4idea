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

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

public class ClientsTest {
    @Test
    public void testChanges() throws P4JavaException, URISyntaxException {
        IOptionsServer server = P4Util.connect();
        IClient client = P4Util.loadClient(server);
        List<IChangelistSummary> changelists = server.getChangelists(0,
                Collections.<IFileSpec>emptyList(),
                client.getName(), null, false, false, true, true);

        assertThat(
                changelists,
                notNullValue());

        for (IChangelistSummary sum: changelists) {
            System.out.println("Changelist: " + sum.getId());
        }

        assertThat(
                changelists.size(),
                is(6));
    }


    @Test
    public void testOpened() {

    }


    @Test
    public void testRevert() {

    }


    @Test
    public void testFsync() {

    }
}
