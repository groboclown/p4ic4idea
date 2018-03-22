/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.server;

import java.net.URISyntaxException;
import java.util.Properties;

import com.perforce.p4java.server.IServerAddress.Protocol;

/**
 * The purpose of this class is for building a server address object.
 */
public class ServerAddressBuilder {

	private Protocol protocol = null;
	private String host = null;
	private int port = -1;
	private String query = null;
	private Properties properties = null;
	private String uri = null;
	private String rsh = null;

	/**
	 * Instantiates an empty server address builder.
	 */
	public ServerAddressBuilder() {}
	
	/**
	 * Instantiates a new server address builder from a string.
	 * 
	 * @param serverAddress
	 *            the server address
	 * @throws URISyntaxException
	 */
	public ServerAddressBuilder(String serverAddress) throws URISyntaxException {
		if (serverAddress == null) {
			throw new IllegalArgumentException(
					"The server address cannot be null.");
		}
		parseUri(serverAddress);
	}

	/**
	 * Parse the URI.
	 * <p>
	 * 
	 * Note: assume the URI string is not encoded.
	 * 
	 * @param query
	 *            the query
	 * @throws URISyntaxException
	 */
	private void parseUri(String uri) throws URISyntaxException {

		this.uri = uri;
		
		int protoPartEnd = uri.indexOf("://");
		if (protoPartEnd < 0) {
			throw new URISyntaxException(uri,
					"missing or malformed Perforce scheme / protocol part");
		}
		String protoPart = uri.substring(0, protoPartEnd);
		if ((protoPart.length() + 3) >= uri.length()) {
			throw new URISyntaxException(uri,
					"missing or malformed Perforce server hostname");
		}
		this.protocol = Protocol.fromString(protoPart);
		if (protocol == null) {
			throw new URISyntaxException(uri, "unknown protocol");
		}

		String restStr = uri.substring(protoPartEnd + 3);
		if (protocol == Protocol.P4JRSH || protocol == Protocol.P4JRSHNTS) {
			this.rsh = restStr;
			this.port = 0;
			this.host = "localhost";
		} else {
			int portStart = restStr.lastIndexOf(":");
			int queryStart = restStr.indexOf("?");
			if (portStart == 0) {
				throw new URISyntaxException(uri,
						"missing or malformed Perforce server hostname");
			}
			if ((portStart < 0) || ((queryStart > 0) && (queryStart < portStart))) {
				throw new URISyntaxException(uri,
						"missing or malformed Perforce server port specifier");
			}
			this.host = restStr.substring(0, portStart);
	
			String portPart = null;
			if (queryStart > 0) {
				portPart = restStr.substring(portStart + 1, queryStart);
	
				if (queryStart >= restStr.length()) {
					throw new URISyntaxException(uri,
							"empty or malformed P4Java query string");
				}
				this.query = restStr.substring(queryStart + 1);
			} else {
				portPart = restStr.substring(portStart + 1);
			}
			try {
				this.port = new Integer(portPart);
			} catch (NumberFormatException nfe) {
				throw new URISyntaxException(uri,
						"non-numeric Perforce server port specifier");
			}
	
			if (this.query != null) {
				if (this.properties == null) {
					this.properties = new Properties();
				}
				// Do this without the help of the various servlet classes, mostly
				// because we don't want to drag that entire package into here.
				String[] params = this.query.split("&");
				if (params != null) {
					for (String param : params) {
						if (param != null) {
							String[] pair = param.split("=");
							if ((pair != null) && (pair.length > 0) && (pair[0] != null)) {
								if (pair.length > 1) {
									this.properties.put(pair[0], pair[1] != null ? pair[1] : "");
								} else {
									this.properties.put(pair[0], "");
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Builds the server address.
	 * 
	 * @return the server address
	 */
	public ServerAddress build() {
		return new ServerAddress(this);
	}

	/**
	 * Gets the protocol.
	 * 
	 * @return the protocol
	 */
	public Protocol getProtocol() {
		return this.protocol;
	}

	/**
	 * Sets the protocol.
	 * 
	 * @param protocol the protocol
	 */
	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	/**
	 * Gets the host.
	 * 
	 * @return the host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Sets the host.
	 * 
	 * @param host the host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Gets the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Sets the port.
	 * 
	 * @param port the port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Gets the query.
	 * 
	 * @return the query
	 */
	public String getQuery() {
		return this.query;
	}

	/**
	 * Gets the properties (parsed from the query string).
	 * 
	 * @return the properties
	 */
	public Properties getProperties() {
		return this.properties;
	}

	/**
	 * Gets the uri.
	 * 
	 * @return the uri
	 */
	public String getUri() {
		if (this.uri != null)
			return this.uri;
		
		String newUri = protocol.toString() + "://";
		if (protocol == Protocol.P4JRSH || protocol == Protocol.P4JRSHNTS) {
			newUri += this.rsh;
		} else {
			newUri += this.host + ":" + this.port;
		}
		return newUri;
	}

	/**
	 * Gets the rsh command.
	 * 
	 * @return the rsh command
	 */
	public String getRsh() {
		return this.rsh;
	}

	/**
	 * Sets the rsh command.
	 * 
	 * @param rsh the rsh command
	 */
	public void setRsh(String rsh) {
		this.rsh = rsh;
	}
}
