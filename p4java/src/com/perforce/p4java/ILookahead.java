package com.perforce.p4java;

/**
 * Interface for use by {@link CharsetConverter} to query for more bytes to
 * convert based on the last character decoded. This is most commonly used for
 * line ending lookahead when \r\n needs to be converted to \n. The lookahead in
 * that case would get the next x bytes where x is the number of bytes wide the
 * \n character is when the \r character is specified as the last decoded
 * character.
 * 
 */
public interface ILookahead {

	/**
	 * Get an array of bytes to add based on the last decoded character found.
	 * The array of bytes returned represent "looking ahead" to see if more
	 * characters should be converted.
	 * 
	 * @param lastDecodedChar
	 * @return - array of bytes or null if lookahead is not needed
	 */
	public byte[] bytesToAdd(char lastDecodedChar);

}
