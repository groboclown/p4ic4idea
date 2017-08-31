/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.features132;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.perforce.p4java.admin.IProperty;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.option.server.GetPropertyOptions;
import com.perforce.p4java.option.server.PropertyOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.tests.dev.annotations.TestId;
import com.perforce.p4java.tests.dev.unit.P4JavaTestCase;

/**
 * Test P4Java supports 'large errors': p4 cmd --explain
 */
@TestId("Dev132_PropertyALTest")
public class PropertyALTest extends P4JavaTestCase {
    
    IOptionsServer adminserver = null;

	/**
	 * Test that P4Java handles 'p4 property -l -A' output change 
	 */
	@Test
	public void testPropertyAL() {
	
        String pname = "PropertyALTest";
        String pval = "PropertyALTest";
        int pseq = 4;
        
        HashMap<String, String> targets = new HashMap<String, String>();
        targets.put("2013.1", "p4java://eng-p4java-vm.perforce.com:20131");
        targets.put("2013.2", "p4java://eng-p4java-vm.perforce.com:20132");
        
		try {
		    for(Entry<String, String> target : targets.entrySet() ) {
		        
    			// Connect to a 2013.1 server for 1st phase
    		    serverUrlString = target.getValue();
    			
    			adminserver = getServerAsSuper();
                assertNotNull(adminserver);
    
    			// Connect to the server.
                adminserver.connect();
    
                adminserver.setProperty(pname, pval,
                        new PropertyOptions().setSequence(pseq));
                
                List<IProperty> properties = adminserver.getProperty(
                        new GetPropertyOptions().setListAll(true));
                
                boolean seen = false;
                for (IProperty prop : properties) {
                    if(prop.getName().equals(pname)) {
                        assertEquals("value must match",
                                prop.getValue(), pval);
                        assertEquals("sequence must match",
                                prop.getSequence(), pseq);
                        seen = true;
                        break;
                    }
                }
                assertTrue("must have seen our property (object): " +
                            target.getKey(), seen);
                
                seen = false;
                HashMap<String, Object> inMap = new HashMap<String, Object>();
                inMap.put(Server.IN_MAP_USE_TAGS_KEY, "false");
                List<Map<String, Object>> res = adminserver.execMapCmdList(
                        "property", new String[] {"-l", "-A"}, inMap);
                for (Map<String, Object> map : res) {
                    if (map.containsKey("fmt0") && map.containsValue(pval)) {
                        String msg = RpcMessage.interpolateArgs(
                                (String) map.get("fmt0"), map);
                        if (msg.contains("PropertyALTest = PropertyALTest")) {
                            if (target.getKey().equals("2013.1")) {
                                assertEquals(
                                        "PropertyALTest = PropertyALTest", msg);
                            } else {
                                assertEquals(
                               "PropertyALTest = PropertyALTest (any) #4", msg);
                            }
                            seen = true;
                            break;
                        }
                    }
                }
                assertTrue("must have seen our property (text): " +
                        target.getKey(), seen);
                
                adminserver.deleteProperty(pname,
                        new PropertyOptions().setSequence(pseq));
                endServerSession(adminserver);
                adminserver = null;
		    }

			
		} catch (P4JavaException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} catch (URISyntaxException e) {
			fail("Unexpected exception: " + e.getLocalizedMessage());
		} finally {
		    try {
		        adminserver.deleteProperty(pname, null);
		    } catch (Throwable e) {}
            if (adminserver != null) {
                this.endServerSession(adminserver);
            }
		}
	}
}
