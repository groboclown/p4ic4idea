/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.util.Properties;

import com.perforce.p4java.Log;

/**
 * A convenience class used to hold all publicly-visible
 * properties keys used by the RPC implementation.<p>
 * 
 * The intention here is to simply centralize property key names
 * so end users have some idea what's available and what each key's
 * semantics are. This is NOT intended as a full property repository
 * -- property management is generally up to individual classes
 * and packages.<p>
 * 
 * Note that in general, all properties can have an optional short
 * form ("NICK", Anglo-English for nickname...) which is typically
 * just the full form without the cumbersome com.perforce.whatever
 * prefix; use of the short form is particularly convenient for passing
 * in properties through the url mechanism, but you have to be very
 * careful that there are no conflicting system or environmental
 * properties...<p>
 * 
 * Note that if you add a property somewhere that's likely to be
 * useful to end users, you should use the mechanism here to document
 * it.
 * 
 *
 */

public class RpcPropertyDefs {

	/**
	 * The standard property name prefix for all RPC properties.
	 */
	public static final String RPC_PROPERTY_PREFIX = "com.perforce.p4java.rpc.";

	/**
	 * The default property delimiter for RPC properties.
	 */
	public static final String RPC_DEFAULT_PROPERTY_DELIMITER = ",";
	
	/**
	 * Default size in bytes of the "standard" send byte buffer. Usually interpreted
	 * by P4Java as a hint for initial allocation, and may be overridden in the face
	 * of actual conditions. It's rare that you'd want to change this.
	 */
	public static final int RPC_DEFAULT_SEND_BYTE_BUF_SIZE = 40960;
	
	/**
	 * Default size in bytes of the "standard" recv byte buffer. Usually interpreted
	 * by P4Java as a hint for initial allocation, and may be overridden in the face
	 * of actual conditions. It's rare that you'd want to change this.
	 */
	public static final int RPC_DEFAULT_RECV_BYTE_BUF_SIZE = 10240;

	/**
	 * Short form for the RPC_DEFAULT_SEND_BYTE_BUF_SIZE property key.
	 */
	public static final String RPC_DEFAULT_SEND_BYTE_BUF_SIZE_NICK = "defByteSendBufSize";
	
	/**
	 * Short form for the RPC_DEFAULT_RECV_BYTE_BUF_SIZE property key.
	 */
	public static final String RPC_DEFAULT_RECV_BYTE_BUF_SIZE_NICK = "defByteRecvBufSize";
	
	/**
	 * Default size in bytes of the "standard" file I/O buffer. Usually interpreted
	 * by P4Java as a hint for initial allocation, and may be overridden in the face
	 * of actual conditions.
	 */
	public static final int RPC_DEFAULT_FILE_BUF_SIZE = 10240;
	
	/**
	 * Short form for the RPC_DEFAULT_FILE_BUF_SIZE property key.
	 */
	public static final String RPC_DEFAULT_FILE_BUF_SIZE_NICK = "defFileBufSize";
	
	/**
	 * Default size in bytes of the "peek" we take into local files to try
	 * to infer from the contents what type the file is.
	 */
	public static final int RPC_DEFAULT_FILETYPE_PEEK_SIZE = 1024 * 64;
	
	/**
	 * Default setting for enable/disable TCP_NODELAY (disable/enable Nagle's
	 * algorithm).
	 */
	public static final boolean RPC_SOCKET_TCP_NO_DELAY_DEFAULT = true;
	
	/**
	 * If this property is set, attempt to use the associated value to
	 * enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
	 */
	public static final String RPC_SOCKET_TCP_NO_DELAY_NICK = "tcpNoDelay";

	/**
	 * If this property is set and starts with "n" or "N", DON'T use
	 * the socket keepalive option. Defaults to using keepalive.
	 */
	public static final String RPC_SOCKET_USE_KEEPALIVE_NICK = "useKeepAlive";
	
	/**
	 * Default number of milliseconds to use for RPC socket read or write timeouts.
	 */
	public static final int RPC_SOCKET_SO_TIMEOUT_DEFAULT = 30000;
	
	/**
	 * The number of milliseconds to use for RPC socket read or write timeouts.
	 * If set to zero, timeouts are disabled.
	 */
	public static final String RPC_SOCKET_SO_TIMEOUT_NICK = "sockSoTimeout";
	
	/**
	 * Default socket performance preferences are described by three integers
	 * whose values indicate the relative importance of short connection time,
	 * low latency, and high bandwidth.
	 */
	public static final int[] RPC_SOCKET_PERFORMANCE_PREFERENCES_DEFAULT = new int[] {1, 2, 0};
	
