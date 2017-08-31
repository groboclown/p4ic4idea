package com.perforce.p4java.server.delegator;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.admin.ServerConfigurationValue;
import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface for a ConfigureDelegator implementation.
 */
public interface IConfigureDelegator {
    /**
     * Set or unset a specific names server configuration variable. Config
     * variables are unset by passing in a null value parameter.
     * <p>
     *
     * Expected variable name formats are as specified in the main Perforce
     * documentation: [servername + #] variablename -- but this is not enforced
     * by P4Java itself.
     * <p>
     *
     * Note: you must be an admin or super user for this command to work.
     *
     * @param name
     *            non-null config variable name.
     * @param value
     *            if null, unset the named variable; otherwise, set it to the
     *            passed-in string value.
     * @return possibly-empty operation status string returned by the server in
     *         response to this set / unset attempt.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2011.1
     */
    String setOrUnsetServerConfigurationValue(
            @Nonnull String name,
            @Nullable String value) throws P4JavaException;
    
    /**
     * Show server configuration values. See the main Perforce documentation for
     * the details of this admin command, but note that only one of serverName
     * or variableName should be non-null (they can both be null, which means
     * ignore them both). If they're both null, serverName currently takes
     * precedence, but that's not guaranteed.
     * <p>
     *
     * Note: you must be an admin or super user for this command to work.
     *
     * @param serverName
     *            if not null, only show values associated with the named
     *            server; if equals ServerConfigurationValue.ALL_SERVERS, show
     *            values associated with all participating servers.
     * @param variableName
     *            if not null, only show the value of this named config
     *            variable.
     * @return non-null (but possibly-empty) list of qualifying
     *         ServerConfigurationValue objects.
     * @throws P4JavaException
     *             if an error occurs processing this method and its parameters.
     * @since 2011.1
     */
    List<ServerConfigurationValue> showServerConfiguration(
            String serverName,
            String variableName) throws P4JavaException;
}
