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
package net.groboclown.p4.server.impl.cache.store;

import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static net.groboclown.idea.ExtAsserts.assertContainsExactly;
import static org.junit.jupiter.api.Assertions.*;

class PrimitiveMapTest {
    @Test
    void serializeDeserialize()
            throws PrimitiveMap.UnmarshalException {
        PrimitiveMap map = new PrimitiveMap();
        map.putString("sk", "string key value");
        map.putStringList("slk", Arrays.asList("one", "two", "three"));
        map.putInt("ixk", Integer.MAX_VALUE);
        map.putInt("ink", Integer.MIN_VALUE);
        map.putLong("lxk", Long.MAX_VALUE);
        map.putLong("lnk", Long.MIN_VALUE);

        Element serialized = XmlSerializer.serialize(map);
        assertNotNull(serialized);
        PrimitiveMap demap = XmlSerializer.deserialize(serialized, PrimitiveMap.class);
        assertEquals("string key value", demap.getStringNotNull("sk"));
        assertContainsExactly(map.getStringList("slk"), "one", "two", "three");
        assertEquals(Integer.MAX_VALUE, demap.getIntNullable("ixk", 0));
        assertEquals(Integer.MIN_VALUE, demap.getIntNullable("ink", 0));
        assertEquals(Long.MAX_VALUE, demap.getLongNullable("lxk", 0));
        assertEquals(Long.MIN_VALUE, demap.getLongNullable("lnk", 0));
    }
}