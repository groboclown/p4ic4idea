/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.io.apple;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.FileDecoderException;

/**
 * This class handles the extraction of the data fork, resource fork and other
 * entries from an AppleSingle/Double file. The Perforce 'apple' file type is a
 * compressed AppleSingle (Mac resource + data) file. The Perforce 'resource'
 * file type is a compressed AppleDouble (Mac resource fork) file.
 */
public class AppleFileDecoder extends AppleFile {

	/**
	 * Instantiates a new apple file decoder.
	 * 
	 * @param fileData
	 *            the file data
	 */
	public AppleFileDecoder(AppleFileData fileData) {
		if (fileData != null) {
			this.fileData = fileData;
		}
	}

	/**
	 * Extract the data fork, resource fork and other entries from the Apple
	 * file.
	 * 
	 * @throws FileDecoderException
	 *             the file decoder exception
	 */
	@SuppressWarnings("unused")
	public void extract() throws FileDecoderException {
		// Verify the validity of the Apple file
		verify();

		byte[] data = this.fileData.getData();
		int offset = this.fileData.getOffset();
		int length = this.fileData.getLength();
		int contentPosition = 0;
		int entryId = 0;
		int entryOffset = 0;
		int entryLength = 0;
		for (int i = 0; i < this.numEntries; i++) {
			contentPosition = offset + 26 + i * 12;
			/* Entry ID */
			entryId = 0;
			entryId |= data[(contentPosition++)] & 0xFF;
			entryId <<= 8;
			entryId |= data[(contentPosition++)] & 0xFF;
			entryId <<= 8;
			entryId |= data[(contentPosition++)] & 0xFF;
			entryId <<= 8;
			entryId |= data[(contentPosition++)] & 0xFF;

			/* Entry offset */
			entryOffset = 0;
			entryOffset |= data[(contentPosition++)] & 0xFF;
			entryOffset <<= 8;
			entryOffset |= data[(contentPosition++)] & 0xFF;
			entryOffset <<= 8;
			entryOffset |= data[(contentPosition++)] & 0xFF;
			entryOffset <<= 8;
			entryOffset |= data[(contentPosition++)] & 0xFF;
			entryOffset &= 0x7FFFFFFF;

			/* Entry length */
			entryLength = 0;
			entryLength |= data[(contentPosition++)] & 0xFF;
			entryLength <<= 8;
			entryLength |= data[(contentPosition++)] & 0xFF;
			entryLength <<= 8;
			entryLength |= data[(contentPosition++)] & 0xFF;
			entryLength <<= 8;
			entryLength |= data[(contentPosition++)] & 0xFF;
			entryLength &= 0x7FFFFFFF;

			switch (entryId) {
			case 1:
				this.dataFork = new AppleFileData(data, offset + entryOffset,
						entryLength);
				break;
			case 2:
				this.resourceFork = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 3:
				this.realName = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 4:
				this.comment = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 5:
				this.iconBW = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 6:
				this.iconColor = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 8:
				this.fileDatesInfo = new AppleFileData(data, offset
						+ entryOffset, entryLength);
					extractFileDates(data, offset + entryOffset, entryLength);
				break;
			case 9:
				this.finderInfo = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 10:
				this.macintoshInfo = new AppleFileData(data, offset
						+ entryOffset, entryLength);
			case 11:
				this.proDOSFileInfo = new AppleFileData(data, offset
						+ entryOffset, entryLength);
			case 12:
				this.msDOSFileInfo = new AppleFileData(data, offset
						+ entryOffset, entryLength);
			case 13:
				this.shortName = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 14:
				this.afpFileInfo = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			case 15:
				this.directoryID = new AppleFileData(data, offset
						+ entryOffset, entryLength);
				break;
			default:
				Log.warn("Apple file entry ID: " + entryId + " is not handled.");

			}
		}
	}
}