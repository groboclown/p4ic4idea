/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionType;
import com.perforce.p4java.tests.dev.annotations.TestId;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@TestId("RpcFunctionTypeTest")
public class RpcFunctionTypeTest extends BaseEnumTestHelper {

    /**
     * @see BaseEnumTestHelper#getEnum()
     */
    protected RpcFunctionType[] getEnum() {
	return RpcFunctionType.values();
    }

    /**
     * @see BaseEnumTestHelper#valueOf(String)
     */
    protected RpcFunctionType valueOf(String name) {
	return RpcFunctionType.valueOf(name);
    }

}
