/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.io.apple;

import com.perforce.p4java.Log;
import com.perforce.p4java.exception.FileDecoderException;

/**
 * This abstract class handles AppleSingle/Double files. It contains a common
 * method to verify the Apple file, and figure out if it is an AppleSingle or
 * AppleDouble formatted file.
 * <p>
 * 
 * The AppleSingle format is a representation of Macintosh files as one
 * consecutive stream of bytes. AppleSingle combines the data fork, resource
 * fork and the related Finder meta-file information into a single file.
 * <p>
 * 
 * The AppleDouble format stores the data fork, resource fork as two separate
 * files. AppleDouble leaves the data fork in its original format, and the
 * resource fork and Finder information were combined into a second file.
 * <p>
 * 
 * Apple defined the magic number for the AppleSingle format as 0x00051600, and
 * the magic number for the AppleDouble format as 0x00051607.
 * 
 * <pre>
 * AppleSingle file header: 
 * 
 * Field Length
 * ----- ------
 * Magic number ------- 4 bytes
 * Version number ------ 4 bytes
 * Filler ------------- 16 bytes
 * Number of entries ----- 2 bytes
 * 
 * Entry descriptor for each entry:
 * Entry ID ------ 4 bytes
 * Offset -------- 4 bytes
 * Length -------- 4 bytes
 * 
 * Apple reserved entry IDs:
 * 
 * Data Fork -------- 1 Data fork
 * Resource Fork ----- 2 Resource fork
 * Real Name -------- 3 File's name as created on home file system
 * Comment --------- 4 Standard Macintosh comment
 * Icon, B&W -------- 5 Standard Macintosh black and white icon
 * Icon, Color -------- 6 Macintosh color icon
 * File Dates Info ------8 File creation date, modification date, and so on
 * Finder Info -------- 9 Standard Macintosh Finder information
 * Macintosh File Info ---10 Macintosh file information, attributes, and so on
 * ProDOS File Info -----11 ProDOS file information, attributes, and so on
 * MS-DOS File Info ----12 MS-DOS file information, attributes, and so on
 * Short Name --------13 AFP short name
 * AFP File Info ------- 14 AFP file information, attributes, and so on
 * Directory ID --------15 AFP directory ID
 * </pre>
 * 
 * See RFC 1740 for reference: http://tools.ietf.org/html/rfc1740
 */
public abstract class AppleFile {

	/** The Apple file format: AppleSingle, AppleDouble, default to unknown. */
	protected FileFormat format = FileFormat.UNKNOWN;

	/** The raw Apple file. */
	protected AppleFileData fileData = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 1: Data fork. */
	protected AppleFileData dataFork = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 2: Resource fork. */
	protected AppleFileData resourceFork = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 3: File's name as created on home file system. */
	protected AppleFileData realName = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 4: Standard Macintosh comment. */
	protected AppleFileData comment = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 5: Standard Macintosh black and white icon. */
	protected AppleFileData iconBW = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 6: Macintosh color icon. */
	protected AppleFileData iconColor = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 8: File creation date, modification date, and so on. */
	protected AppleFileData fileDatesInfo = AppleFileData.EMPTY_FILE_DATA;

	/** The file dates info entry. */
	protected FileDatesInfoEntry fileDatesInfoEntry = null;

	/** Entry 9: Standard Macintosh Finder information. */
	protected AppleFileData finderInfo = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 10: Macintosh file information, attributes, and so on. */
	protected AppleFileData macintoshInfo = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 11: ProDOS file information, attributes, and so on. */
	protected AppleFileData proDOSFileInfo = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 12: MS-DOS file information, attributes, and so on. */
	protected AppleFileData msDOSFileInfo = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 13: AFP short name. */
	protected AppleFileData shortName = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 14: AFP file information, attributes, and so on. */
	protected AppleFileData afpFileInfo = AppleFileData.EMPTY_FILE_DATA;

	/** Entry 15: AFP directory ID. */
	protected AppleFileData directoryID = AppleFileData.EMPTY_FILE_DATA;

	/** The num entries. */
	protected int numEntries = 0;

	/**
	 * The Apple file format.
	 */
	public enum FileFormat {

		APPLE_SINGLE,
		APPLE_DOUBLE,
		UNKNOWN;

		/**
		 * Return a suitable Apple file format as inferred from the passed-in
		 * string. Otherwise return the UNKNOWN file format.
		 * 
		 * @param fileFormat
		 *            the file format
		 * @return the FileFormat
		 */
		public static FileFormat fromString(String fileFormat) {
			if (fileFormat == null) {
				return null;
			}

			try {
				return FileFormat.valueOf(fileFormat.toUpperCase());
			} catch (IllegalArgumentException iae) {
				Log.error("Bad conversion attempt in FileFormat.fromString; string: "
						+ fileFormat + "; message: " + iae.getMessage());
				Log.exception(iae);
				return UNKNOWN;
			}
		}
	};

