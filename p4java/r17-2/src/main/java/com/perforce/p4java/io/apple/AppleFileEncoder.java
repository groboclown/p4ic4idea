/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.io.apple;

import com.perforce.p4java.exception.FileEncoderException;

/**
 * This class handles the combination of the data fork, resource fork and other
 * entries into an AppleSingle/Double file.
 * <p>
 * 
 * Note that if it is an AppleDouble, the data fork is a separate file external
 * to this file.
 */
public class AppleFileEncoder extends AppleFile {

	/**
	 * Instantiates a new apple file decoder.
	 * 
	 * @throws FileEncoderException 
	 */
	public AppleFileEncoder(FileFormat fileFormat) throws FileEncoderException {
		if (fileFormat == null) {
			throw new FileEncoderException("Null file format passed to the AppleFileEncoder constructor.");
		}
		if (fileFormat == FileFormat.UNKNOWN) {
			throw new FileEncoderException("Unknown file format passed to the AppleFileEncoder constructor.");
		}
	}

	/**
	 * Combine the data fork, resource fork and other entries into an
	 * AppleSingle/Double file.
	 * 
	 * @throws FileEncoderException
	 *             the file encoder exception
	 */
	@SuppressWarnings("unused")
	public void combine() throws FileEncoderException {

		boolean isAppleSingle = (this.format == FileFormat.APPLE_SINGLE);
		boolean isAppleDouble = (this.format == FileFormat.APPLE_DOUBLE);

		boolean hasDataFork = (this.dataFork != AppleFileData.EMPTY_FILE_DATA);
		boolean hasResourceFork = (this.resourceFork != AppleFileData.EMPTY_FILE_DATA);
		boolean hasRealName = (this.realName != AppleFileData.EMPTY_FILE_DATA);
		boolean hasComment = (this.comment != AppleFileData.EMPTY_FILE_DATA);
		boolean hasIconBW = (this.iconBW != AppleFileData.EMPTY_FILE_DATA);
		boolean hasIconColor = (this.iconColor != AppleFileData.EMPTY_FILE_DATA);
		boolean hasFileDatesInfo = (this.fileDatesInfoEntry != null);
		boolean hasFinderInfo = (this.finderInfo != AppleFileData.EMPTY_FILE_DATA);
		boolean hasMacintoshInfo = (this.macintoshInfo != AppleFileData.EMPTY_FILE_DATA);
		boolean hasProDOSFileInfo = (this.proDOSFileInfo != AppleFileData.EMPTY_FILE_DATA);
		boolean hasMsDOSFileInfo = (this.msDOSFileInfo != AppleFileData.EMPTY_FILE_DATA);
		boolean hasShortName = (this.shortName != AppleFileData.EMPTY_FILE_DATA);
		boolean hasAfpFileInfo = (this.afpFileInfo != AppleFileData.EMPTY_FILE_DATA);
		boolean hasDirectoryID = (this.directoryID != AppleFileData.EMPTY_FILE_DATA);

		this.fileData = AppleFileData.EMPTY_FILE_DATA;

		int length = 90 + this.realName.getLength()
				+ this.resourceFork.getLength();

		/* AppleSingle includes the data fork */
		if (isAppleSingle) {
			length += this.dataFork.getLength();
		}

		byte[] data = new byte[length];
		int position = 0;

		/* Magic number for AppleSingle or AppleDouble */
		if (isAppleDouble) {
			data[(position++)] = 0;
			data[(position++)] = 5;
			data[(position++)] = 22;
			data[(position++)] = 0;
		} else {
			data[(position++)] = 0;
			data[(position++)] = 5;
			data[(position++)] = 22;
			data[(position++)] = 7;
		}

		/* Version number */
		data[(position++)] = 0;
		data[(position++)] = 2;
		data[(position++)] = 0;
		data[(position++)] = 0;

		/* Filler */

		for (int k = 0; k < 16; k++) {
			data[(position++)] = 0;
		}

		/* Number of entries */
		this.numEntries = 0;
		if (hasRealName) {
			this.numEntries += 1;
		}
		if (hasFileDatesInfo) {
			this.numEntries += 1;
		}
		if (hasResourceFork) {
			this.numEntries += 1;
		}
		if ((hasDataFork) && (isAppleSingle)) {
			this.numEntries += 1;
		}
		data[(position++)] = ((byte) (this.numEntries >> 8 & 0xFF));
		data[(position++)] = ((byte) (this.numEntries & 0xFF));

		/* Header information for the entries */

		/* Real name entry header */
		int realNamePosition = 0;
		if (hasRealName) {
			int realNameEntryId = 3;
			int realNameEntryOffset = 0;
			int realNameEntryLength = this.realName.getLength();
			data[(position++)] = ((byte) (realNameEntryId >> 24 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryId >> 16 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryId >> 8 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryId >> 0 & 0xFF));
			realNamePosition = position;
			data[(position++)] = ((byte) (realNameEntryOffset >> 24 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryOffset >> 16 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryOffset >> 8 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryOffset >> 0 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryLength >> 24 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryLength >> 16 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryLength >> 8 & 0xFF));
			data[(position++)] = ((byte) (realNameEntryLength >> 0 & 0xFF));
		}

		/* File dates info entry header */
		int fileDatesInfoPosition = 0;
		if (hasFileDatesInfo) {
			int fileDatesInfoEntryId = 8;
			int fileDatesInfoEntryOffset = 0;
			int fileDatesInfoEntryLength = 16;
			data[(position++)] = ((byte) (fileDatesInfoEntryId >> 24 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryId >> 16 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryId >> 8 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryId >> 0 & 0xFF));
			fileDatesInfoPosition = position;
			data[(position++)] = ((byte) (fileDatesInfoEntryOffset >> 24 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryOffset >> 16 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryOffset >> 8 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryOffset >> 0 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryLength >> 24 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryLength >> 16 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryLength >> 8 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoEntryLength >> 0 & 0xFF));
		}

		/* Resource fork entry header */
		int resourceForkPosition = 0;
		if (hasResourceFork) {
			int resourceForkEntryId = 2;
			int resourceForkEntryOffset = 0;
			int resourceFokrEntryLength = this.resourceFork.getLength();
			data[(position++)] = ((byte) (resourceForkEntryId >> 24 & 0xFF));
			data[(position++)] = ((byte) (resourceForkEntryId >> 16 & 0xFF));
			data[(position++)] = ((byte) (resourceForkEntryId >> 8 & 0xFF));
			data[(position++)] = ((byte) (resourceForkEntryId >> 0 & 0xFF));
			resourceForkPosition = position;
			data[(position++)] = ((byte) (resourceForkEntryOffset >> 24 & 0xFF));
			data[(position++)] = ((byte) (resourceForkEntryOffset >> 16 & 0xFF));
			data[(position++)] = ((byte) (resourceForkEntryOffset >> 8 & 0xFF));
			data[(position++)] = ((byte) (resourceForkEntryOffset >> 0 & 0xFF));
			data[(position++)] = ((byte) (resourceFokrEntryLength >> 24 & 0xFF));
			data[(position++)] = ((byte) (resourceFokrEntryLength >> 16 & 0xFF));
			data[(position++)] = ((byte) (resourceFokrEntryLength >> 8 & 0xFF));
			data[(position++)] = ((byte) (resourceFokrEntryLength >> 0 & 0xFF));
		}

		/* Data fork entry header */
		int dataForkPosition = 0;
		if ((hasDataFork) && (isAppleSingle)) {
			int dataForkEntryId = 1;
			int dataForkEntryOffset = 0;
			int dataForkEntryLength = this.dataFork.getLength();
			data[(position++)] = ((byte) (dataForkEntryId >> 24 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryId >> 16 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryId >> 8 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryId >> 0 & 0xFF));
			dataForkPosition = position;
			data[(position++)] = ((byte) (dataForkEntryOffset >> 24 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryOffset >> 16 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryOffset >> 8 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryOffset >> 0 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryLength >> 24 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryLength >> 16 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryLength >> 8 & 0xFF));
			data[(position++)] = ((byte) (dataForkEntryLength >> 0 & 0xFF));
		}

		/* Content for the entries */

		/* Real name content */
		if (hasRealName) {
			int realNamePositionCurrent = position;
			position = realNamePosition;
			data[(position++)] = ((byte) (realNamePositionCurrent >> 24 & 0xFF));
			data[(position++)] = ((byte) (realNamePositionCurrent >> 16 & 0xFF));
			data[(position++)] = ((byte) (realNamePositionCurrent >> 8 & 0xFF));
			data[(position++)] = ((byte) (realNamePositionCurrent >> 0 & 0xFF));
			position = realNamePositionCurrent;
			byte[] realNameData = this.realName.getData();
			int realNameOffset = this.realName.getOffset();
			int realNameLength = this.realName.getLength();
			System.arraycopy(realNameData, realNameOffset, data, position,
					realNameLength);
			position += realNameLength;
		}

		/* File dates info content */
		if (hasFileDatesInfo) {
			int fileDatesInfoPositionCurrent = position;
			position = fileDatesInfoPosition;
			data[(position++)] = ((byte) (fileDatesInfoPositionCurrent >> 24 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoPositionCurrent >> 16 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoPositionCurrent >> 8 & 0xFF));
			data[(position++)] = ((byte) (fileDatesInfoPositionCurrent >> 0 & 0xFF));
			position = fileDatesInfoPositionCurrent;
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getCreateTime() >> 24 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getCreateTime() >> 16 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getCreateTime() >> 8 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getCreateTime() >> 0 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getModifyTime() >> 24 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getModifyTime() >> 16 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getModifyTime() >> 8 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getModifyTime() >> 0 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getBackupTime() >> 24 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getBackupTime() >> 16 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getBackupTime() >> 8 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getBackupTime() >> 0 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getAccessTime() >> 24 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getAccessTime() >> 16 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getAccessTime() >> 8 & 0xFF));
			data[(position++)] = ((byte) (this.fileDatesInfoEntry
					.getAccessTime() >> 0 & 0xFF));
		}

		/* Resource fork content */
		if (hasResourceFork) {
			int resourceForkPositionCurrent = position;
			position = resourceForkPosition;
			data[(position++)] = ((byte) (resourceForkPositionCurrent >> 24 & 0xFF));
			data[(position++)] = ((byte) (resourceForkPositionCurrent >> 16 & 0xFF));
			data[(position++)] = ((byte) (resourceForkPositionCurrent >> 8 & 0xFF));
			data[(position++)] = ((byte) (resourceForkPositionCurrent >> 0 & 0xFF));
			position = resourceForkPositionCurrent;
			byte[] resourceForkData = this.resourceFork.getData();
			int resourceForkOffset = this.resourceFork.getOffset();
			int resourceForkLength = this.resourceFork.getLength();
			System.arraycopy(resourceForkData, resourceForkOffset, data,
					position, resourceForkLength);
			position += resourceForkLength;
		}

		/* Data fork content */
		if ((hasDataFork) && (isAppleSingle)) {
			int dataForkPosition2 = position;
			position = dataForkPosition;
			data[(position++)] = ((byte) (dataForkPosition2 >> 24 & 0xFF));
			data[(position++)] = ((byte) (dataForkPosition2 >> 16 & 0xFF));
			data[(position++)] = ((byte) (dataForkPosition2 >> 8 & 0xFF));
			data[(position++)] = ((byte) (dataForkPosition2 >> 0 & 0xFF));
			position = dataForkPosition2;
			byte[] dataForkData = this.dataFork.getData();
			int dataForkOffset = this.dataFork.getOffset();
			int dataForkLength = this.dataFork.getLength();
			System.arraycopy(dataForkData, dataForkOffset, data, position,
					dataForkLength);
			position += dataForkLength;
		}

		/* Create the Apple file data */
		this.fileData = new AppleFileData(data, 0, position);
	}
}