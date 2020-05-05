/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc;

import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.ProtocolError;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcMarshalable;
import com.perforce.p4java.impl.mapbased.rpc.packet.RpcPacketField;
import com.perforce.p4java.impl.mapbased.server.Server;

/**
 * Used as a container for each Perforce RPC call's external (operating
 * system, calling program, etc.) environment. Serialized as the first part of
 * most client-side RPC user command calls. Only sent once per user
 * command dispatch loop (as far as is known).
 * 
 *
 */

public class ExternalEnv implements RpcMarshalable {
	
	private static final String RPC_ARGNAME_ENV_PROGRAM = "prog";
	private static final String RPC_ARGNAME_ENV_VERSION = "version";
	private static final String RPC_ARGNAME_ENV_CLIENTNAME = "client";
	private static final String RPC_ARGNAME_ENV_CWD = "cwd";
	private static final String RPC_ARGNAME_ENV_HOSTNAME = "host";
	private static final String RPC_ARGNAME_ENV_PORT = "port";
	private static final String RPC_ARGNAME_ENV_LANGUAGE = "language";
	private static final String RPC_ARGNAME_ENV_OSNAME = "os";
	private static final String RPC_ARGNAME_ENV_USERNAME = "user";
	private static final String RPC_ARGNAME_ENV_UNICODE = "unicode";
	
	private String progName = null;	// The calling program's name
	private String version = null;	// Calling program's version
	private String client = null;	// this client's name
	private String cwd = null;		// the current working directory
	private String host = null;		// this client's host name (for host locking)
	private String port = null;		// the Perforce server port (i.e. perforce:1666)
	private String language = null;	// preferred language for error messages
	private String os = null;		// the name of the OS (for calculating paths)
	private String user = null;		// the user's name
	private boolean unicode = false;	// using unicode (utf8) or not?
	private Charset currentCharset = CharsetDefs.LOCAL; // what the reigning charset is.
	
	public ExternalEnv(String progName, String version, String client, String cwd,
			String host, String port, String language, String os, String user,
			boolean unicode, Charset currentCharset) {
		this.progName = progName;
		this.version = version;
		this.client = client;
		this.cwd = cwd;
		this.host = host;
		this.port = port;
		this.language = language;
		this.os = os;
		this.user = user;
		this.unicode = unicode;
		this.currentCharset = currentCharset;
	}

	public void marshal(ByteBuffer buf) throws BufferOverflowException {
		
		if (buf == null) {
			throw new NullPointerError("Null ByteBuffer passed to P4JRpcEnv.marshal()");
		}
		String charsetName = currentCharset == null ? RpcConnection.NON_UNICODE_SERVER_CHARSET_NAME
														: (unicode ? CharsetDefs.UTF8_NAME : currentCharset.name());
		try {
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_PROGRAM, progName.getBytes(CharsetDefs.UTF8.name()));
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_VERSION, version.getBytes(CharsetDefs.UTF8.name()));
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_CLIENTNAME, client.getBytes(charsetName));
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_CWD, cwd.getBytes(CharsetDefs.UTF8.name()));
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_HOSTNAME, host.getBytes(CharsetDefs.UTF8.name()));
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_PORT, port.getBytes(CharsetDefs.UTF8.name()));
			
			if (language != null) {
				RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_LANGUAGE, language.getBytes(CharsetDefs.UTF8.name()));
			}
			
			if (unicode) {
				RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_UNICODE, "".getBytes(CharsetDefs.UTF8.name()));
			}
			
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_OSNAME, os.getBytes(CharsetDefs.UTF8.name()));
			RpcPacketField.marshal(buf, RPC_ARGNAME_ENV_USERNAME, user.getBytes(CharsetDefs.UTF8.name()));
		} catch (UnsupportedEncodingException exc) {
			Log.exception(exc);
			throw new ProtocolError("rpc marshaling error: unsupported encoding: " + exc.getMessage());
		}
	}
	
	public byte[] marshal() {
		// FIXME: reimplement and refactor elsewhere properly when we're sure this is working -- HR.
		
		ByteBuffer byteBuf = ByteBuffer.allocate(10240);	// FIXME!!!! -- HR.
		
		marshal(byteBuf);
		byteBuf.flip();
		
		int envLength = byteBuf.limit();
		byte[] envBytes = new byte[envLength];
		byteBuf.get(envBytes);
		
		return envBytes;
	}
	
	public String toString() {
		return RPC_ARGNAME_ENV_PROGRAM + ": " + Server.guardNull(progName) + "; "
				+ RPC_ARGNAME_ENV_VERSION + ": " + Server.guardNull(version) + "; "
				+ RPC_ARGNAME_ENV_CLIENTNAME + ": " + Server.guardNull(client) + "; "
				+ RPC_ARGNAME_ENV_CWD + ": " + Server.guardNull(cwd) + "; "
				+ RPC_ARGNAME_ENV_HOSTNAME + ": " + Server.guardNull(host) + "; "
				+ RPC_ARGNAME_ENV_PORT + ": " + Server.guardNull(port) + "; "
				+ RPC_ARGNAME_ENV_LANGUAGE + ": " + Server.guardNull(language) + "; "
				+ RPC_ARGNAME_ENV_OSNAME + ": " + Server.guardNull(os) + "; "
				+ RPC_ARGNAME_ENV_USERNAME + ": " + Server.guardNull(user);
	}

	public String getProgName() {
		return this.progName;
	}

	public void setProgName(String progName) {
		this.progName = progName;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getClient() {
		return this.client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getCwd() {
		return this.cwd;
	}

	public void setCwd(String cwd) {
		this.cwd = cwd;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return this.port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getOs() {
		return this.os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public boolean isUnicode() {
		return this.unicode;
	}

	public void setUnicode(boolean unicode) {
		this.unicode = unicode;
	}

	public Charset getCurrentCharset() {
		return this.currentCharset;
	}

	public void setCurrentCharset(Charset currentCharset) {
		this.currentCharset = currentCharset;
	}
}
