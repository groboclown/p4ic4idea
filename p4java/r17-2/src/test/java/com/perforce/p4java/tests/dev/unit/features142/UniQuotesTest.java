package com.perforce.p4java.tests.dev.unit.features142;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage;

import junit.framework.Assert;

public class UniQuotesTest {

    @Test
    public void testUniQuoteHandling() {
        
        Assert.assertEquals("Perforce password (P4PASSWD) invalid or unset.",
                RpcMessage.interpolateArgs(
                        "Perforce password (%'P4PASSWD'%) invalid or unset.",
                        new HashMap<String, Object>()));
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("bob", "replacement");
        Assert.assertEquals("Contrived replacement %test%",
                RpcMessage.interpolateArgs("%'Contrived'% %bob% %test%", map));
    }

}
