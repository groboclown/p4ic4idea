/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet;

import com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey;

/**
 * Defines the various Perforce RPC packet field types and associated
 * methods. Mainly used to determine whether to treat a packet field value
 * as a string or bytes; most fields are strings, but things like worKRec
 * or depotRec fields are binary pass-throughs from the point of view of
 * the client (they're state keys and / or state records used by the server).
 * 
 *
 */

public enum RpcPacketFieldType {
	
	/**
	 * Field type is unknown. Field should probably not be used.
	 */
	NONE,
	
	/**
	 * Field is text; possibly UTF-8 encoded.
	 */
	TEXT,
	
	/**
	 * Field is bytes, not interpreted.
	 */
	BINARY;
	
	/**
	 * Return the field type associated with the passed-in name, if any.
	 */
	
	public static RpcPacketFieldType getFieldType(String fieldName) {
		
		if (fieldName != null) {
			if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.DATA)) {
				return BINARY;
			} else if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.WORKREC)) {
				return BINARY;
			} else if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.WORKREC2)) {
				return BINARY;
			} else if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.DEPOTREC)) {
				return BINARY;
			} else if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.INTEGREC)) {
				return BINARY;
			} else if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.BASEDEPOTREC)) {
				return BINARY;
			} else if (fieldName.equalsIgnoreCase(RpcFunctionMapKey.HAVEREC)) {
			    return BINARY;
			} else if (fieldName.startsWith(RpcFunctionMapKey.ATTR_PREFIX)){
				return BINARY;
			} else {
				return TEXT;
			}
		}
		
		return NONE;
	}
}
