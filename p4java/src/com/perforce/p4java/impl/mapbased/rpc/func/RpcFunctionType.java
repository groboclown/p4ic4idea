package com.perforce.p4java.impl.mapbased.rpc.func;

import java.util.Locale;

/**
 * How we classify a Perforce RPC function for dispatch, encoding,
 * and other processing.
 */

public enum RpcFunctionType {
	
	/**
	 * "Simple" client functions, e.g. "client-Message" to be
	 * processed by the client and that do not (generally...)
	 * require further interaction with the server.
	 */
	CLIENT,
	
	/**
	 * More complex client functions that will probably cause
	 * duplex back-and-forth messaging between client and server.
	 */
	CLIENT_DM,
	
	/**
	 * The normal end-user-initiated perforce functions, e.g.
	 * "info" or "depots".
	 */
	USER,
	
	/**
	 * Server functions to be processed by the Perforce server.
	 */
	SERVER,
	
	/**
	 * Protocol meta functions, e.g. "flush1" or "compress2".
	 */
	PROTOCOL,
	
	/**
	 * Non-function; mainly serves as an error or guard.
	 */
	NONE;

	
	/**
	 * Get the string prefix used by this function type when encoded
	 * for the wire. E.g. "dm-" for CLIENT_DM, or "user" for USER.
	 * The protocol type has no prefix (at least none that I've
	 * seen).
	 */
	
	public String getEncodingPrefix() {
		
		switch (this) {
			case CLIENT_DM:
				return "dm-";
				
			case PROTOCOL:
				return "";
				
			default:
				return this.toString().toLowerCase(Locale.ENGLISH) + "-";
		}
	}
	
	/**
	 * Return the name prefix (e.g. "USER_") for this func spec.
	 */
	
	public String getTypeNamePrefix() {
		switch (this) {
			case CLIENT_DM:
				return "CLIENT_DM_";
				
			default:
				return this.toString() + "_";
		}
	}
};

