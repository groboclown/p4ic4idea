package com.perforce.p4java.admin;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.MapKeys.NAME_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.SERVER_NAME_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.TYPE_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.VALUE_KEY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;

/**
 * Encapsulates the server configuration values that are available through the
 * IOptionsServer set/get server configuration methods.
 * <p>
 *
 * This class is complicated by the fact that any or all of the fields may be
 * set or null, and by the way that the same config name may have several
 * different ServerConfigurationValue objects depending on how the variable has
 * been set. As this class is intended for advanced admin use only, none of the
 * fields are documented in any detail here -- see the main Perforce
 * documentation for the 'configure' operation for a full explanation.
 * <p>
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
     * Currently-known server configuration value types. Deliberately not
     * explained here -- see the main Perforce admin documentation.
     */
    public enum ConfigType {
        
        /** The default. */
        DEFAULT, 
        /** The unknown. */
        UNKNOWN, 
        /** The option. */
        OPTION, 
        /** The environment. */
        ENVIRONMENT, 
        /** The tunable. */
        TUNABLE,
        /** The configure. */
        CONFIGURE;

        /**
         * Tolerant case-insensitive version of fromString.
         *
         * @param str
         *            possibly-null candidate string.
         * @return corresponding ConfigType, or UNKNOWN if str was null or
         *         didn't seem to correspond to any known config type.
         */
        public static ConfigType fromString(final String str) {
            if (isNotBlank(str)) {
                for (ConfigType configType : ConfigType.values()) {
                    if (str.toUpperCase().startsWith(configType.toString())) {
                        return configType;
                    }
                }
            }

            return UNKNOWN;
        }
    }

    /** The server name. */
    private String serverName = null;
    
    /** The type. */
    private ConfigType type = ConfigType.UNKNOWN;
    
    /** The name. */
    private String name = null;
    
    /** The value. */
    private String value = null;

    /**
     * Default constructor -- all fields set to null except type, which is set
     * to ConfigType.UNKNOWN.
     */
    public ServerConfigurationValue() {
    }

    /**
     * Explicit value constructor.
     *
     * @param serverName the server name
     * @param type the type
     * @param name the name
     * @param value the value
     */
    public ServerConfigurationValue(final String serverName, final ConfigType type,
            final String name, final String value) {

        this.serverName = serverName;
        this.type = type;
        this.name = name;
        this.value = value;
    }

    /**
     * Explicit value convenience constructor with string 'type' field, which is
     * converted to ConfigType on the fly.
     *
     * @param serverName the server name
     * @param type the type
     * @param name the name
     * @param value the value
     */
    public ServerConfigurationValue(final String serverName, final String type, final String name,
            final String value) {

        this.serverName = serverName;
        this.type = ConfigType.fromString(type);
        this.name = name;
        this.value = value;
    }

    /**
     * Constructor for map-based returns from the server; not intended for
     * general use.
     *
     * @param map the map
     */
    public ServerConfigurationValue(final Map<String, Object> map) {
        if (nonNull(map)) {
            name = parseString(map, NAME_KEY);
            value = parseString(map, VALUE_KEY);
            serverName = parseString(map, SERVER_NAME_KEY);
            type = ConfigType.fromString(parseString(map, TYPE_KEY));
        }
    }

    /**
     * Gets the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the server name.
     *
     * @param serverName the server name
     * @return the server configuration value
     */
    public ServerConfigurationValue setServerName(final String serverName) {
        this.serverName = serverName;
        return this;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public ConfigType getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type
     * @return the server configuration value
     */
    public ServerConfigurationValue setType(final ConfigType type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name
     * @return the server configuration value
     */
    public ServerConfigurationValue setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value
     * @return the server configuration value
     */
    public ServerConfigurationValue setValue(final String value) {
        this.value = value;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Server<" + serverName + ">, " + "Type<" + type + ">, " + "Name<" + name + ">, "
                + "Value<" + value + ">.";
    }

}
