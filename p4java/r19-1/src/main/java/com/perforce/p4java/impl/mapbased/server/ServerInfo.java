/**
 * 
 */
package com.perforce.p4java.impl.mapbased.server;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.server.IServerInfo;

/**
 * Default simple implementation for server info interface.
 */

public class ServerInfo implements IServerInfo {

	// Perforce server info date pattern with time zone
	// Example: "2014/06/05 17:28:14 -0700 PDT"
	public static final String SERVER_INFO_DATE_PATTERN = "yyyy/MM/dd HH:mm:ss Z z";
	
	private String clientName = null;
	private String clientHost = null;
	private String clientRoot = null;
	private String clientAddress = null;
	private String clientCurrentDirectory = null;
	private String peerAddress = null;
	private String serverAddress = null;
	private String serverDate = null;
	private Calendar serverCalendar = null;
	private String serverLicense = null;
	private String serverRoot = null;
	private String serverUptime = null;
	private String serverVersion = null;
	private String serverLicenseIp = null;
	private boolean serverEncrypted = false;
	private String serverId = null;
	private String serverCluster = null;
	
	private String replica = null;
	private boolean passwordEnabled = false;
	private boolean caseSensitive = false;
	private boolean monitorEnabled = false;
	private boolean unicodeEnabled = false;
	private boolean moveDisabled = false;
	private String proxyVersion = null;
	private String proxyAddress = null;
	private String proxyRoot = null;
	private boolean proxyEncrypted = false;
	private String brokerVersion = null;
	private String brokerAddress = null;
	private boolean brokerEncrypted = false;
	private String sandboxVersion = null;
	private String sandboxPort = null;

	private String userName = null;
	private String integEngine = null;
	private String ssoAuth = null;

	/**
	 * Default constructor; leaves all fields initialized to null.
	 */
	public ServerInfo() {
	}

	/**
	 * Explicit-value all-fields constructor.
	 * 
	 * @deprecated Use constructor with map parameter to initialize all fields.
	 */
	@Deprecated
	public ServerInfo(String clientName, String clientHost, String clientRoot,
			String clientAddress, String clientCurrentDirectory,
			String serverAddress, String serverDate, String serverLicense,
			String serverRoot, String serverUptime, String serverVersion,
			String serverLicenseIp, String proxyVersion, String userName,
			boolean unicodeEnabled, boolean monitorEnabled) {
		this.clientName = clientName;
		this.clientHost = clientHost;
		this.clientRoot = clientRoot;
		this.clientAddress = clientAddress;
		this.clientCurrentDirectory = clientCurrentDirectory;
		this.serverAddress = serverAddress;
		this.serverDate = serverDate;
		this.serverLicense = serverLicense;
		this.serverRoot = serverRoot;
		this.serverUptime = serverUptime;
		this.serverVersion = serverVersion;
		this.serverLicenseIp = serverLicenseIp;
		this.proxyVersion = proxyVersion;
		this.userName = userName;
		this.unicodeEnabled = unicodeEnabled;
		this.monitorEnabled = monitorEnabled;
	}

	/**
	 * Constructor for use with maps passed back from the Perforce server only.
	 * 
	 * When a broker is involved, there will be more than one map.
	 */
	public ServerInfo(List<Map<String, Object>> maps) {
		for (Map<String, Object> map : maps) {
			setFromMap(map);
		}
	}

	/**
	 * Constructor for use with maps passed back from the Perforce server only.
	 */
	public ServerInfo(Map<String, Object> map) {
		if (map != null) {
			setFromMap(map);
		}
	}

