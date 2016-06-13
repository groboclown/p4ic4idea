/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IServerProcess;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Simple default generic implementation class for the P4JServerProcess interface.
 * 
 *
 */

public class ServerProcess extends ServerResource implements IServerProcess {
	
	private int id = 0;
	private String prog = null;
	private String host = null;
	private String client = null;
	private String status = null;
	private String userName = null;
	private String time = null;
	private String command = null;
	private String args = null;
	
	public ServerProcess() {
	}
	
	/**
	 * Construct a new implementation from the map passed back
	 * from the lower level exec map command.
	 * 
	 * @param map map passed back from the Perforce server
	 */
	
	public ServerProcess(Map<String, Object> map) {		
		if (map != null) {
			try {
				if (map.containsKey(MapKeys.ID_LC_KEY)) {
					this.id = new Integer((String) map.get(MapKeys.ID_LC_KEY));
				}
				this.prog = (String) map.get(MapKeys.PROG_LC_KEY);
				this.host = (String) map.get(MapKeys.HOST_LC_KEY);
				this.client = (String) map.get(MapKeys.CLIENT_LC_KEY);
				this.status = (String) map.get(MapKeys.STATUS_LC_KEY);
				this.userName = (String) map.get(MapKeys.USER_LC_KEY);	
				this.time = (String) map.get(MapKeys.TIME_LC_KEY);
				this.command = (String) map.get(MapKeys.COMMAND_LC_KEY);
				this.args = (String) map.get(MapKeys.ARGS_LC_KEY);
			} catch (Throwable thr) {
				Log.warn("Unexpected exception in ServerProcess constructor: "
									+ thr.getMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getId()
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getProg()
	 */
	public String getProg() {
		return this.prog;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getHost()
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getClient()
	 */
	public String getClient() {
		return this.client;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getStatus()
	 */
	public String getStatus() {
		return this.status;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getUserName()
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getTime()
	 */
	public String getTime() {
		return this.time;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getCommand()
	 */
	public String getCommand() {
		return this.command;
	}

	/**
	 * @see com.perforce.p4java.core.IServerProcess#getArgs()
	 */
	public String getArgs() {
		return this.args;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setProg(String prog) {
		this.prog = prog;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setArgs(String args) {
		this.args = args;
	}
}
