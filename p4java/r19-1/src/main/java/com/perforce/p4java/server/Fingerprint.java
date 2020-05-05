package com.perforce.p4java.server;

/**
 * Object representing the three parts of a Perforce fingerprint, the server
 * address, the user name, and the ticket token value. <p>
 * 
 * Note: currently the user portion should be "******" as it is not used.
 */
public class Fingerprint {

	private String serverAddress;
	private String userName;
	private String fingerprintValue;

	/**
	 * Creates an empty fingerprint
	 */
	public Fingerprint() {
		this(null, null, null);
	}

	/**
	 * Create a fingerprint with the specified server address, user name, and
	 * fingerprint value.
	 * 
	 * @param serverAddress
	 * @param userName
	 * @param fingerprintValue
	 */
	public Fingerprint(String serverAddress, String userName, String fingerprintValue) {
		this.serverAddress = serverAddress;
		this.userName = userName;
		this.fingerprintValue = fingerprintValue;
	}

	/**
	 * @return the serverAddress
	 */
	public String getServerAddress() {
		return serverAddress;
	}

	/**
	 * @param serverAddress
	 *            the serverAddress to set
	 */
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName
	 *            the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the fingerprintValue
	 */
	public String getFingerprintValue() {
		return fingerprintValue;
	}

	/**
	 * @param fingerprintValue
	 *            the fingerprintValue to set
	 */
	public void setFingerprintValue(String fingerprintValue) {
		this.fingerprintValue = fingerprintValue;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Fingerprint) {
			return this.toString().equals(obj.toString());
		} else {
			return false;
		}
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.serverAddress + "=" + this.userName + ":"
				+ this.getFingerprintValue();
	}

}
