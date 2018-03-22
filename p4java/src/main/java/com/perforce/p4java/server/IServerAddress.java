/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.server;

import java.util.Properties;

/**
 * Provides a specification for identifying a Perforce server as a resource in a
 * network. Usually, this means information needed for a Perforce client to
 * communicate with the Perforce server.
 * <p>
 * 
 * This specification has the familiarity of the standard Java URI. The protocol
 * part specifies which network implementation to use and whether it is a secure
 * (SSL) connection. The optional query string part can be used to set P4Java
 * properties.
 * <p>
 * 
 * <pre>
 * The P4Java URI syntax:
 * 
 * protocol://host:port?query
 * 
 * protocol = p4java|p4javassl|p4jrpc|p4jrpcssl|p4jrpcnts|p4jrpcntsssl
 * host = hostname|IP address
 * port = [0-9]* (values 0 to 65535)
 * query = string (i.e. key0=value0&key1=value1...)
 * 
 * p4java - default protocol (same as p4jrpc)
 * p4javassl - secure default protocol (same as p4jrpcssl)
 * p4jrpc - one-shot (connection-per-command) RPC protocol implementation.
 * p4jrpcssl - secure one-shot (connection-per-command) RPC protocol implementation.
 * p4jrpcnts - non-thread-safe (multiple-commands-per-connection) RPC protocol implementation.
 * p4jrpcntsssl - secure non-thread-safe (multiple-commands-per-connection) RPC protocol implementation.
 * p4jrsh - run p4d in 'rsh' mode.
 * 
 * P4Java URI Examples:
 * 
 * p4java://myp4server:1777
 * 
 * p4javassl://myp4server.xyz.com:1777?key0=value0&key1=value1
 * </pre>
 */

public interface IServerAddress {

	/**
	 * Specifies the connection protocol
	 */
	public enum Protocol {
		/**
		 * Default protocol (same as p4jrpc).
		 */
		P4JAVA("p4java"),

		/**
		 * Default SSL protocol (same as p4jrpcssl).
		 */
		P4JAVASSL("p4javassl"),

		/**
		 * One-shot (connection-per-command) RPC protocol.
		 */
		P4JRPC("p4jrpc"),

		/**
		 * One-shot (connection-per-command) SSL RPC protocol.
		 */
		P4JRPCSSL("p4jrpcssl"),

		/**
		 * Non-thread-safe (multiple-commands-per-connection) RPC protocol.
		 */
		P4JRPCNTS("p4jrpcnts"),

		/**
		 * Non-thread-safe (multiple-commands-per-connection) SSL RPC protocol.
		 */
		P4JRPCNTSSSL("p4jrpcntsssl"),

		/**
		 * RSH protocol (run p4d in 'rsh' mode).
		 */
		P4JRSH("p4jrsh"),

		/**
		 * Non-thread-safe (multiple-commands-per-connection) RSH protocol (run p4d in 'rsh' mode).
		 */
		P4JRSHNTS("p4jrshnts");

		/**
		 * The connection protocol in string form.
		 */
		private String protocol;

		/**
		 * Instantiates a new connection protocol.
		 * 
		 * @param protocol
		 *            the connection protocol
		 */
		Protocol(String protocol) {
			this.protocol = protocol;
		}

		/**
		 * Returns the connection protocol as inferred from the passed-in value.
		 * If the value is null, or no such connection protocol can be inferred,
		 * returns null.
		 * 
		 * @param connectionProtocol
		 *            the string value of the connection protocol
		 * @return the connection protocol
		 */
		public static Protocol fromString(String protocol) {
			if (protocol != null) {
				for (Protocol p : Protocol.values()) {
					if (protocol.equalsIgnoreCase(p.protocol)) {
						return p;
					}
				}
			}
			return null;
		}

		/**
		 * Returns the string value representing the connection protocol for the
		 * server URI.
		 * 
		 * @return the string
		 */
		public String toString() {
			return this.protocol;
		}

		/**
		 * Return true if the protocol is secure. <p>
		 * 
		 * We use a pattern 
		 * 
		 * @return true/false
		 */
		public boolean isSecure() {
			if (protocol != null) {
				if (protocol.toLowerCase().endsWith("ssl")) {
					return true;
				}
			}
			return false;
		}
	};

	/**
	 * Return true, if the protocol is secure.
	 * 
	 * @return true/false
	 */
	boolean isSecure();
	
	/**
	 * Gets the URI form of the associated address.
	 * 
	 * @return the uri
	 */
	String getUri();

	/**
	 * Gets the connection protocol.
	 * 
	 * @return the connection protocol
	 */
	Protocol getProtocol();

	/**
	 * Gets the IP address or hostname of the server.
	 * 
	 * @return the host
	 */
	String getHost();

	/**
	 * Gets the port number of the server.
	 * 
	 * @return the port
	 */
	int getPort();

	/**
	 * Gets the query component of the URI.
	 * 
	 * @return the query
	 */
	String getQuery();

	/**
	 * Gets the properties parsed from the query component of the URI.
	 * 
	 * @return the properties
	 */
	Properties getProperties();

	/**
	 * Gets the command for running the server in 'rsh' mode. 
	 * 
	 * @return the rsh
	 */
	String getRsh();
}