	/**
	 * This class represents the file dates.
	 */
	public class FileDatesInfoEntry {

		/** The create time. */
		private int createTime = Integer.MIN_VALUE;

		/** The modify time. */
		private int modifyTime = Integer.MIN_VALUE;

		/** The backup time. */
		private int backupTime = Integer.MIN_VALUE;

		/** The access time. */
		private int accessTime = Integer.MIN_VALUE;

		/**
		 * Instantiates a new file dates info entry.
		 */
		public FileDatesInfoEntry() {

		}

		/**
		 * Gets the creates the time.
		 * 
		 * @return the creates the time
		 */
		public int getCreateTime() {
			return createTime;
		}

		/**
		 * Sets the creates the time.
		 * 
		 * @param createTime
		 *            the new creates the time
		 */
		public void setCreateTime(int createTime) {
			this.createTime = createTime;
		}

		/**
		 * Gets the modify time.
		 * 
		 * @return the modify time
		 */
		public int getModifyTime() {
			return modifyTime;
		}

		/**
		 * Sets the modify time.
		 * 
		 * @param modifyTime
		 *            the new modify time
		 */
		public void setModifyTime(int modifyTime) {
			this.modifyTime = modifyTime;
		}

		/**
		 * Gets the backup time.
		 * 
		 * @return the backup time
		 */
		public int getBackupTime() {
			return backupTime;
		}

		/**
		 * Sets the backup time.
		 * 
		 * @param backupTime
		 *            the new backup time
		 */
		public void setBackupTime(int backupTime) {
			this.backupTime = backupTime;
		}

		/**
		 * Gets the access time.
		 * 
		 * @return the access time
		 */
		public int getAccessTime() {
			return accessTime;
		}

		/**
		 * Sets the access time.
		 * 
		 * @param accessTime
		 *            the new access time
		 */
		public void setAccessTime(int accessTime) {
			this.accessTime = accessTime;
		}
	}

	/**
	 * Sets the num entries.
	 * 
	 * @param numEntries
	 *            the new num entries
	 */
	public void setNumEntries(int numEntries) {
		this.numEntries = numEntries;
	}

	/**
	 * Verify the validity of the Apple file.
	 * 
	 * @throws FileDecoderException
	 *             the file decoder exception
	 */
	@SuppressWarnings("unused")
	protected void verify() throws FileDecoderException {
		byte[] data = this.fileData.getData();
		int offset = this.fileData.getOffset();
		int length = this.fileData.getLength();
		int position = offset;
		if (length < 26) {
			throw new FileDecoderException("File is too short");
		}

		/* Magic number */
		int magic = 0;
		magic |= data[(position++)] & 0xFF;
		magic <<= 8;
		magic |= data[(position++)] & 0xFF;
		magic <<= 8;
		magic |= data[(position++)] & 0xFF;
		magic <<= 8;
		magic |= data[(position++)] & 0xFF;

		/* Check Apple file format: AppleSingle or AppleDobule */
		if (magic == 0x00051600) {
			this.format = FileFormat.APPLE_SINGLE;
		} else if (magic == 0x00051607) {
			this.format = FileFormat.APPLE_DOUBLE;
		} else {
			throw new FileDecoderException("Invalid Apple file magic number.");
		}

		/* Version number */
		int version = 0;
		version |= data[(position++)] & 0xFF;
		version <<= 8;
		version |= data[(position++)] & 0xFF;
		version <<= 8;
		version |= data[(position++)] & 0xFF;
		version <<= 8;
		version |= data[(position++)] & 0xFF;
		if (version != 0x00020000) {
			throw new FileDecoderException("Unknown Apple file version");
		}

		/* Filler */
		position += 16;

		/* Number of entries */
		this.numEntries = 0;
		this.numEntries |= data[(position++)] & 0xFF;
		this.numEntries <<= 8;
		this.numEntries |= data[(position++)] & 0xFF;
		if (length < 26 + 12 * this.numEntries) {
			throw new FileDecoderException("Corrupt Apple file data.");
		}

		/* Check entries */
		int entryId = 0;
		int entryOffset = 0;
		int entryLength = 0;
		int contentPosition = 26 + 12 * this.numEntries;
		for (int i = 0; i < this.numEntries; i++) {
			position = 26 + i * 12;
			/* Entry ID */
			entryId = 0;
			entryId |= data[(position++)] & 0xFF;
			entryId <<= 8;
			entryId |= data[(position++)] & 0xFF;
			entryId <<= 8;
			entryId |= data[(position++)] & 0xFF;
			entryId <<= 8;
			entryId |= data[(position++)] & 0xFF;

			/* Entry offset */
			entryOffset = 0;
			entryOffset |= data[(position++)] & 0xFF;
			entryOffset <<= 8;
			entryOffset |= data[(position++)] & 0xFF;
			entryOffset <<= 8;
			entryOffset |= data[(position++)] & 0xFF;
			entryOffset <<= 8;
			entryOffset |= data[(position++)] & 0xFF;
			entryOffset &= 0x7FFFFFFF;

			/* Entry length */
			entryLength = 0;
			entryLength |= data[(position++)] & 0xFF;
			entryLength <<= 8;
			entryLength |= data[(position++)] & 0xFF;
			entryLength <<= 8;
			entryLength |= data[(position++)] & 0xFF;
			entryLength <<= 8;
			entryLength |= data[(position++)] & 0xFF;
			entryLength &= 0x7FFFFFFF;
			if ((entryOffset < contentPosition)
					|| (length < entryOffset + entryLength)) {
				throw new FileDecoderException("Corrupt Apple file data.");
			}
		}
	}

