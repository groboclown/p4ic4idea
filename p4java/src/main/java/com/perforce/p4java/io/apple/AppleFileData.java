/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.io.apple;

/**
 * This class is for representing the AppleSingle/Double file or its file forks
 * (data fork and resource fork) and the related Finder meta-file information.
 */
public final class AppleFileData {

	public static final AppleFileData EMPTY_FILE_DATA = new AppleFileData();
	private byte[] data;
	private int offset;
	private int length;

	/**
	 * Instantiates a new apple file data.
	 */
	public AppleFileData() {
		this.data = new byte[0];
		this.offset = 0;
		this.length = 0;
	}

	/**
	 * Instantiates a new apple file data.
	 *
	 * @param data the data
	 */
	public AppleFileData(byte[] data) {
		this.data = data;
		this.offset = 0;
		this.length = data.length;
	}

	/**
	 * Instantiates a new apple file data.
	 *
	 * @param data the data
	 * @param offset the offset
	 * @param length the length
	 */
	public AppleFileData(byte[] data, int offset, int length) {
		if ((0 > offset) || (offset > data.length))
			throw new IndexOutOfBoundsException();
		if ((0 > length) || (length > data.length - offset))
			throw new IndexOutOfBoundsException();
		this.data = data;
		this.offset = offset;
		this.length = length;
	}

	/**
	 * Gets the bytes.
	 *
	 * @return the bytes
	 */
	public byte[] getBytes() {
		byte[] data = new byte[this.length];
		System.arraycopy(this.data, this.offset, data, 0, this.length);
		return data;
	}

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * Gets the offset.
	 *
	 * @return the offset
	 */
	public int getOffset() {
		return this.offset;
	}

	/**
	 * Gets the length.
	 *
	 * @return the length
	 */
	public int getLength() {
		return this.length;
	}
}