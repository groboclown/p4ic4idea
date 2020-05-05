/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.impl.generic.client.ClientLineEnding;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SymbolicLinkHelper;
import com.perforce.p4java.impl.mapbased.rpc.sys.helper.SysFileHelperBridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Encapsulates and implements a lot of Perforce-specific information
 * and operations on Perforce client-side files by extending the basic
 * java.io.File class with Perforce-specific fields and methods.
 */

public class RpcPerforceFile extends File {

	public static String TRACE_PREFIX = "RpcPerforceFile";

	public static String systemTmpDirName = null;

	public static final String TMP_FILE_PFX = "p4j";
	public static final String TMP_FILE_SFX = ".tmp";

	private static final long serialVersionUID = 1L;

	private RpcPerforceFileType fileType = null;
	private ClientLineEnding lineEnding = null;

	public static String createTempFileName(String tmpDirName) {

		// Kinda cheating, really...

		File tmpDir = null;

		try {
			if (tmpDirName != null) {
				tmpDir = new File(tmpDirName);
			}

			File tmpFile = File.createTempFile(TMP_FILE_PFX, TMP_FILE_SFX,
					tmpDir);

			return tmpFile.getPath();
		} catch (IOException ioexc) {
			Log.error(
					"Unable to create temporary file: " + ioexc.getLocalizedMessage());
		}

		return null;
	}

	public RpcPerforceFile(String fileName, String fileTypeStr) {
		super(fileName);
		if (fileName == null) {
			// We don't need or want (much less expect) null paths in the API:
			throw new NullPointerError(
					"Null file name passed to RpcPerforceFile constructor");
		}

		this.fileType = RpcPerforceFileType.decodeFromServerString(fileTypeStr);
		this.lineEnding = ClientLineEnding.decodeFromServerString(
				fileTypeStr, this.fileType);
	}

	public RpcPerforceFile(String fileName, RpcPerforceFileType fileType) {
		super(fileName);
		if (fileName == null) {
			// We don't need or want (much less expect) null paths in the API:
			throw new NullPointerError(
					"Null file name passed to RpcPerforceFile constructor");
		}

		this.fileType = fileType;
		this.lineEnding = ClientLineEnding.FST_L_LOCAL;
	}

	public RpcPerforceFile(String fileName, RpcPerforceFileType fileType,
	                       ClientLineEnding lineEnding) {
		super(fileName);
		if (fileName == null) {
			// We don't need or want (much less expect) null paths in the API:
			throw new NullPointerError(
					"Null file name passed to RpcPerforceFile constructor");
		}

		this.fileType = fileType;
		this.lineEnding = lineEnding;
	}

	/**
	 * Our "special" version of rename, intended to cope with the
	 * cases when the normal rename won't work (typically cross-device
	 * renames) or when we need to do some under-the-covers stitching
	 * up (for example, GKZIP stream decoding).
	 */

	public boolean renameTo(File targetFile) {
		return renameTo(targetFile, false);
	}

	/**
	 * Another special version of renameTo to support RPC implementation-
	 * specific needs. This one allows callers to specify whether to
	 * always copy as-is (no munging).
	 */
	public boolean renameTo(File targetFile, boolean alwaysCopyUnMunged) {
		if (targetFile == null) {
			throw new NullPointerError(
					"Null target file in RpcPerforceFile.renameTo");
		}
		try {
			if ((this.fileType == null) || alwaysCopyUnMunged || canCopyAsIs()) {
				if (super.renameTo(targetFile)) {
					return true;
				}
				// try again, but delete the target first (Windows)
				targetFile.delete();
				if (super.renameTo(targetFile)) {
					return true;
				} else {
					return copyTo(targetFile);
				}
			} else {
				// !canCopyAsIs...
				return decodeTo(targetFile);
			}
		} catch (IOException ioexc) {
			Log.error("Unexpected problem with renaming / copying file '"
					+ targetFile.getName() + "': " + ioexc.getLocalizedMessage());
			Log.exception(ioexc);
		}
		return false;
	}

	private boolean setWritable(String filePath) {
		boolean writable = false;
		ISystemFileCommandsHelper helper = SysFileHelperBridge.getSysFileCommands();
		if (helper != null) {
			writable = helper.setWritable(filePath, true);
		}
		return writable;
	}

