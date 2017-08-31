/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.enumeration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { DiffTypeTest.class, FileActionTest.class,
	FileSpecOpStatusTest.class, LogCallbackTest.class,
	RpcFunctionSpecTest.class, RpcFunctionTypeTest.class,
	SsoCallbackTest.class })
public class EnumSuite {

}
