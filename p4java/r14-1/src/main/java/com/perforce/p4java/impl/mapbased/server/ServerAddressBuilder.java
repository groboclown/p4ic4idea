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
	 * Gets the host.
	 * 
	 * @return the host
	 */
	public String getHost() {
		return this.host;
	}

	/**
	 * Gets the port.
	 * 
	 * @return the host
	 */
	public int getPort() {
		return this.port;
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
		return this.uri;
	}
}
