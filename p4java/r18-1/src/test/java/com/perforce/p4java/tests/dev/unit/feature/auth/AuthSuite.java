/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.tests.dev.unit.feature.auth;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Kevin Sawicki (ksawicki@perforce.com)
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { AuthTicketTest.class, ProxyAuthTest.class })
public class AuthSuite {

}
