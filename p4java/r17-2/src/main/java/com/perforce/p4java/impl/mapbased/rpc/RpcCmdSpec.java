/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.util.Map;


/**
 * Encapsulates all we know about a command coming in from the upper
 * levels of the P4Java implementation. Used as a common currency
 * in the various dispatchers and associated plumbing.
 */

public class RpcCmdSpec {

	private String cmdName = null;
	private String[] cmdArgs = null;
	private String cmdTicket = null;
	private Map<String, Object> inMap = null;
	private String inString = null;
	private ExternalEnv cmdEnv = null;
	
	public RpcCmdSpec(String cmdName, String[] cmdArgs, String cmdTicket,
								Map<String, Object> inMap, String inString, ExternalEnv cmdEnv) {
		this.cmdName = cmdName;
		this.cmdArgs = cmdArgs;
		this.cmdTicket = cmdTicket;
		this.inMap = inMap;
		this.inString = inString;
		this.cmdEnv = cmdEnv;
	}

	public String getCmdName() {
		return this.cmdName;
	}

	public void setCmdName(String cmdName) {
		this.cmdName = cmdName;
	}

	public String[] getCmdArgs() {
		return this.cmdArgs;
	}

	public void setCmdArgs(String[] cmdArgs) {
		this.cmdArgs = cmdArgs;
	}

	public Map<String, Object> getInMap() {
		return this.inMap;
	}

	public void setInMap(Map<String, Object> inMap) {
		this.inMap = inMap;
	}

	public ExternalEnv getCmdEnv() {
		return this.cmdEnv;
	}

	public void setCmdEnv(ExternalEnv cmdEnv) {
		this.cmdEnv = cmdEnv;
	}

	public String getCmdTicket() {
		return this.cmdTicket;
	}

	public void setCmdTicket(String cmdTicket) {
		this.cmdTicket = cmdTicket;
	}

	public String getInString() {
		return inString;
	}

	public void setInString(String inString) {
		this.inString = inString;
	}
}
