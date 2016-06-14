package com.perforce.p4java.server;

/**
 * Object representing the three parts of a Perforce ticket, the server address,
 * the user name, and the ticket token value.
 * 
 */
public class AuthTicket {

	private String serverAddress;
	private String userName;
	private String ticketValue;

	/**
	 * Creates an empty ticket
	 */
	public AuthTicket() {
		this(null, null, null);
	}

	/**
	 * Create a ticket with the specified server address, user name, and ticket
	 * value.
	 * 
	 * @param serverAddress
	 * @param userName
	 * @param ticketValue
	 */
	public AuthTicket(String serverAddress, String userName, String ticketValue) {
		this.serverAddress = serverAddress;
		this.userName = userName;
		this.ticketValue = ticketValue;
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
	 * @return the ticketValue
	 */
	public String getTicketValue() {
		return ticketValue;
	}

	/**
	 * @param ticketValue
	 *            the ticketValue to set
	 */
	public void setTicketValue(String ticketValue) {
		this.ticketValue = ticketValue;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof AuthTicket) {
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
				+ this.getTicketValue();
	}

}
