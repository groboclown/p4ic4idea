/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.packet;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Defines the methods required to marshal an arbitrary object
 * onto a ByteBuffer for the Perforce RPC connection.
 * 
 *
 */

public interface RpcMarshalable {
	
	/**
	 * Marshal the associated object onto the passed-in ByteBuffer.
	 * 
	 * @param buf non-null ByteBuffer; should be big enough to take the marshaled object
	 * @throws BufferOverflowException if the passed-in buffer wasn't big enough
	 */
	
	void marshal(ByteBuffer buf) throws BufferOverflowException;
	
}
