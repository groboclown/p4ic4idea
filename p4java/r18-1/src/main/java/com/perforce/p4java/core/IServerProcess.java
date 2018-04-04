/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.core;

/**
 * Defines the fields available for each Perforce server process
 * object returned from the IServer getServerProcessList method.
 * Fields defined here are documented in the main Perforce documentation
 * and will not detailed here.<p>
 * 
 * Note that any of the String-returning methods below may return null even
 * on non-error results.<p>
 * 
 * IServerProcess objects are always complete and not refreshable or
 * updatable.
 * 
 *
 */

public interface IServerProcess extends IServerResource {
	int getId();
	String getProg();
	String getHost();
	String getClient();
	String getTime();
	String getStatus();
	String getCommand();
	String getUserName();
	String getArgs();
}
