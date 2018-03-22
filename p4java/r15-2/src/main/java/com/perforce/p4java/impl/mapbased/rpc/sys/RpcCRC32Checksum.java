/**
 * Copyright (c) 2012 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Provides a wrapper to the basic Java CRC32 to allow us to use the JZlib pure
 * Java implementation of the CRC32 checksum from RFC1952.<p>
 * 
 * This avoids the basic Java CRC32's JNI overhead for certain uses of
 * checksumming where many small pieces of data are checksummed in succession.
 */
public class RpcCRC32Checksum extends CRC32 {

    private com.jcraft.jzlib.CRC32 jcrc32;
    
    /**
     * Creates a new RpcCRC32Checksum object.
     */
    public RpcCRC32Checksum() {
    	super();
    	jcrc32 = new com.jcraft.jzlib.CRC32();
    }

    /**
     * Updates the Rpc CRC-32 checksum with the specified byte (the low
     * eight bits of the argument b).
     *
     * @param b the byte to update the checksum with
     */
    public void update(int b) {
    	ByteBuffer buf = ByteBuffer.allocate(4);
    	buf.putInt(b);
    	byte[] ba = buf.array();
        this.jcrc32.update(ba, 0, ba.length);
    }

    /**
     * Updates the Rpc CRC-32 checksum with the specified array of bytes.
     */
    public void update(byte[] b, int off, int len) {
    	this.jcrc32.update(b, off, len);
    }

    /**
     * Updates the Rpc CRC-32 checksum with the specified array of bytes.
     *
     * @param b the array of bytes to update the checksum with
     */
    public void update(byte[] b) {
    	this.jcrc32.update(b, 0, b.length);
    }

    /**
     * Resets Rpc CRC-32 to initial value.
     */
    public void reset() {
    	this.jcrc32.reset();
    }

    /**
     * Returns CRC-32 value.
     */
    public long getValue() {
        return this.jcrc32.getValue();
    }
}
