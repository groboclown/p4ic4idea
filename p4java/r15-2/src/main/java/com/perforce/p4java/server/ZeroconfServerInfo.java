/**
 * 
 */
package com.perforce.p4java.server;

/**
 * Defines the information zeroconf gives us about Perforce servers registered
 * with and locally-visible to zeroconf (assuming zeroconf is available and
 * loaded with P4Java). See ServerFactory.getZeroconfServers and associated
 * methods for a full explanation of Perforce zeroconf usage.<p>
 * 
 * Most of the fields defined below should be self-explanatory, but note that any
 * or all of them may be null, and the semantics and format of the description
 * and version strings are not defined here at all (but the version string is
 * usually in the standard Perforce format if this ZeroconfServerInfo object was
 * cobbled together from a valid zerconf registration).
 * 
 * @deprecated  As of release 2013.1, ZeroConf is no longer supported by the
 * 				Perforce server 2013.1.
 */
@Deprecated
public class ZeroconfServerInfo {
	
	/**
	 * The port number used to indicate that the port has not
	 * been validly set anywhere.
	 */
	public static int P4D_ZEROCONF_NOPORT = -1;
	
	private String name = null;
	private String type = null;
	private String description = null;
	private String version = null;
	private String hostAddress = null;
	private String hostName = null;
	private int port = P4D_ZEROCONF_NOPORT;
	
	/**
	 * Default constructor -- sets all fields to null except
	 * port, which is set to P4D_ZEROCONF_NOPORT.
	 */
	public ZeroconfServerInfo() {
	}

	/**
	 * Explicit-value constructor.
	 */
	public ZeroconfServerInfo(String name, String type, String description,
			String version, String hostAddress, String hostName, int port) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.version = version;
		this.hostAddress = hostAddress;
		// Host name is derived from the getServer() method on the JmDNS service interface;
		// we need to remove the "." at the end of the string if it exists so it can be a
		// usable as-is name: 
		this.hostName = hostName;
		if ((hostName != null) && hostName.endsWith(".") && (hostName.length() > 1)) {
			this.hostName = hostName.substring(0, hostName.lastIndexOf("."));
		} else {
			this.hostName = hostName;
		}
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getHostAddress() {
		return hostAddress;
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Construct a nice string representation, with the server's address
	 * presented as a P4Java URI. Mostly intended for debugging.
	 */
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		if (getName() != null) {
			strBuilder.append("Server name: " + getName());
			strBuilder.append(";");
		}
		if (getHostAddress() != null) {
			strBuilder.append(" URI: p4java://" + getHostName());
			strBuilder.append(":" + getPort());
			strBuilder.append(" (" + getHostAddress() + ")");
		}
		
		if (getDescription() != null) {
			strBuilder.append(" Description: " + getDescription());
			strBuilder.append(";");
		}
		
		if (getVersion() != null) {
			strBuilder.append(" Version: " + getVersion());
			strBuilder.append(";");
		}

		return strBuilder.toString();
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
}
