/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func;

/**
 * Defines some simple metadata about Perforce RPC functions.
 * 
 *
 */

public class RpcFunctionMetadata {
	
	RpcFunctionSpec name = RpcFunctionSpec.NONE;
	RpcFunctionType type = RpcFunctionType.NONE;
	String encoding = null;
	
	public RpcFunctionMetadata() {
	}
	
	public RpcFunctionMetadata(RpcFunctionSpec name,
				RpcFunctionType type, String encoding) {
		this.name = name;
		this.type = type;
		this.encoding = encoding;
	}

	public RpcFunctionSpec getName() {
		return this.name;
	}

	public void setName(RpcFunctionSpec name) {
		this.name = name;
	}

	public RpcFunctionType getType() {
		return this.type;
	}

	public void setType(RpcFunctionType type) {
		this.type = type;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
};
