/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.FileDecoderException;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFile;
import com.perforce.p4java.impl.mapbased.rpc.sys.RpcPerforceFileType;
import com.perforce.p4java.io.apple.AppleFileData;
import com.perforce.p4java.io.apple.AppleFileDecoder;

/**
 * Helper class for handling Apple files.
 */
public class AppleFileHelper {

	/**
	 * Extract the data fork and the resource fork from the Apple file.
	 *
	 * @param file the Apple file
	 */
	public static void extractFile(RpcPerforceFile file) {
		if (file.getFileType() == RpcPerforceFileType.FST_APPLEFILE) {
			FileOutputStream fosData = null;
			FileOutputStream fosResource = null;
			try {
				byte[] data = AppleFileHelper.getBytesFromFile(file);
				AppleFileData fileData = new AppleFileData(data);
				AppleFileDecoder appleFile = new AppleFileDecoder(fileData);
				appleFile.extract();
				fosData = new FileOutputStream(file);
				AppleFileData forkData = appleFile.getDataFork();
				if (forkData != AppleFileData.EMPTY_FILE_DATA) {
					fosData.write(forkData.getBytes());
				}
				String resourceFilePath = file.getParent() + File.separator + "%" + file.getName();
				RpcPerforceFile targetResourceFile = new RpcPerforceFile(resourceFilePath, file.getFileType());
				fosResource = new FileOutputStream(targetResourceFile);
				AppleFileData forkResource = appleFile.getResourceFork();
				if (forkResource != AppleFileData.EMPTY_FILE_DATA) {
					fosResource.write(forkResource.getBytes());
				}
			} catch (IOException e) {
				Log.error("Problem handling the Apple file: " + file.getName());
			} catch (FileDecoderException e) {
				Log.error("Problem decoding the Apple file: " + file.getName());
			} finally {
				if (fosData != null) {
					try {
						fosData.close();
					} catch (Exception e) {
						// Do nothing
					}
				}
				if (fosResource != null) {
					try {
						fosResource.close();
					} catch (Exception e) {
						// Do nothing
					}
				}
			}
		}
	}
	
	/**
	 * Gets the bytes from file.
	 * 
	 * @param file
	 *            the file
	 * @return the bytes from file
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			// File is too large
			throw new IOException("Apple file too large for decoding.");
		}

		byte[] bytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;

		try {
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
				offset += numRead;
			}
			// Ensure all the bytes have been read in
			if (offset < bytes.length) {
				throw new IOException(
						"Could not completely read the Apple file "
								+ file.getName());
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Do nothing
				}
			}
		}

		return bytes;
	}
}
