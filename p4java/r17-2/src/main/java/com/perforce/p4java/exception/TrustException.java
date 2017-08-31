/*
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.exception;

/**
 * Special subclass of ConnectionException to signal the case of attempting to
 * establish trust for a SSL connection to the Perforce server.
 * <p>
 * 
 * Note that the exception should be handled with addition steps to try to
 * establish trust again. The fingerprint will be set to this exception object
 * whenever possible. This fingerprint should be used as a parameter for
 * executing the IOptionsServer.addTrust(String fingerprint) method to install
 * trust to the Perforce SSL connection. Only the "NEW_CONNECTION" and "NEW_KEY"
 * types should be handled for trust retries.
 */

public class TrustException extends ConnectionException {

	private static final long serialVersionUID = 1L;

	private Type type = null;

	private String serverHostPort = null;
	private String serverIpPort = null;
	private String fingerprint = null;

	public enum Type {
		NEW_CONNECTION, NEW_KEY, INSTALL, UNINSTALL
	};

	public TrustException(Type type, String serverHostPort,
			String serverIpPort, String fingerprint, String message) {
		super(message);
		this.type = type;
		this.serverHostPort = serverHostPort;
		this.serverIpPort = serverIpPort;
		this.fingerprint = fingerprint;
	}

	public TrustException(Type type, String serverHostPort,
			String serverIpPort, String fingerprint, String message,
			Throwable cause) {
		super(message, cause);
		this.type = type;
		this.serverHostPort = serverHostPort;
		this.serverIpPort = serverIpPort;
		this.fingerprint = fingerprint;
	}

	public Type getType() {
		return type;
	}

	public String getServerHostPort() {
		return serverHostPort;
	}

	public String getServerIpPort() {
		return serverIpPort;
	}

	public String getFingerprint() {
		return fingerprint;
	}
}
