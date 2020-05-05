/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.func.client;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.Log;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.exception.FileEncoderException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.mapbased.rpc.connection.RpcConnection;
import com.perforce.p4java.impl.mapbased.rpc.func.client.ClientMerge.ResolveChoice;
import com.perforce.p4java.impl.mapbased.rpc.func.helper.MD5Digester;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcOutputStream;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Helper class for carrying useful merge state around during the various merge
 * operations defined in ClientMerge. Modeled somewhat on the C++ API's
 * clientmerge3.cc object, but tuned more to our more limited purposes.
 * Also includes support for two-way merge, but this is currently less-well
 * exercised and tested.<p>
 * <p>
 * Note: not particularly thread-safe, nor intended to be.
 */

public class ClientMergeState {

	public static final String TRACE_PREFIX = "ClientMergeState";

	public static final String DEFAULT_TMPFILE_PFX = "p4j";
	public static final String DEFAULT_TMPFILE_SFX = ".mrg";

	/**
	 * True if merging is being done from an external stream, e.g.
	 * with the resolveFile() method rather than resolveFilesAuto or whatever
	 */
	private boolean externalStreamMerge = false;

	/**
	 * Only used if externalStreamMerge is true.
	 */
	private String externalTmpFilename = null;

	private String tmpDir = null;

	private String clientPath = null;
	private String baseName = null;
	private String theirName = null;
	private String yourName = null;

	private String baseTmpFilename = null;
	private String theirTmpFilename = null;
	private String yourTmpFilename = null;
	private String resultTmpFilename = null;

	private RpcPerforceFile baseTmpFile = null;
	private RpcPerforceFile theirTmpFile = null;
	private RpcPerforceFile yourTmpFile = null;
	private RpcPerforceFile resultTmpFile = null;

	private RpcOutputStream baseTmpFileStream = null;
	private RpcOutputStream yourTmpFileStream = null;
	private RpcOutputStream theirTmpFileStream = null;
	private RpcOutputStream resultTmpFileStream = null;

	private MD5Digester theirDigester = null;
	private MD5Digester yourDigester = null;
	private MD5Digester resultDigester = null;

	private int yourChunks = 0;
	private int theirChunks = 0;
	private int conflictChunks = 0;
	private int bothChunks = 0;

	private int bits = 0;
	private int oldBits = 0;

	private boolean safeMerge = false;
	private boolean autoMerge = false;

	private RpcPerforceFileType clientType = null;
	private RpcPerforceFileType resultType = null;
	private ClientLineEnding clientLineEnding = null;
	private ClientLineEnding resultLineEnding = null;

	private Charset charset = null;

	private boolean showAll = false;

	protected boolean twoWayMerge = false; // Set true iff is a two-way merge
	protected String baseDigest = null;

	/**
	 * @param externalStreamMerge set true if this is a merge from an external stream
	 * @param tmpDir              the name of a suitable directory for creating temporary files in
	 */
	protected ClientMergeState(String clientPath, boolean externalStreamMerge,
	                           RpcPerforceFileType clientType, ClientLineEnding clientLineEnding, RpcPerforceFileType resultType, ClientLineEnding resultLineEnding,
	                           String tmpDir, Charset charset) {
		this.externalStreamMerge = externalStreamMerge;
		this.clientPath = clientPath;
		this.clientType = clientType;
		this.clientLineEnding = clientLineEnding;
		this.resultType = resultType;
		this.resultLineEnding = resultLineEnding;
		this.charset = charset;
		if (tmpDir == null) {
			throw new NullPointerError(
					"null tmpdir passed to ClientMergeState constructor");
		}
		this.tmpDir = tmpDir;
	}

	/**
	 * Open and / or create the necessary files for this merge. The "yours"
	 * file is the original client file, and doesn't need opening. The rest
	 * are opened as tmp files in the system tmp directory; this isn't exactly
	 * the same as the C++ API's behaviour (which opens them in the target directory)
	 * but it should be fairly safe.<p>
	 * <p>
	 * Note that the file types for each file are copied from the C++ API usage;
	 * I'm not entirely sure this arrangement always make sense...
	 *
	 * @throws IOException if there's been a problem opening any of the files.
	 */
	protected void openMergeFiles(RpcConnection rpcConnection) throws IOException {
		this.baseTmpFilename = RpcPerforceFile.createTempFileName(this.tmpDir);
		this.baseTmpFile = new RpcPerforceFile(this.baseTmpFilename, clientType, clientLineEnding);
		this.baseTmpFileStream = new RpcOutputStream(this.baseTmpFile, rpcConnection, false);

		this.theirTmpFilename = RpcPerforceFile.createTempFileName(this.tmpDir);
		this.theirTmpFile = new RpcPerforceFile(this.theirTmpFilename, resultType, resultLineEnding);
		this.theirTmpFileStream = new RpcOutputStream(this.theirTmpFile, rpcConnection, false);
		this.theirDigester = new MD5Digester();

		this.yourTmpFilename = this.clientPath;
		this.yourTmpFile = new RpcPerforceFile(this.yourTmpFilename, clientType);
		this.yourDigester = new MD5Digester();

		this.resultTmpFilename = RpcPerforceFile.createTempFileName(this.tmpDir);
		this.resultTmpFile = new RpcPerforceFile(this.resultTmpFilename, resultType, resultLineEnding);
		this.resultTmpFileStream = new RpcOutputStream(this.resultTmpFile, rpcConnection, false);
		this.resultDigester = new MD5Digester();
	}