	/**
	 * Extract file dates.
	 *
	 * @param data the data
	 * @param offset the offset
	 * @param length the length
	 */
	protected void extractFileDates(byte[] data, int offset, int length) {
		if ((0 > offset) || (offset > data.length))
			throw new IndexOutOfBoundsException();
		if ((0 > length) || (length > data.length - offset))
			throw new IndexOutOfBoundsException();

		  int position = offset;
		
		  int createTime = 0;
	      createTime |= data[(position++)] & 0xFF;
	      createTime <<= 8;
	      createTime |= data[(position++)] & 0xFF;
	      createTime <<= 8;
	      createTime |= data[(position++)] & 0xFF;
	      createTime <<= 8;
	      createTime |= data[(position++)] & 0xFF;
	      int modifyTime = 0;
	      modifyTime |= data[(position++)] & 0xFF;
	      modifyTime <<= 8;
	      modifyTime |= data[(position++)] & 0xFF;
	      modifyTime <<= 8;
	      modifyTime |= data[(position++)] & 0xFF;
	      modifyTime <<= 8;
	      modifyTime |= data[(position++)] & 0xFF;
	      int backupTime = 0;
	      backupTime |= data[(position++)] & 0xFF;
	      backupTime <<= 8;
	      backupTime |= data[(position++)] & 0xFF;
	      backupTime <<= 8;
	      backupTime |= data[(position++)] & 0xFF;
	      backupTime <<= 8;
	      backupTime |= data[(position++)] & 0xFF;
	      int accessTime = 0;
	      accessTime |= data[(position++)] & 0xFF;
	      accessTime <<= 8;
	      accessTime |= data[(position++)] & 0xFF;
	      accessTime <<= 8;
	      accessTime |= data[(position++)] & 0xFF;
	      accessTime <<= 8;
	      accessTime |= data[(position++)] & 0xFF;

	      this.fileDatesInfoEntry = new FileDatesInfoEntry();
	      fileDatesInfoEntry.setCreateTime(createTime);
	      fileDatesInfoEntry.setModifyTime(modifyTime);
	      fileDatesInfoEntry.setBackupTime(backupTime);
	      fileDatesInfoEntry.setAccessTime(accessTime);
	}

	/**
	 * Gets the format.
	 * 
	 * @return the format
	 */
	public FileFormat getFormat() {
		return format;
	}

	/**
	 * Sets the format.
	 * 
	 * @param format
	 *            the new format
	 */
	public void setFormat(FileFormat format) {
		this.format = format;
	}

	/**
	 * Gets the file data.
	 * 
	 * @return the file data
	 */
	public AppleFileData getFileData() {
		return fileData;
	}

	/**
	 * Sets the file data.
	 * 
	 * @param fileData
	 *            the new file data
	 */
	public void setFileData(AppleFileData fileData) {
		this.fileData = fileData;
	}

	/**
	 * Gets the data fork.
	 * 
	 * @return the data fork
	 */
	public AppleFileData getDataFork() {
		return dataFork;
	}

	/**
	 * Sets the data fork.
	 * 
	 * @param dataFork
	 *            the new data fork
	 */
	public void setDataFork(AppleFileData dataFork) {
		this.dataFork = dataFork;
	}

	/**
	 * Gets the resource fork.
	 * 
	 * @return the resource fork
	 */
	public AppleFileData getResourceFork() {
		return resourceFork;
	}

	/**
	 * Sets the resource fork.
	 * 
	 * @param resourceFork
	 *            the new resource fork
	 */
	public void setResourceFork(AppleFileData resourceFork) {
		this.resourceFork = resourceFork;
	}

	/**
	 * Gets the real name.
	 * 
	 * @return the real name
	 */
	public AppleFileData getRealName() {
		return realName;
	}

