/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;

/**
 * Filter input stream to do Perforce-specific line end
 * munging where necessary. This is a lot more complicated that
 * you might think, given the potential for cross-packet and cross-read
 * multibyte end of line spanning.<p>
 * 
 * FIXME: implement "share" mode -- HR.
 * FIXME: unicode version -- HR.
 */

public class RpcLineEndFilterInputStream extends FilterInputStream {

	private ClientLineEnding lineEnding = null;
	private static byte[] localLineEndBytes
						= ClientLineEnding.FST_L_LOCAL_BYTES;
	
	private byte[] inBytes = new byte[RpcPropertyDefs.RPC_DEFAULT_FILE_BUF_SIZE];
	private int inBytesPos = 0;
	private int inBytesRead = 0;
	private int matchPos = 0;
	private byte[] inLineEnd = null;
	
	public RpcLineEndFilterInputStream(InputStream inStream,
									ClientLineEnding lineEnding) {
		super(inStream);
		this.lineEnding = lineEnding;
		if (this.lineEnding == null) {
			this.lineEnding = ClientLineEnding.FST_L_LOCAL;
		}

		switch (this.lineEnding) {
			case FST_L_LF:
				this.inLineEnd = ClientLineEnding.FST_L_LF_BYTES;
				break;
				
			case FST_L_CR:
				this.inLineEnd = ClientLineEnding.FST_L_CR_BYTES;
				break;
				
			case FST_L_CRLF:
				this.inLineEnd = ClientLineEnding.FST_L_CRLF_BYTES;
				break;
				
			case FST_L_LFCRLF:
				this.inLineEnd = ClientLineEnding.FST_L_LFCRLF_BYTES;
				break;
				
			default:
				this.inLineEnd = localLineEndBytes;
				break;
		}
	}

	@Override
	public int read() throws IOException {
		throw new UnimplementedError(
				"RpcLineEndFilterInputStream.read()");
	}

	/**
	 * Read from the associated input stream looking for end of line
	 * strings to replace with the Perforce server newline character.
	 */
	@Override
	public int read(byte[] targetBytes, int off, int len) throws IOException {

		int bytesOut = 0;
		int outPos = off;
		
		while (bytesOut < len) {
			if (inBytesPos == inBytesRead) {
				inBytesRead = this.in.read(inBytes);
				inBytesPos = 0;
			}
			
			if (inBytesRead <= 0) {
				break;
			}
			
			if (inBytes[inBytesPos] == this.inLineEnd[matchPos]) {
				inBytesPos++;
				matchPos++;
				if (matchPos >= this.inLineEnd.length) {
					targetBytes[outPos++] =
							ClientLineEnding.PERFORCE_SERVER_LINE_END_BYTE;
					bytesOut++;
					matchPos = 0;
				}
			} else {
				if (matchPos != 0) {
					for (int i = 0; i < matchPos; i++) {
						targetBytes[outPos++] = this.inLineEnd[i];
						bytesOut++;
					}
					matchPos = 0;
				} else {
					matchPos = 0;
					targetBytes[outPos++] = inBytes[inBytesPos++];
					bytesOut++;
				}
			}
		}
		
		return (bytesOut == 0 ? -1 : bytesOut);
	}

	/**
	 * Convenience method; equivalent to read(b, 0, b.length).
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int available() throws IOException {
		throw new UnimplementedError(
		"RpcLineEndFilterInputStream.available()");
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new UnimplementedError(
		"RpcLineEndFilterInputStream.mark()");
	}

	@Override
	public boolean markSupported() {
		throw new UnimplementedError(
		"RpcLineEndFilterInputStream.read()");
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new UnimplementedError(
		"RpcLineEndFilterInputStream.markSupported()");
	}

	@Override
	public long skip(long n) throws IOException {
		throw new UnimplementedError(
		"RpcLineEndFilterInputStream.skip()");
	}
}
