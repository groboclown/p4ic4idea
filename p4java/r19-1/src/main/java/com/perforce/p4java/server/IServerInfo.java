/**
 * 
 */
package com.perforce.p4java.server;

import java.util.Calendar;

/**
 * Provides a snapshot onto what the Perforce server knows about both
 * itself and the Perforce client.<p>
 * 
 * Semantics for the methods below are intended to the same as documented
 * elsewhere for the output of the p4 'info' command.<p>
 * 
 * Note that individual methods defined below are <b>not</b> guaranteed to return
 * non-null or even meaningful results.<p>
 * 
 * Note also that the values returned here are what the Perforce server
 * believes is current and accurate; these may not always be exactly the
 * same as what the client itself believes.
 */

public interface IServerInfo {
	String getUserName();
	String getClientName();
	String getClientRoot();
	String getClientHost();
	String getClientAddress();
	String getClientCurrentDirectory();
	
	String getPeerAddress();

	String getServerAddress();
	String getServerRoot();
	String getServerDate();
	Calendar getServerCalendar();
	String getServerUptime();
	String getServerVersion();
	String getServerLicense();
	String getServerLicenseIp();
	boolean isServerEncrypted();
	String getServerId();
	String getServerCluster();
	
	String getProxyVersion();
	String getProxyAddress();
	String getProxyRoot();
	boolean isProxyEncrypted();
	
	String getBrokerVersion();
	String getBrokerAddress();
	boolean isBrokerEncrypted();
	
	String getSandboxVersion();
	String getSandboxPort();
	
	String getIntegEngine();
	
	boolean isPasswordEnabled();
	boolean isCaseSensitive();
	boolean isUnicodeEnabled();
	boolean isMonitorEnabled();
	boolean isMoveDisabled();
	boolean isEncrypted();

	String getSSOAuth();
	Boolean isSSOAuthRequired();
}
