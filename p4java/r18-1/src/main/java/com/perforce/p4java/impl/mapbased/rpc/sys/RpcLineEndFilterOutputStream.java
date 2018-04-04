/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;

/**
 * Simple FilterOutputStream extension to deal with Perforce text file
 * line end translation.
 */

public class RpcLineEndFilterOutputStream  extends FilterOutputStream {
	
	/**
	 * What the Perforce server uses as a line end separator when sending
	 * us text file contents
	 */
	public static final String P4SERVER_LINSEP_STR = "\n";
	public static final byte P4SERVER_LINSEP_BYTE = '\n';
	
	private static final byte CR_BYTE = '\r';
	
	private ClientLineEnding lineEnding = null;
	private byte[] localNewlineBytes
					= ClientLineEnding.FST_L_LOCAL_BYTES;
	
	private byte[] outBytes = null;
	
	public RpcLineEndFilterOutputStream(OutputStream out,
							ClientLineEnding lineEnding) {
		super(out);
		this.lineEnding = lineEnding;
		this.outBytes = new byte[RpcPropertyDefs.RPC_DEFAULT_FILE_BUF_SIZE];
		
		if (this.lineEnding == null) {
			throw new NullPointerError(
				"null line ending spec in RpcLineEndFilterOutputStream constructor");
		}
		if (this.localNewlineBytes == null) {
			throw new NullPointerError(
			"null local line ending bytes in RpcLineEndFilterOutputStream constructor");
		}
	}
	
	public void write(OutputStream out,byte[] bytes, int off, int len) throws IOException {
		if (bytes == null) {
			throw new NullPointerError(
					"Null byte array passed to RpcLineEndFilterOutputStream.write()");
		}
		
		if (off < 0) {
			throw new P4JavaError(
					"Negative byte array offset in RpcLineEndFilterOutputStream.write()");
		}
		
		if (len < 0) {
			throw new P4JavaError(
					"Negative byte array length in RpcLineEndFilterOutputStream.write()");
		}
		
		if ((off + len) > bytes.length) {
			throw new P4JavaError(
					"(off + len) > bytes.length in RpcLineEndFilterOutputStream.write()");
		}
		
		int outPos = 0;
		int inPos = off;
		byte b = 0;
		
		if (this.outBytes.length < bytes.length) {
			this.outBytes = new byte[bytes.length];
		}
		
		for (int bytesIn = 0; bytesIn < len; bytesIn++) {
			
			b = bytes[inPos++];
			
			if (b == P4SERVER_LINSEP_BYTE) {
				switch (lineEnding) {
					case FST_L_LOCAL:
						for (byte nb : this.localNewlineBytes) {
							
							if (outPos >= outBytes.length) {
								out.write(outBytes, 0, outPos);
								outPos = 0;
							}
							outBytes[outPos++] = nb;
						}
						break;
						
					case FST_L_CRLF:
						
						if (outPos >= outBytes.length) {
							out.write(outBytes, 0, outPos);
							outPos = 0;
						}
						outBytes[outPos++] = CR_BYTE;
						
						if (outPos >= outBytes.length) {
							out.write(outBytes, 0, outPos);
							outPos = 0;
						}
						outBytes[outPos++] = b;
						break;
						
					case FST_L_CR:
						if (outPos >= outBytes.length) {
							out.write(outBytes, 0, outPos);
							outPos = 0;
						}
						outBytes[outPos++] = CR_BYTE;
						break;
						
					default:
						
						if (outPos >= outBytes.length) {
							out.write(outBytes, 0, outPos);
							outPos = 0;
						}
						outBytes[outPos++] = b;
						break;
				}
			} else {
				
				if (outPos >= outBytes.length) {
					out.write(outBytes, 0, outPos);
					outPos = 0;
				}
				
				outBytes[outPos++] = b;
			}
		}
		
		if (outPos > 0) {
			out.write(outBytes, 0, outPos);
		}
	}
	
	public void write(byte[] bytes, int off, int len) throws IOException {
		write(this.out,bytes, off, len);
	}
	
	public void write(byte[] bytes) throws IOException {
		if (bytes == null) {
			throw new NullPointerError(
					"Null byte array passed to RpcLineEndFilterOutputStream.write()");
		}
		this.write(bytes, 0, bytes.length);
	}
}