	protected void writeMarker(String markerString) throws IOException, FileDecoderException, FileEncoderException {
		if (checkStream(resultTmpFileStream)) {
			// Convert the marker to UTF-8 since writeConverted assumes a UTF-8
			// to local charset conversion
			resultTmpFileStream.writeConverted(markerString.getBytes(CharsetDefs.UTF8_NAME));
		} else {
			throw new NullPointerError("bad stream in writeResultChunk");
		}
	}

	protected void writeBaseChunk(byte[] bytes) throws IOException, FileDecoderException, FileEncoderException {
		if (checkStream(baseTmpFileStream)) {
			baseTmpFileStream.writeConverted(bytes);
		} else {
			throw new NullPointerError("bad stream in writeBaseChunk");
		}
	}

	protected void writeTheirChunk(byte[] bytes) throws IOException, FileDecoderException, FileEncoderException {
		if (checkStream(theirTmpFileStream)) {
			theirDigester.update(bytes);
			theirTmpFileStream.writeConverted(bytes);
		} else {
			throw new NullPointerError("bad stream in writeTheirChunk");
		}
	}

	protected void writeYourChunk(byte[] bytes) throws IOException {
		yourDigester.update(bytes);
		// We don't need to write anything.
	}

	protected void writeResultChunk(byte[] bytes) throws IOException, FileDecoderException, FileEncoderException {
		if (checkStream(resultTmpFileStream)) {
			resultDigester.update(bytes);
			resultTmpFileStream.writeConverted(bytes);
		} else {
			throw new NullPointerError("bad stream in writeResultChunk");
		}
	}

