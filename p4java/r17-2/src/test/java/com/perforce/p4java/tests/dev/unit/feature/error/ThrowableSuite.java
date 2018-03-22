/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.error;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { AccessExceptionTest.class, ClientErrorTest.class,
	ConfigExceptionTest.class, ConnectionExceptionTest.class,
	ConnectionNotConnectedExceptionTest.class,
	NoSuchObjectExceptionTest.class, NullPointerErrorTest.class,
	OptionsExceptionTest.class, P4JavaErrorTest.class,
	P4JavaExceptionTest.class, ProtocolErrorTest.class,
	RequestExceptionTest.class, ResourceExceptionTest.class,
	UnimplementedErrorTest.class })
public class ThrowableSuite {

}