	/**
	 * Sets the real name.
	 * 
	 * @param realName
	 *            the new real name
	 */
	public void setRealName(AppleFileData realName) {
		this.realName = realName;
	}

	/**
	 * Gets the comment.
	 * 
	 * @return the comment
	 */
	public AppleFileData getComment() {
		return comment;
	}

	/**
	 * Sets the comment.
	 * 
	 * @param comment
	 *            the new comment
	 */
	public void setComment(AppleFileData comment) {
		this.comment = comment;
	}

	/**
	 * Gets the icon bw.
	 * 
	 * @return the icon bw
	 */
	public AppleFileData getIconBW() {
		return iconBW;
	}

	/**
	 * Sets the icon bw.
	 * 
	 * @param iconBW
	 *            the new icon bw
	 */
	public void setIconBW(AppleFileData iconBW) {
		this.iconBW = iconBW;
	}

	/**
	 * Gets the icon color.
	 * 
	 * @return the icon color
	 */
	public AppleFileData getIconColor() {
		return iconColor;
	}

	/**
	 * Sets the icon color.
	 * 
	 * @param iconColor
	 *            the new icon color
	 */
	public void setIconColor(AppleFileData iconColor) {
		this.iconColor = iconColor;
	}

	/**
	 * Gets the file dates info.
	 * 
	 * @return the file dates info
	 */
	public AppleFileData getFileDatesInfo() {
		return fileDatesInfo;
	}

	/**
	 * Sets the file dates info.
	 * 
	 * @param fileDatesInfo
	 *            the new file dates info
	 */
	public void setFileDatesInfo(AppleFileData fileDatesInfo) {
		this.fileDatesInfo = fileDatesInfo;
	}

	/**
	 * Gets the finder info.
	 * 
	 * @return the finder info
	 */
	public AppleFileData getFinderInfo() {
		return finderInfo;
	}

	/**
	 * Sets the finder info.
	 * 
	 * @param finderInfo
	 *            the new finder info
	 */
	public void setFinderInfo(AppleFileData finderInfo) {
		this.finderInfo = finderInfo;
	}

	/**
	 * Gets the macintosh info.
	 * 
	 * @return the macintosh info
	 */
	public AppleFileData getMacintoshInfo() {
		return macintoshInfo;
	}

	/**
	 * Sets the macintosh info.
	 * 
	 * @param macintoshInfo
	 *            the new macintosh info
	 */
	public void setMacintoshInfo(AppleFileData macintoshInfo) {
		this.macintoshInfo = macintoshInfo;
	}

	/**
	 * Gets the pro dos file info.
	 * 
	 * @return the pro dos file info
	 */
	public AppleFileData getProDOSFileInfo() {
		return proDOSFileInfo;
	}

	/**
	 * Sets the pro dos file info.
	 * 
	 * @param proDOSFileInfo
	 *            the new pro dos file info
	 */
	public void setProDOSFileInfo(AppleFileData proDOSFileInfo) {
		this.proDOSFileInfo = proDOSFileInfo;
	}

	/**
	 * Gets the ms dos file info.
	 * 
	 * @return the ms dos file info
	 */
	public AppleFileData getMsDOSFileInfo() {
		return msDOSFileInfo;
	}

	/**
	 * Sets the ms dos file info.
	 * 
	 * @param msDOSFileInfo
	 *            the new ms dos file info
	 */
	public void setMsDOSFileInfo(AppleFileData msDOSFileInfo) {
		this.msDOSFileInfo = msDOSFileInfo;
	}

	/**
	 * Gets the short name.
	 * 
	 * @return the short name
	 */
	public AppleFileData getShortName() {
		return shortName;
	}

	/**
	 * Sets the short name.
	 * 
	 * @param shortName
	 *            the new short name
	 */
	public void setShortName(AppleFileData shortName) {
		this.shortName = shortName;
	}

	/**
	 * Gets the afp file info.
	 * 
	 * @return the afp file info
	 */
	public AppleFileData getAfpFileInfo() {
		return afpFileInfo;
	}

	/**
	 * Sets the afp file info.
	 * 
	 * @param afpFileInfo
	 *            the new afp file info
	 */
	public void setAfpFileInfo(AppleFileData afpFileInfo) {
		this.afpFileInfo = afpFileInfo;
	}

	/**
	 * Gets the directory id.
	 * 
	 * @return the directory id
	 */
	public AppleFileData getDirectoryID() {
		return directoryID;
	}

	/**
	 * Sets the directory id.
	 * 
	 * @param directoryID
	 *            the new directory id
	 */
	public void setDirectoryID(AppleFileData directoryID) {
		this.directoryID = directoryID;
	}

	/**
	 * Gets the num entries.
	 * 
	 * @return the num entries
	 */
	public int getNumEntries() {
		return numEntries;
	}
}