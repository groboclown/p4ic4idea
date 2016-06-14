/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.server;

import java.util.Properties;

import com.perforce.p4java.server.IServerAddress;

/**
 * Default implementation of the IServerAddress interface.
 */
public class ServerAddress implements IServerAddress {

	private Protocol protocol = null;
	private String host = null;
	private int port = -1;
	private String query = null;
	private Properties properties = null;
	private String uri = null;
	private String rsh = null;

	/**
	 * Instantiates a new server address.
	 * 
	 * @param builder
	 *            the server address builder
	 */
	ServerAddress(ServerAddressBuilder builder) {
		this.protocol = builder.getProtocol();
		this.host = builder.getHost();
		this.port = builder.getPort();
		this.query = builder.getQuery();
		this.properties = builder.getProperties();
		this.uri = builder.getUri();
		this.rsh = builder.getRsh();
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getProtocol()
	 */
	public Protocol getProtocol() {
		return this.protocol;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getHost()
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getPort()
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getQuery()
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getProperties()
	 */
	public Properties getProperties() {
		return this.properties;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getUri()
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#getRsh()
	 */
	public String getRsh() {
		return this.rsh;
	}

	/**
	 * @see com.perforce.p4java.server.IServerAddress#isSecure()
	 */
	public boolean isSecure() {
		if (this.protocol != null) {
			return this.protocol.isSecure();
		}
		return false;
	}
}