	/**
	 * If this property is set, attempt to set the underlying RPC socket's
	 * performance preferences to the associated values, in integers, delimited
	 * by commas. Note that you must specify exactly three integers, otherwise
	 * this property will be ignored.<p>
	 * 
	 * The absolute values of the integers are irrelevant; in order to choose a
	 * protocol the values are simply compared, with larger values indicating
	 * stronger preferences. Negative values represent a lower priority than
	 * positive values.<p>
	 * 
	 * If the application prefers short connection time over both low latency
	 * and high bandwidth, for example, then it could invoke this method with
	 * the values (1, 0, 0). If the application prefers high bandwidth above low
	 * latency, and low latency above short connection time, then it could
	 * invoke this method with the values (0, 1, 2).
	 */
	public static final String RPC_SOCKET_PERFORMANCE_PREFERENCES_NICK = "sockPerfPrefs";
	
	/**
	 * If this property is set, attempt to set the underlying RPC socket's
	 * system receive buffer size to the associated value, in bytes.
	 */
	public static final String RPC_SOCKET_RECV_BUF_SIZE_NICK = "sockRecvBufSize";
	
	/**
	 * If this property is set, attempt to set the underlying RPC socket's
	 * system send buffer size to the associated value, in bytes.
	 */
	public static final String RPC_SOCKET_SEND_BUF_SIZE_NICK = "sockSendBufSize";
	
	/**
	 * Default blocking queue size used in RPC send / recv queues; size
	 * is in elements.
	 */
	public static final int RPC_DEFAULT_QUEUE_SIZE = 10;
	
	/**
	 * If this property is set, attempt to set the underlying RPC connection's
	 * queue lengths to the associated value, in elements.
	 */
	public static final String RPC_DEFAULT_QUEUE_SIZE_NICK = "defQueueSize";
	
	/**
	 * Number of sockets to retain in pool when released
	 */
	public static final String RPC_SOCKET_POOL_SIZE_NICK = "socketPoolSize";
	
	/**
	 * Default number of sockets retained in pool
	 */
	public static final int RPC_SOCKET_POOL_DEFAULT_SIZE = 0;
	
	/**
	 * If this property is set and equals "false", do not trust all certificates.
	 */
	public static final String RPC_SECURE_SOCKET_TRUST_ALL_NICK = "secureSocketTrustAll";
	
	/**
	 * Default secure socket trust all certificates (i.e. self-signed).
	 */
	public static final boolean RPC_DEFAULT_SECURE_SOCKET_TRUST_ALL = true;

	/**
	 * If this property is set, attempt to instantiate the SSLContext with the
	 * associated value. The property value represents a standard name of a
	 * protocol (for example, TLS, SSL, etc.).<p>
	 * 
	 * Use Security.getProviders(), Provider.getServices() and Service.getAlgorithm()
	 * to list all the providers and the algorithms supporter.
	 */
	public static final String RPC_SECURE_SOCKET_PROTOCOL_NICK = "secureSocketProtocol";

	/**
	 * Default secure socket protocol.
	 */
	public static final String RPC_DEFAULT_SECURE_SOCKET_PROTOCOL = "TLS";

	/**
	 * If this property is set and equals "false", do not attempt to set enabled
	 * protocol versions (SSLSocket.setEnabledProtocols()) for the connection
	 * and use the protocol versions currently enabled for the connection.
	 */
	public static final String RPC_SECURE_SOCKET_SET_ENABLED_PROTOCOLS_NICK = "secureSocketSetEnabledProptocols";

	/**
	 * Default secure socket set enabled protocols.
	 */
	public static final boolean RPC_DEFAULT_SECURE_SOCKET_SET_ENABLED_PROTOCOLS = true;

	/**
	 * If this property is set, attempt to set enabled protocol versions
	 * (SSLSocket.setEnabledProtocols()) for the connection. The property value
	 * represents a comma-separated list of one or more protocol versions
	 * (for example, TLSv1, SSLv3, etc.).<p>
	 * 
	 * Use SSLSocket.getSupportedProtocols() and SSLSocket.getEnabledProtocols()
	 * to list all supported and enabled protocol versions for the connection.
	 */
	public static final String RPC_SECURE_SOCKET_ENABLED_PROTOCOLS_NICK = "secureSocketEnabledProtocols";

	/**
	 * Default secure socket enabled protocol versions.<p>
	 * 
	 * The current server (12.1) limit the protocol version support to the
	 * IETF-standard TLSv1.
	 */
	public static final String RPC_DEFAULT_SECURE_SOCKET_ENABLED_PROTOCOLS = "TLSv1";