	/**
	 * Copy this file to another (target file). Assumes no
	 * decoding necessary. If the target file exists,
	 * it's removed before copying.
	 */

	public boolean copyTo(File targetFile) throws IOException {

		if (targetFile == null) {
			throw new NullPointerError(
					"Null target file in RpcPerforceFile.copyTo");
		}

		if (targetFile.exists()) {
			if (!targetFile.delete()) {
				// Attempt to make the file writable if it isn't deleted,
				// continue even if it fails as an exception will be thrown by
				// the output stream
				if (!targetFile.canWrite()) {
					if (setWritable(targetFile.getAbsolutePath())) {
						targetFile.delete();
					}
				}

				//Warn if file still exists
				if (targetFile.exists()) {
					// FIXME: cope better with delete fail -- HR.
					Log
							.warn("Unable to delete target file for copy in RpcPerforceFile.copyTo; target: '"
									+ targetFile.getPath());
				}
			}
		}
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		FileChannel sourceChannel = null;
		FileChannel targetChannel = null;
		try {
			long bytesTransferred = 0;

			inStream = new FileInputStream(this);
			outStream = new FileOutputStream(targetFile);
			sourceChannel = inStream.getChannel();
			targetChannel = outStream.getChannel();

			if ((sourceChannel != null) && (targetChannel != null)) {
				// Light fuse, stand back...

				bytesTransferred = sourceChannel.transferTo(
						0, sourceChannel.size(), targetChannel);

				if (bytesTransferred != sourceChannel.size()) {
					Log.error("channel copy for copyTo operation failed with fewer bytes"
							+ " transferred than expected; expected: " + sourceChannel.size()
							+ "; saw: " + bytesTransferred);
					return false;    // FIXME: clean up...
				}

				return true;
			}
		} finally {
			try {
				if (sourceChannel != null) sourceChannel.close();
			} catch (Exception exc) {
				Log.warn("source channel file close error in RpcPerforceFile.copyTo(): "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
			try {
				if (targetChannel != null) targetChannel.close();
			} catch (Exception exc) {
				Log.warn("target channel file close error in RpcPerforceFile.copyTo(): "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
			try {
				if (inStream != null) inStream.close();
			} catch (Exception exc) {
				Log.warn("instream file close error in RpcPerforceFile.copyTo(): "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
			try {
				if (outStream != null) outStream.close();
			} catch (Exception exc) {
				Log.warn("outstream file close error in RpcPerforceFile.copyTo(): "
						+ exc.getLocalizedMessage());
				Log.exception(exc);
			}
		}

		return false;
	}

	public boolean decodeTo(File targetFile) throws IOException {

		if (targetFile == null) {
			throw new NullPointerError(
					"Null target file in RpcPerforceFile.decodeTo");
		}

		return copyTo(targetFile);
	}

	public RpcPerforceFileType getFileType() {
		return this.fileType;
	}

	public void setFileType(RpcPerforceFileType fileType) {
		this.fileType = fileType;
	}

	public ClientLineEnding getLineEnding() {
		return lineEnding;
	}

	public void setLineEnding(ClientLineEnding lineEnding) {
		this.lineEnding = lineEnding;
	}

	/**
	 * True IFF we should be able to copy this file as-is, i.e. without
	 * GKZIP decoding or munging, etc. Currently all file types can
	 * be copied as-is, but this wasn't always true and may not always
	 * be true...
	 */
	public boolean canCopyAsIs() {
		return true;
	}

	/**
	 * @see java.io.File#equals()
	 */
	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof RpcPerforceFile)) {
			if ((super.equals((File) obj)) &&
					(((RpcPerforceFile) obj).getFileType() == this.fileType) &&
					(((RpcPerforceFile) obj).getLineEnding() == this.lineEnding)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see java.io.File#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public boolean isSymlink() {
		if (SymbolicLinkHelper.isSymbolicLinkCapable()) {
			return SymbolicLinkHelper.isSymbolicLink(this.getAbsolutePath());
		}
		return !(this.isFile() || this.isDirectory());
	}

	/**
	 * Check if the file or symbolic link exists.
	 */
	public static boolean fileExists(File file, boolean fstSymlink) {

		if (file != null) {
			if (file.exists()) {
				return true;
			} else if (fstSymlink) {
				return SymbolicLinkHelper.exists(file.getPath());
			}
		}
		return false;
	}
}