	/**
	 * add any fields that are set in map; don't clear fields that are missing from map
	 */
	private void setFromMap(Map<String, Object> map) {
		this.userName = setFromMap(map, "userName", this.userName);
		this.clientCurrentDirectory = setFromMap(map, "clientCwd", this.clientCurrentDirectory);
		this.clientName = setFromMap(map, "clientName", this.clientName);
		this.clientRoot = setFromMap(map, "clientRoot", this.clientRoot);
		this.clientHost = setFromMap(map, "clientHost", this.clientHost);
		this.clientAddress = setFromMap(map, "clientAddress", this.clientAddress);
		
		this.serverAddress = setFromMap(map, "serverAddress", this.serverAddress);
		this.serverDate = setFromMap(map, "serverDate", this.serverDate);
		this.serverLicense = setFromMap(map, "serverLicense", this.serverLicense);
		this.serverRoot = setFromMap(map, "serverRoot", this.serverRoot);
		this.serverUptime = setFromMap(map, "serverUptime", this.serverUptime);
		this.serverVersion = setFromMap(map, "serverVersion", this.serverVersion);
		this.serverLicenseIp = setFromMap(map, "serverLicense-ip", this.serverLicenseIp);
		this.serverId = setFromMap(map, "ServerID", this.serverId);
		this.serverCluster = setFromMap(map, "serverCluster", this.serverCluster);
		
		this.replica = setFromMap(map, "replica", this.replica);

		this.proxyVersion = setFromMap(map, "proxyVersion", this.proxyVersion);
		this.proxyAddress = setFromMap(map, "proxyAddress", this.proxyAddress);
		this.proxyRoot = setFromMap(map, "proxyRoot", this.proxyRoot);
		
		this.brokerVersion = setFromMap(map, "brokerVersion", this.brokerVersion);
		this.brokerAddress = setFromMap(map, "brokerAddress", this.brokerAddress);
		this.sandboxVersion = setFromMap(map, "p4sandboxBrokerVersion", this.sandboxVersion);
		this.sandboxPort = setFromMap(map, "p4sandboxBrokerPort", this.sandboxPort);

		this.integEngine = setFromMap(map, "integEngine", this.integEngine);
		this.ssoAuth = setFromMap(map, "ssoAuth", this.ssoAuth);

		if (map.get("serverDate") != null) {
            DateFormat df = new SimpleDateFormat(SERVER_INFO_DATE_PATTERN);
            try {
				Date d = df.parse((String)map.get("serverDate"));
				if (d != null) {
					this.serverCalendar = Calendar.getInstance();
					this.serverCalendar.setTime(d);
				}
			} catch (ParseException e) {
				// ignore for now...
			}
		}

		if (map.containsKey("serverEncryption")
				&& ((String) map.get("serverEncryption")).equalsIgnoreCase("encrypted")) {
			this.serverEncrypted = true;
		}
		if (map.containsKey("proxyEncryption")
				&& ((String) map.get("proxyEncryption")).equalsIgnoreCase("encrypted")) {
			this.proxyEncrypted = true;
		}
		if (map.containsKey("brokerEncryption")
				&& ((String) map.get("brokerEncryption")).equalsIgnoreCase("encrypted")) {
			this.brokerEncrypted = true;
		}
		if (map.containsKey("password")
				&& ((String) map.get("password")).equalsIgnoreCase("enabled")) {
			this.passwordEnabled = true;
		}
		if (map.containsKey("caseHandling")
				&& ((String) map.get("caseHandling")).equalsIgnoreCase("sensitive")) {
			this.caseSensitive = true;
		}
		if (map.containsKey("unicode")
				&& ((String) map.get("unicode")).equalsIgnoreCase("enabled")) {
			this.unicodeEnabled = true;
		}
		if (map.containsKey("monitor")
				&& ((String) map.get("monitor")).equalsIgnoreCase("enabled")) {
			this.monitorEnabled = true;
		}
		if (map.containsKey("move")
				&& ((String) map.get("move")).equalsIgnoreCase("disabled")) {
			this.moveDisabled = true;
		}
	}
	
	private String setFromMap(Map<String, Object> map, String key, String defaultValue) {
		if (map.containsKey(key))
			return (String)map.get(key);
		else
			return defaultValue;
	}
	
	public String getServerAddress() {
		return this.serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public String getServerDate() {
		return this.serverDate;
	}

	public void setServerDate(String serverDate) {
		this.serverDate = serverDate;
	}

	public Calendar getServerCalendar() {
		return this.serverCalendar;
	}

	public void setServerCalendar(Calendar serverCalendar) {
		this.serverCalendar = serverCalendar;
	}

	public String getServerLicense() {
		return this.serverLicense;
	}

	public void setServerLicense(String serverLicense) {
		this.serverLicense = serverLicense;
	}

	public String getServerRoot() {
		return this.serverRoot;
	}

	public void setServerRoot(String serverRoot) {
		this.serverRoot = serverRoot;
	}

	public String getServerUptime() {
		return this.serverUptime;
	}

	public void setServerUptime(String serverUptime) {
		this.serverUptime = serverUptime;
	}

	public String getServerVersion() {
		return this.serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}

	public String getClientName() {
		return this.clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getClientHost() {
		return this.clientHost;
	}

	public void setClientHost(String clientHost) {
		this.clientHost = clientHost;
	}

	public String getClientRoot() {
		return this.clientRoot;
	}

	public void setClientRoot(String clientRoot) {
		this.clientRoot = clientRoot;
	}

	public String getClientAddress() {
		return this.clientAddress;
	}

	public void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress;
	}

	public String getPeerAddress() {
		return this.peerAddress;
	}

	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}

