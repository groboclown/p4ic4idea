/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionSpec;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("RpcFunctionSpecTest")
public class RpcFunctionSpecTest extends BaseEnumTestHelper {

    /**
     * @see BaseEnumTestHelper#getEnum()
     */
    protected RpcFunctionSpec[] getEnum() {
	return RpcFunctionSpec.values();
    }

    /**
     * @see BaseEnumTestHelper#valueOf(String)
     */
    protected RpcFunctionSpec valueOf(String name) {
	return RpcFunctionSpec.valueOf(name);
    }

}