	/**
	 * If this property is set and equals "true", do not perform
	 * command metadata checks in the RPC layer. This is not recommended,
	 * and you're on your own if you set this to true -- any number of
	 * "interesting" things can happen if not used properly.
	 */
	public static final String RPC_RELAX_CMD_NAME_CHECKS_NICK = "relaxCmdNameChecks";
	
	/**
	 * If this property is set, attempt to set the underlying RPC protocol 'app'
	 * tag to the associated value.
	 */
	public static final String RPC_APPLICATION_NAME_NICK = "applicationName";

	/**
	 * Convenience method to first try to get the short form from the passed-in
	 * properties, then try for the long form. Returns null if it can't find
	 * a definition associated with either short or long form keys.<p>
	 * 
	 * Note: this method is null safe, i.e. if either or both props or nick is null,
	 * it simply returns null.
	 */
	
	public static String getProperty(Properties props, String nick) {
		return getProperty(props, nick, null);
	}
	
	/**
	 * Convenience method to first try to get the short form from the passed-in
	 * properties, then try for the long form. Returns defaultValue if it can't
	 * find a definition associated with either short or long form keys.<p>
	 * 
	 * Note: this method is null safe, i.e. if either or both props or nick is null,
	 * it simply returns null.
	 */
	
	public static String getProperty(Properties props, String nick, String defaultValue) {
		
		if ((props != null) && (nick != null)) {
			String propStr = null;
			if (props.get(nick) != null) {
				propStr = String.valueOf(props.get(nick));
			}
			
			if (propStr == null) {
				if (props.get(RPC_PROPERTY_PREFIX + nick) != null) {
					propStr = String.valueOf(props.get(RPC_PROPERTY_PREFIX + nick));
				}
			}
			
			if (propStr != null) {
				return propStr;
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * Return a named property as an int, if possible. Defaults to defaultValue
	 * if the property wasn't found under first its short form, then its long form,
	 * or if the resulting attempt to convert to an integer was unsuccessful.<p>
	 * 
	 * Will log to P4JLog any conversion error as a warning.<p>
	 * 
	 * Note: this method is null safe, i.e. if either or both props or nick is null,
	 * it simply returns defaultValue.
	 */
	
	public static int getPropertyAsInt(Properties props, String nick, int defaultValue) {
		String propStr = getProperty(props, nick, null);
		int retVal = defaultValue;
		
		if (propStr != null) {
			try {
				retVal = new Integer(propStr);
			} catch (Exception exc) {
				Log.warn("Integer property conversion error; prop name: '"
						+ nick + "'; prop value: "
						+ propStr);
				Log.exception(exc);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Return a named property as an int array, if possible. The property value
	 * is split into values by a specified delimiter (if null, a default delimiter
	 * will be used). Return an empty int array if the property wasn't found under first its short form,
	 * then its long form, or if the resulting attempt to convert to an integer was unsuccessful.<p>
	 * 
	 * Will log to P4JLog any conversion error as a warning.<p>
	 * 
	 * Note: this method is null safe, i.e. if either or both props or nick is null,
	 * it simply returns an empty int array.
	 */
	
	public static int[] getPropertyAsIntArray(Properties props, String nick, String delimiter, int[] defaultValues) {
		String separator = delimiter != null ? delimiter : RPC_DEFAULT_PROPERTY_DELIMITER;
		int[] retVals = defaultValues != null ? defaultValues : new int[]{};

		String propStr = getProperty(props, nick, null);

		if (propStr != null) {
			try {
				String[] items = propStr.split(separator);
				int[] results = new int[items.length];
				for (int i = 0; i < items.length; i++) {
			        results[i] = Integer.parseInt(items[i].trim());
				}
				return results;
			} catch (Exception exc) {
				Log.warn("Integer property conversion error; prop name: '"
						+ nick + "'; prop value: "
						+ propStr);
				Log.exception(exc);
			}
		}
		
		return retVals;
	}

	/**
	 * Return a named property a boolean, if possible. Defaults to defaultValue
	 * if the property wasn't found under first its short form, then its long form,
	 * or if the resulting attempt to convert to an integer was unsuccessful.<p>
	 * 
	 * Will log to P4JLog any conversion error as a warning.<p>
	 * 
	 * Note: this method is null safe, i.e. if either or both props or nick is null,
	 * it simply returns defaultValue.
	 */
	public static boolean getPropertyAsBoolean(Properties props, String nick, boolean defaultValue) {
		String propStr = getProperty(props, nick, null);
		boolean retVal = defaultValue;
		
		if (propStr != null) {
			try {
				return new Boolean(propStr);
			} catch (Exception exc) {
				Log.warn("Integer property conversion error; prop name: '"
						+ nick + "'; prop value: "
						+ propStr);
				Log.exception(exc);
			}
		}
		
		return retVal;
	}
}
