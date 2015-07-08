/**
 * 
 */
package com.perforce.p4java.admin;

import java.util.Map;

/**
 * Encapsulates the server configuration values that are available through
 * the IOptionsServer set/get server configuration methods.<p>
 * 
 * This class is complicated by the fact that any or all of the fields
 * may be set or null, and by the way that the same config name may have
 * several different ServerConfigurationValue objects depending on how
 * the variable has been set. As this class is intended for advanced
 * admin use only, none of the fields are documented in any detail here
 * -- see the main Perforce documentation for the 'configure' operation
 * for a full explanation.<p>
 * 
 * @since 2011.1
 */
public class ServerConfigurationValue {
	
	/**
	 * Special string value used to signal that the corresponding config value
	 * is available to (or defined for) all participating servers.
	 */
	public static final String ALL_SERVERS = "allservers";
	
	/**
	 * Currently-known server configuration value types. Deliberately
	 * not explained here -- see the main Perforce admin documentation.
	 */
	public static enum ConfigType {
		UNKNOWN,
		OPTION,
		ENVIRONMENT,
		TUNABLE,
		CONFIGURE;
		
		/**
		 * Tolerant case-insensitive version of fromString.
		 *  
		 * @param str possibly-null candidate string.
		 * @return corresponding ConfigType, or UNKNOWN if str was null
		 * 			or didn't seem to correspond to any known config type.
		 */
		public static ConfigType fromString(String str) {
			if (str != null) {
				for (ConfigType configType : ConfigType.values()) {
					if (configType.toString().equalsIgnoreCase(str)) {
						return configType;
					}
				}
			}
			
			return UNKNOWN;
		}
	};
	
	private String serverName = null;
	private ConfigType type = ConfigType.UNKNOWN;
	private String name = null;
	private String value = null;
	
	/**
	 * Default constructor -- all fields set to null except type, which
	 * is set to ConfigType.UNKNOWN.
	 */
	public ServerConfigurationValue() {
	}
	
	/**
	 * Explicit value constructor.
	 */
	public ServerConfigurationValue(String serverName, ConfigType type,
			String name, String value) {
		this.serverName = serverName;
		this.type = type;
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Explicit value convenience constructor with string 'type' field, which
	 * is converted to ConfigType on the fly.
	 */
	public ServerConfigurationValue(String serverName, String type,
			String name, String value) {
		this.serverName = serverName;
		this.type = ConfigType.fromString(type);
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Constructor for map-based returns from the server; not intended for
	 * general use.
	 */
	public ServerConfigurationValue(Map<String, Object> map) {
		if (map != null) {
			this.name = (String) map.get("Name");
			this.value = (String) map.get("Value");
			this.serverName = (String) map.get("ServerName");
			this.type = ConfigType.fromString((String) map.get("Type"));
		}
	}
	
	public String getServerName() {
		return serverName;
	}
	public ServerConfigurationValue setServerName(String serverName) {
		this.serverName = serverName;
		return this;
	}
	public ConfigType getType() {
		return type;
	}
	public ServerConfigurationValue setType(ConfigType type) {
		this.type = type;
		return this;
	}
	public String getName() {
		return name;
	}
	public ServerConfigurationValue setName(String name) {
		this.name = name;
		return this;
	}
	public String getValue() {
		return value;
	}
	public ServerConfigurationValue setValue(String value) {
		this.value = value;
		return this;
	}
}
