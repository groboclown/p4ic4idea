/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;

/**
 * Filter input stream to do Perforce-specific line end
 * munging where necessary. This is a lot more complicated that
 * you might think, given the potential for cross-packet and cross-read
 * multibyte end of line spanning.
 * 
 * This should always be placed after a charset converter pipe.
 * Double byte charsets are not allowed here!
 */

public class RpcLineEndFilterInputStream extends FilterInputStream {

	private ClientLineEnding lineEnding = null;
	
	private byte[] inBytes = new byte[RpcPropertyDefs.RPC_DEFAULT_FILE_BUF_SIZE];
	private int inBytesPos = 0;
	private int inBytesRead = 0;
	
	public RpcLineEndFilterInputStream(InputStream inStream,
									ClientLineEnding lineEnding) {
		super(inStream);
		this.lineEnding = lineEnding;
		if (this.lineEnding == null) {
			this.lineEnding = ClientLineEnding.FST_L_LOCAL;
		}

		switch (this.lineEnding) {
			case FST_L_LF:
			case FST_L_CR:
			case FST_L_CRLF:				
			case FST_L_LFCRLF:
				break;

			case FST_L_LOCAL:
				if (Arrays.equals(ClientLineEnding.FST_L_LOCAL_BYTES,
								  ClientLineEnding.FST_L_LF_BYTES)) {
					this.lineEnding = ClientLineEnding.FST_L_LF;
				} else if (Arrays.equals(ClientLineEnding.FST_L_LOCAL_BYTES,
						                 ClientLineEnding.FST_L_CR_BYTES)) {
					this.lineEnding = ClientLineEnding.FST_L_CR;
				} else if (Arrays.equals(ClientLineEnding.FST_L_LOCAL_BYTES,
							             ClientLineEnding.FST_L_CRLF_BYTES)) {
					this.lineEnding = ClientLineEnding.FST_L_CRLF;
				} else {
					this.lineEnding = ClientLineEnding.FST_L_LF;
				}
				break;
				
		case FST_L_CRLF_UTF_16BE:
		case FST_L_CRLF_UTF_16LE:
		case FST_L_CR_UTF_16BE:
		case FST_L_CR_UTF_16LE:
		case FST_L_LFCRLF_UTF_16BE:
		case FST_L_LFCRLF_UTF_16LE:
		case FST_L_LF_UTF_16BE:
		case FST_L_LF_UTF_16LE:
			throw new IllegalArgumentException("UTF16 type line ending not allowed here!");
				
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

		// Read logic: read whole lines that end in \r, and
		// arrange so that a following \n translates the \r
		// into a \n and the \n is dropped.

		int bytesOut = 0;
		int outPos = off;
		
		// soaknl: we saw a \r, skip this \n
		boolean soaknl = false;
		
		while (bytesOut < len || soaknl) {
			if (inBytesPos == inBytesRead) {
				inBytesRead = this.in.read(inBytes);
				inBytesPos = 0;
			}
			
			if (inBytesRead <= 0) {
				break;
			}


		    // Skipping \n because we saw a \r?

		    if( soaknl ) {
				if( inBytes[inBytesPos] == '\n' ) {
					inBytesPos++;
				    targetBytes[outPos-1] = '\n';
				}
				soaknl = false;
		    }
			
			switch( this.lineEnding ) {
		    case FST_L_LF:
				// Straight copy.
		    	while (bytesOut < len && inBytesPos != inBytesRead) {
			    	targetBytes[outPos++] = inBytes[inBytesPos++];
					bytesOut++;
		    	}
				break;
				
		    case FST_L_CR:
				// Copy to the next \r.  If we hit one, translate
				// it to \n.
		    	while (bytesOut < len && inBytesPos != inBytesRead) {
			    	targetBytes[outPos++] = inBytes[inBytesPos++];
					bytesOut++;
					if (targetBytes[outPos-1] == '\r') {
						targetBytes[outPos-1] = '\n';
						break;
					}
		    	}
				break;
		    	
		    case FST_L_CRLF:
				// Copy to next \r.  If we hit one, arrange so that
				// if we see \n the next time through (when we know
				// there'll be data in the buffer), that \n will
		    	// replace the \r in the output.

		    	while (bytesOut < len && inBytesPos != inBytesRead) {
			    	targetBytes[outPos++] = inBytes[inBytesPos++];
					bytesOut++;
					if (targetBytes[outPos-1] == '\r') {
						soaknl = true;
						break;
					}
		    	}
				break;
				

		    case FST_L_LFCRLF:
				// Copy to next \r.  If we hit one, translate it into
		    	// a \n, but arrange so that if we see \n the next time
		    	// through (when we know there'll be data in the buffer),
		    	// drop the subsequent \n.
				// LFCRLF reads CRLF.
		    	
		    	while (bytesOut < len && inBytesPos != inBytesRead) {
			    	targetBytes[outPos++] = inBytes[inBytesPos++];
					bytesOut++;
					if (targetBytes[outPos-1] == '\r') {
						targetBytes[outPos-1] = '\n';
						soaknl = true;
						break;
					}
				}
				break;
				
		    default:
		    	throw new IllegalStateException("Translating undefined line ending...");
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