	public String getClientCurrentDirectory() {
		return this.clientCurrentDirectory;
	}

	public void setClientCurrentDirectory(String currentDirectory) {
		this.clientCurrentDirectory = currentDirectory;
	}

	public String getServerLicenseIp() {
		return this.serverLicenseIp;
	}

	public void setServerLicenseIp(String serverLicenseIp) {
		this.serverLicenseIp = serverLicenseIp;
	}

	public boolean isServerEncrypted() {
		return this.serverEncrypted;
	}

	public void setServerEncrypted(boolean serverEncrypted) {
		this.serverEncrypted = serverEncrypted;
	}

	public String getReplica() {
		return this.replica;
	}

	public void setReplica(String replica) {
		this.replica = replica;
	}

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public boolean isPasswordEnabled() {
		return this.passwordEnabled;
	}

	public void setPasswordEnabled(boolean passwordEnabled) {
		this.passwordEnabled = passwordEnabled;
	}

	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public boolean isMonitorEnabled() {
		return this.monitorEnabled;
	}

	public void setMonitorEnabled(boolean monitorEnabled) {
		this.monitorEnabled = monitorEnabled;
	}

	public boolean isUnicodeEnabled() {
		return this.unicodeEnabled;
	}

	public void setUnicodeEnabled(boolean unicodeEnabled) {
		this.unicodeEnabled = unicodeEnabled;
	}

	public boolean isMoveDisabled() {
		return this.moveDisabled;
	}

	public void setMoveDisabled(boolean moveDisabled) {
		this.moveDisabled = moveDisabled;
	}

	public String getProxyVersion() {
		return this.proxyVersion;
	}

	public void setProxyVersion(String proxyVersion) {
		this.proxyVersion = proxyVersion;
	}

	public String getProxyAddress() {
		return this.proxyAddress;
	}

	public void setProxyAddress(String proxyAddress) {
		this.proxyAddress = proxyAddress;
	}

	public String getProxyRoot() {
		return this.proxyRoot;
	}

	public void setProxyRoot(String proxyRoot) {
		this.proxyRoot = proxyRoot;
	}

	public boolean isProxyEncrypted() {
		return this.proxyEncrypted;
	}

	public void setProxyEncrypted(boolean proxyEncrypted) {
		this.proxyEncrypted = proxyEncrypted;
	}

	public String getBrokerVersion() {
		return this.brokerVersion;
	}

	public void setBrokerVersion(String brokerVersion) {
		this.brokerVersion = brokerVersion;
	}

	public String getBrokerAddress() {
		return this.brokerAddress;
	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}

	public boolean isBrokerEncrypted() {
		return this.brokerEncrypted;
	}

	public void setBrokerEncrypted(boolean brokerEncrypted) {
		this.brokerEncrypted = brokerEncrypted;
	}

	public String getSandboxVersion() {
		return this.sandboxVersion;
	}

	public void setSandboxVersion(String sandboxVersion) {
		this.sandboxVersion = sandboxVersion;
	}

	public String getSandboxPort() {
		return this.sandboxPort;
	}

	public void setSandboxPort(String sandboxPort) {
		this.sandboxPort = sandboxPort;
	}

	public boolean isEncrypted() {
		return (this.serverEncrypted ||
				this.proxyEncrypted ||
				this.brokerEncrypted);
	}
	
	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getServerCluster() {
		return serverCluster;
	}

	public void setServerCluster(String serverCluster) {
		this.serverCluster = serverCluster;
	}

	public String getIntegEngine() {
		return integEngine;
	}

	public void setIntegEngine(String integEngine) {
		this.integEngine = integEngine;
	}

	public String getSSOAuth() {
		return ssoAuth;
	}

	public void setSSOAuth(String ssoAuth) {
		this.ssoAuth = ssoAuth;
	}

	public Boolean isSSOAuthRequired() {
		return "required".equals(ssoAuth);
	}
}
