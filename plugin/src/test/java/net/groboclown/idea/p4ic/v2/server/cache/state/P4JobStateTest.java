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

import org.jdom.Element;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class P4JobStateTest {
    @Test
    public void testGetId() {
        P4JobState job = new P4JobState("testid");
        assertThat(job.getId(), is("testid"));
    }

    @Test
    public void testDetails() {
        P4JobState job = new P4JobState("testid");
        assertThat(job.getDetails(), notNullValue());
    }

    @Test
    public void testDescription() {
        P4JobState job = new P4JobState("testid");
        assertThat(job.getDescription(), is(nullValue()));
        job.setDescription("desc value");
        assertThat(job.getDescription(), is("desc value"));
    }

    @Test
    public void testDeserializationNull() {
        Element el = new Element("x");
        DecodeReferences refs = new DecodeReferences();

        assertThat(P4JobState.deserialize(el, refs), is(nullValue()));
    }

    @Test
    public void testSerialization1() {
        Element el = new Element("x");
        DecodeReferences refs = new DecodeReferences();
        el.setAttribute("n", "testid1");

        P4JobState job = P4JobState.deserialize(el, refs);
        assertThat(job, notNullValue());
        assertThat(job.getId(), is("testid1"));
        assertThat(job.getLastUpdated(), is(CachedState.NEVER_LOADED));
        assertThat(job.getDetails().size(), is(0));
        assertThat(job.getDescription(), is(""));
    }

    @Test
    public void testSerialization2() {
        P4JobState init = new P4JobState("testid2");
        init.setDescription("desc value");
        init.lastUpdated = new Date();
        init.getDetails().put("a", "b");
        init.getDetails().put("c", "d");

        Element el = new Element("x");
        EncodeReferences encs = new EncodeReferences();
        init.serialize(el, encs);

        DecodeReferences decs = new DecodeReferences();
        final P4JobState job = P4JobState.deserialize(el, decs);
        assertThat(job, notNullValue());
        assertThat(job.getId(), is("testid2"));
        assertThat(job.getLastUpdated(), is(init.getLastUpdated()));
        assertThat(job.getDescription(), is("desc value"));
        assertThat(job.getDetails().size(), is(2));
        assertThat(job.getDetails().get("a"), is("b"));
        assertThat(job.getDetails().get("c"), is("d"));
    }
}
