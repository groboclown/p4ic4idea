/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;

/**
 * Provides a Perforce-specific extension to the basic Java
 * InputStream to allow us to intercept methods and implement
 * our own extensions.<p>
 * 
 * The current main use is for line-end processing with the
 * RpcLineEndFilterInputStream filter class; more uses
 * will probably follow with experience....
 */

public class RpcInputStream extends FileInputStream {
	
	private RpcPerforceFile file = null;
	private RpcPerforceFileType fileType = RpcPerforceFileType.FST_TEXT;
	private RpcLineEndFilterInputStream lineEndStream = null;
	private ClientLineEnding lineEnding = null;

	public RpcInputStream(RpcPerforceFile file) throws IOException {
		super(file);
		if (file == null) {
			throw new NullPointerError(
				"Null RpcPerforceFile passed to RpcInputStream constructor");
		}
		
		this.file = file;
		this.fileType = this.file.getFileType();
		this.lineEnding = this.file.getLineEnding();
		
		if (this.lineEnding == null) {
			this.lineEnding = ClientLineEnding.FST_L_LOCAL;
		}
		
		if (this.fileType == null) {
			this.fileType = RpcPerforceFileType.FST_TEXT;
		}
		
		switch (this.fileType) {
			case FST_TEXT:
			case FST_UNICODE:
			case FST_UTF16:
			case FST_XTEXT:
				if (ClientLineEnding.needsLineEndFiltering(
														this.lineEnding)) {
					this.lineEndStream = new RpcLineEndFilterInputStream(
							new BufferedInputStream(new FileInputStream(this.file)), this.lineEnding);
				}
				
				break;
		}
	}

	@Override
	public void close() throws IOException {
		switch (this.fileType) {
			case FST_TEXT:
			case FST_UNICODE:
			case FST_UTF16:
			case FST_XTEXT:
				if (this.lineEndStream != null) {
					this.lineEndStream.close();
				}
				break;
		}
		
		super.close();
	}
	
	@Override
	public int read() throws IOException {
		throw new UnimplementedError("RpcInputStream.read()");
	}

	@Override
	public int read(byte[] targetBytes, int targetOffset, int targetLen) throws IOException {
		if (targetBytes == null) {
			throw new NullPointerError("Null target byte array in RpcInputStream.read()");
		}
		if (targetOffset < 0) {
			throw new P4JavaError("Negative target offset in RpcInputStream.read()");
		}
		if (targetLen < 0) {
			throw new P4JavaError("Negative target length in RpcInputStream.read()");
		}
		
		switch (this.fileType) {
			case FST_TEXT:
			case FST_UNICODE:
			case FST_UTF16:
			case FST_XTEXT:
				if (this.lineEndStream != null) {
					return this.lineEndStream.read(
									targetBytes, targetOffset, targetLen);
				}
				
				break;
		}
		
		return super.read(targetBytes, targetOffset, targetLen);
	}

	@Override
	public int read(byte[] targetBytes) throws IOException {
		return this.read(targetBytes, 0, targetBytes.length);
	}
}