	protected boolean finishMerge(ResolveChoice choice) throws IOException {

		boolean succeeded = false;

		try {
			if (checkStream(resultTmpFileStream)) {
				resultTmpFileStream.close();
			}
			if (checkStream(baseTmpFileStream)) {
				baseTmpFileStream.close();
			}
			if (checkStream(theirTmpFileStream)) {
				theirTmpFileStream.close();
			}

			// Move correct file to target, if needed;

			switch (choice) {

				case THEIRS:
					// Move theirs to yours...

					succeeded = this.theirTmpFile.renameTo(this.yourTmpFile);
					break;

				case MERGED:
					// Move result to yours...

					succeeded = this.resultTmpFile.renameTo(this.yourTmpFile);
					break;

				case EDIT:
					succeeded = this.resultTmpFile.renameTo(this.yourTmpFile);
					break;

				default:
					succeeded = true;
					break;
			}
		} finally {
			try {
				// Try not to delete the target file here...

				if (this.baseTmpFile != null) this.baseTmpFile.delete();
				if (this.theirTmpFile != null) this.theirTmpFile.delete();
				if (this.resultTmpFile != null) this.resultTmpFile.delete();
			} catch (Throwable thr) {
				Log.warn("unexpected exception in closeMerge: " + thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return succeeded;
	}

	protected String getMergeDigestString() {
		// If the file has conflicts, do not report merge digest, otherwise
		// return result digest:

		if (conflictChunks == 0) {
			return resultDigester.digestAs32ByteHex();
		}

		return null;
	}

	protected String getTheirDigestString() {
		return theirDigester.digestAs32ByteHex();
	}

	protected String getYourDigestString() {
		if (isTwoWayMerge()) {
			return new MD5Digester().digestFileAs32ByteHex(yourTmpFile, null);
		}
		return yourDigester.digestAs32ByteHex();
	}

	protected int incrYourChunks() {
		return ++yourChunks;
	}

	protected int theirYourChunks() {
		return ++theirChunks;
	}

	protected int incrConflictChunks() {
		return ++conflictChunks;
	}

	protected int incrTheirChunks() {
		return ++theirChunks;
	}

	protected int incrBothChunks() {
		return ++bothChunks;
	}

	protected String getClientPath() {
		return this.clientPath;
	}

	protected void setClientPath(String clientPath) {
		this.clientPath = clientPath;
	}

	protected String getBaseName() {
		return this.baseName;
	}

	protected void setBaseName(String baseName) {
		this.baseName = baseName;
	}

	protected String getTheirName() {
		return this.theirName;
	}

	protected void setTheirName(String theirName) {
		this.theirName = theirName;
	}

	protected String getYourName() {
		return this.yourName;
	}

	protected void setYourName(String yourName) {
		this.yourName = yourName;
	}

	protected String getBaseTmpFilename() {
		return this.baseTmpFilename;
	}

	protected void setBaseTmpFilename(String baseTmpFilename) {
		this.baseTmpFilename = baseTmpFilename;
	}

	protected String getTheirTmpFilename() {
		return this.theirTmpFilename;
	}

	protected void setTheirTmpFilename(String theirTmpFilename) {
		this.theirTmpFilename = theirTmpFilename;
	}

	protected String getYourTmpFilename() {
		return this.yourTmpFilename;
	}

	protected void setYourTmpFilename(String yourTmpFilename) {
		this.yourTmpFilename = yourTmpFilename;
	}

	protected RpcPerforceFile getBaseTmpFile() {
		return this.baseTmpFile;
	}

	protected void setBaseTmpFile(RpcPerforceFile baseTmpFile) {
		this.baseTmpFile = baseTmpFile;
	}

	protected RpcPerforceFile getTheirTmpFile() {
		return this.theirTmpFile;
	}

	protected void setTheirTmpFile(RpcPerforceFile theirTmpFile) {
		this.theirTmpFile = theirTmpFile;
	}

	protected RpcPerforceFile getYourTmpFile() {
		return this.yourTmpFile;
	}

	protected void setYourTmpFile(RpcPerforceFile yourTmpFile) {
		this.yourTmpFile = yourTmpFile;
	}

	protected RpcOutputStream getBaseTmpFileStream() {
		return this.baseTmpFileStream;
	}

	protected void setBaseTmpFileStream(RpcOutputStream baseTmpFileStream) {
		this.baseTmpFileStream = baseTmpFileStream;
	}

	protected RpcOutputStream getYourTmpFileStream() {
		return this.yourTmpFileStream;
	}

	protected void setYourTmpFileStream(RpcOutputStream yourTmpFileStream) {
		this.yourTmpFileStream = yourTmpFileStream;
	}

	protected RpcOutputStream getTheirTmpFileStream() {
		return this.theirTmpFileStream;
	}

	protected void setTheirTmpFileStream(RpcOutputStream theirTmpFileStream) {
		this.theirTmpFileStream = theirTmpFileStream;
	}

	protected int getYourChunks() {
		return this.yourChunks;
	}

	protected void setYourChunks(int yourChunks) {
		this.yourChunks = yourChunks;
	}

	protected int getTheirChunks() {
		return this.theirChunks;
	}

	protected void setTheirChunks(int theirChunks) {
		this.theirChunks = theirChunks;
	}

	protected int getConflictChunks() {
		return this.conflictChunks;
	}

	protected void setConflictChunks(int conflictChunks) {
		this.conflictChunks = conflictChunks;
	}

	protected int getBothChunks() {
		return this.bothChunks;
	}

	protected void setBothChunks(int bothChunks) {
		this.bothChunks = bothChunks;
	}

	protected int getBits() {
		return this.bits;
	}

	protected void setBits(int bits) {
		this.bits = bits;
	}

	protected int getOldBits() {
		return this.oldBits;
	}

	protected void setOldBits(int oldBits) {
		this.oldBits = oldBits;
	}

	protected String getTmpDir() {
		return this.tmpDir;
	}

	protected void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	protected boolean isExternalStreamMerge() {
		return this.externalStreamMerge;
	}

	protected void setExternalStreamMerge(boolean externalStreamMerge) {
		this.externalStreamMerge = externalStreamMerge;
	}

	protected String getExternalTmpFilename() {
		return this.externalTmpFilename;
	}

	protected void setExternalTmpFilename(String externalTmpFilename) {
		this.externalTmpFilename = externalTmpFilename;
	}

	protected RpcPerforceFile getResultTmpFile() {
		return this.resultTmpFile;
	}

	protected void setResultTmpFile(RpcPerforceFile resultTmpFile) {
		this.resultTmpFile = resultTmpFile;
	}

	protected RpcOutputStream getResultTmpFileStream() {
		return this.resultTmpFileStream;
	}

	protected void setResultTmpFileStream(RpcOutputStream resultTmpFileStream) {
		this.resultTmpFileStream = resultTmpFileStream;
	}

	private boolean checkStream(RpcOutputStream stream) {
		try {
			if (stream != null) {
				FileDescriptor fd = stream.getFD();
				if (fd != null) {
					if (fd.valid()) {
						return true;
					}
				}
			}
		} catch (IOException ioexc) {
			// Ignore for now other than to log it...
			Log.exception(ioexc);
		}
		return false;
	}

	protected boolean isSafeMerge() {
		return this.safeMerge;
	}

	protected void setSafeMerge(boolean safeMerge) {
		this.safeMerge = safeMerge;
	}

	protected boolean isAutoMerge() {
		return this.autoMerge;
	}

	protected void setAutoMerge(boolean autoMerge) {
		this.autoMerge = autoMerge;
	}

	protected boolean isShowAll() {
		return this.showAll;
	}

	protected void setShowAll(boolean showAll) {
		this.showAll = showAll;
	}

	protected boolean isTwoWayMerge() {
		return twoWayMerge;
	}

	protected void setTwoWayMerge(boolean twoWayMerge) {
		this.twoWayMerge = twoWayMerge;
	}

	protected String getBaseDigest() {
		return baseDigest;
	}

	protected void setBaseDigest(String baseDigest) {
		this.baseDigest = baseDigest;
	}
}
