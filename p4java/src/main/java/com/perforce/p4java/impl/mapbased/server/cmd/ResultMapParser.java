package com.perforce.p4java.impl.mapbased.server.cmd;

// p4ic4idea: Massive changes to return IServerMessage instances instead of Strings.
import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.AuthenticationFailedException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.impl.mapbased.rpc.msg.ServerMessage;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.IServerMessage;
import com.perforce.p4java.server.ISingleServerMessage;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.exception.MessageSeverityCode.E_INFO;
import static com.perforce.p4java.exception.MessageSeverityCode.E_WARN;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.COMMIT;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TREE;
import static com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage.CODE;
import static com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage.getSeverity;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Utility to parse a result map and test for info/error messages etc.
 * p4ic4idea: will eventually replace this with an object that will be
 * passed around instead of the Map<String, Object> and the
 * List<Map<String, Object>>.
 */
public abstract class ResultMapParser {

	/**
	 * Default size to use creating string builders.
	 */
	private static final int INITIAL_STRING_BUILDER = 100;
	/**
	 * Signals access (login) needed.
	 */
	protected static final String CORE_AUTH_FAIL_STRING_1 = "Perforce password (P4PASSWD)";
	/**
	 * Signals access (login) needed.
	 */
	protected static final String CORE_AUTH_FAIL_STRING_2 = "Access for user";
	/**
	 * Signals ticket has expired.
	 */
	protected static final String CORE_AUTH_FAIL_STRING_3 = "Your session has expired";
	/**
	 * SSO failure.
	 */
	private static final String AUTH_FAIL_STRING_1 = "Single sign-on on client failed";
	/**
	 * Password failure error.
	 */
	private static final String AUTH_FAIL_STRING_2 = "Password invalid";
	/**
	 * Signals ticket has expired.
	 */
	protected static final String CORE_AUTH_FAIL_STRING_4 = "Your session was logged out";
    /**
     * p4ic4idea: additional error message
     * Signals access (login) needed
     */
    private static final String CORE_AUTH_FAIL_STRING_5 = "Perforce password (%'P4PASSWD'%)";

	/**
	 * Array of access error messages.
	 */
    // p4ic4idea: map the error type to the string; augmented
    private static final String[] ACCESS_ERR_MSGS = {
            CORE_AUTH_FAIL_STRING_1,
            CORE_AUTH_FAIL_STRING_2,
            CORE_AUTH_FAIL_STRING_3,
            CORE_AUTH_FAIL_STRING_4,
            CORE_AUTH_FAIL_STRING_5,
            AUTH_FAIL_STRING_1,
            AUTH_FAIL_STRING_2
    };

    // p4ic4idea: access error types; 1-for-1 with the ACCESS_ERR_MSG list.
    private static final AuthenticationFailedException.ErrorType[] ACCESS_ERR_TYPES = {
            // each index maps to the error message
            AuthenticationFailedException.ErrorType.NOT_LOGGED_IN,
            AuthenticationFailedException.ErrorType.NOT_LOGGED_IN,
            AuthenticationFailedException.ErrorType.SESSION_EXPIRED,
            AuthenticationFailedException.ErrorType.SESSION_EXPIRED,
            AuthenticationFailedException.ErrorType.NOT_LOGGED_IN,
            AuthenticationFailedException.ErrorType.SSO_LOGIN,
            AuthenticationFailedException.ErrorType.PASSWORD_INVALID
    };

	/**
	 * Parses the command result map to return a String of info messages. The
	 * messages are expected to be info and if they are not (i.e. they are error
	 * messages) an exception is thrown.
	 *
	 * @param resultMaps the result maps
	 * @return the string
	 * @throws AccessException  the access exception
	 * @throws RequestException the request exception
	 */
	// p4ic4idea: return IServerMessage instead
	public static String parseCommandResultMapIfIsInfoMessageAsString(
			@Nonnull final List<Map<String, Object>> resultMaps)
			throws AccessException, RequestException {
		IServerMessage msg = toServerMessage(resultMaps);
		handleErrors(msg);
		if (nonNull(msg)) {
		    return msg.getAllInfoStrings();
		}
		return EMPTY;
	}

	/**
	 * Checks for a info message.
	 *
	 * @param map the map
	 * @return true, if info message
     * @deprecated p4ic4idea {@link IServerMessage#isInfo()}
	 */
	// p4ic4idea: deprecated
	public static boolean isInfoMessage(final Map<String, Object> map) {
		return nonNull(map) && (getSeverity(parseCode0ErrorString(map)) == E_INFO);
	}

	/**
	 * Checks for a warning message.
	 *
	 * @param map the map
	 * @return true, if warning message
	 */
	public static boolean isWarningMessage(final Map<String, Object> map) {
		return nonNull(map) && (getSeverity(parseCode0ErrorString(map)) == E_WARN);
	}

    /**
     *
     * @param message IServerMessage
     * @throws RequestException for non-access errors
     * @throws AccessException for access errors
     */
	public static void handleErrors(@Nullable IServerMessage message)
            throws RequestException, AccessException {
	    if (nonNull(message) && message.isError()) {
            AuthenticationFailedException.ErrorType type = getAuthFailType(message);
            if (nonNull(type)) {
                throw new AccessException(message);
            } else {
                throw new RequestException(message);
            }
        }
    }

	/**
	 * Tests the map for warnings and throws an exception if found.
	 *
	 * @param message the map
	 * @return true, if successful
	 * @throws RequestException the request exception
	 * @throws AccessException  the access exception
	 */
	public static void handleWarnings(@Nullable final IServerMessage message)
			throws RequestException {
		if (nonNull(message) && message.isWarning()) {
			throw new RequestException(message);
		}
	}

	/**
	 * Tests the map for errors and throws an exception if found.
	 *
	 * @param map the map
	 * @return true, if successful
	 * @throws RequestException the request exception
	 * @throws AccessException  the access exception
     * @deprecated p4ic4idea: use IServerMessage form instead.
	 */
	public static boolean handleErrorStr(final Map<String, Object> map)
			throws RequestException, AccessException {
	    handleErrors(toServerMessage(map));
	    return false;
	}

	/**
	 * RPC impl errors come across the wire as a map in the form usually like
	 * this:
	 * <p>
	 * <pre>
	 * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
	 * func=client-Message, user=nouser, code0=822483067
	 * </pre>
	 * <p>
	 * With tags being used for non-error payloads, we can just basically pick
	 * up the presence of the code0 entry; if it's there, use fmt0 as the format
	 * and the other args as appropriate...
	 * <p>
	 * <p>
	 * FIXME: work with multiple code/fmt sets... -- HR.
	 *
	 * @param map the map
	 * @return the error string if found.
	 */
	// p4ic4idea: return IServerMessage
	public static IServerMessage getErrorStr(final Map<String, Object> map) {
		return getServerMessage(map, E_FAILED);
	}

	// p4ic4idea: return IServerMessage
	public static IServerMessage getWarningStr(final Map<String, Object> map) {
		return getServerMessage(map, E_WARN);
	}

	/**
	 * Checks to see if an error String is as a result of an auth fail.
	 *
     * @deprecated p4ic4idea: use {@link #getAuthFailType(IServerMessage)} instead
	 * @param errStr the err str
	 * @return true, if is auth fail
	 */
	public static boolean isAuthFail(final String errStr) {
		if (isNotBlank(errStr)) {
			for (String str : ACCESS_ERR_MSGS) {
				if (contains(errStr, str)) {
					return true;
				}
			}
		}

		return false;
	}

    public static AuthenticationFailedException.ErrorType getAuthFailType(IServerMessage err) {
        if (nonNull(err)) {
            // p4ic4idea: TODO this needs to check the error code instead of the message,
            // in case the user sets the language.
            for (int i = 0; i < ACCESS_ERR_MSGS.length; i++) {
                if (err.hasMessageFragment(ACCESS_ERR_MSGS[i])) {
                    return ACCESS_ERR_TYPES[i];
                }
            }
        }

        return null;
    }

	/**
	 * Gets the info message from the passed-in Perforce command results map. If
	 * no info message found in the results map it returns null.
	 * <p>
	 * </p>
	 * <p>
	 * Note that the severity code is MessageSeverityCode.E_INFO. Therefore,
	 * only message with severity code = MessageSeverityCode.E_INFO will be
	 * returned.
	 * <p>
	 * <p>
	 * RPC impl errors come across the wire as a map in the form usually like
	 * this:
	 * <p>
	 * <pre>
	 * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
	 * func=client-Message, user=nouser, code0=822483067
	 * </pre>
	 * <p>
	 * Note that the code0 entry will be used to get the severity level; the
	 * fmt0 entry contains the message.
	 * <p>
	 *
	 * @param map Perforce command results map
	 * @return possibly-null info string
	 * @since 2011.2
     * @deprecated {@link #toServerMessage(Map)} ; {@link IServerMessage#getAllInfoStrings()}
	 */
	@Nullable
	public static String getInfoStr(@Nullable final Map<String, Object> map) {
		if (nonNull(map)) {
		    IServerMessage msg = toServerMessage(map);
		    if (nonNull(msg)) {
                String ret = msg.getAllInfoStrings();
                if (! ret.isEmpty()) {
                	return ret;
				}
            }
		}

		return null;
	}

    /**
     * Returns all Info messages.  Any error messages are handled as exceptions.
     *
     * @param map command results map
     * @return possibly-null info and warning messages.
     */
	@Nullable
    public static IServerMessage getHandledErrorInfo(@Nullable final Map<String, Object> map)
            throws AccessException, RequestException {
	    if (nonNull(map)) {
	        IServerMessage message = getErrorOrInfoStr(map);
	        if (nonNull(message)) {
				Iterable<ISingleServerMessage> errors = message.getForSeverity(E_FAILED);
				if (errors.iterator().hasNext()) {
					IServerMessage errMsg = new ServerMessage(errors);
					AuthenticationFailedException.ErrorType authFailType = getAuthFailType(errMsg);
					if (nonNull(authFailType)) {
						throw new AccessException(errMsg);
					} else {
						throw new RequestException(errMsg);
					}
				}
			}
            return message;
        }
        return null;
    }

	/**
	 * Gets the info/warning/error/fatal message from the passed-in Perforce
	 * command results map. If no info/warning/error/fatal message found in the
	 * results map it returns null.
	 * <p>
	 * </p>
	 * <p>
	 * Note that the minimum severity code is MessageSeverityCode.E_INFO.
	 * Therefore, only message with severity code >= MessageSeverityCode.E_INFO
	 * will be returned.
	 * <p>
	 * <p>
	 * RPC impl errors come across the wire as a map in the form usually like
	 * this:
	 * <p>
	 * <pre>
	 * fmt0=Access for user '%user%' has not been enabled by 'p4 protect'.,
	 * func=client-Message, user=nouser, code0=822483067
	 * </pre>
	 * <p>
	 * Note that the code0 entry will be used to get the severity level; the
	 * fmt0 entry contains the message.
	 * <p>
	 *
	 * @param map Perforce command results map
	 * @return possibly-null info/warning/error/fatal string
	 * @since 2011.2
     */
	public static IServerMessage getErrorOrInfoStr(final Map<String, Object> map) {
	    return getServerMessage(map, E_INFO);
	}

	/**
	 * Throw request exception if error message found.
	 *
	 * @param map the map
	 * @throws RequestException the request exception
	 */
	public static void throwRequestExceptionIfErrorMessageFound(final Map<String, Object> map)
			throws RequestException {
        IServerMessage message = getErrorStr(map);
		if (nonNull(message)) {
			throw new RequestException(message);
		}
	}

	/**
	 * Handle error or info str.
	 *
	 * @param map the map
	 * @return true, if successful
	 * @throws RequestException the request exception
	 * @throws AccessException  the access exception
	 */
	public static boolean handleErrorOrInfoStr(final Map<String, Object> map)
			throws RequestException, AccessException {
        IServerMessage err = getErrorOrInfoStr(map);

		if (nonNull(err)) {
            AuthenticationFailedException.ErrorType authFailType = getAuthFailType(err);
			if (nonNull(authFailType)) {
				throw new AccessException(err);
			} else {
				throw new RequestException(err);
			}
		}
		return false;
	}

	/**
	 * Parses the command result map as string.
	 *
	 * @param resultMaps the result maps
	 * @return the string
	 * @throws AccessException  the access exception
	 * @throws RequestException the request exception
	 */
	public static String parseCommandResultMapAsString(
			@Nonnull final List<Map<String, Object>> resultMaps)
			throws AccessException, RequestException {
		StringBuilder retVal = new StringBuilder();
		if (nonNull(resultMaps)) {
			for (Map<String, Object> map : resultMaps) {
                IServerMessage message = toServerMessage(map);
                if (nonNull(message)) {
                    handleErrors(message);
                    String info = message.getAllInfoStrings();
                    if (isNotBlank(info)) {
                        if (retVal.length() > 0) {
                            retVal.append("\n");
                        }
                        retVal.append(info);
                    }
                }
			}
		} else {
			Log.warn("Null map array is returned when execute Helix command");
		}

		return retVal.toString();
	}

	/**
	 * Parses the command result map as file specs.
	 *
     * p4ic4idea: change id description
	 * @param id         the changelist id
	 * @param server     the server
	 * @param resultMaps the result maps
	 * @return the list
	 */
	public static List<IFileSpec> parseCommandResultMapAsFileSpecs(final int id,
	                                                               final IServer server, final List<Map<String, Object>> resultMaps) {

		List<IFileSpec> fileList = new ArrayList<>();
		if (nonNull(resultMaps)) {
			// NOTE: all the results are returned in *one* map, not an array of
			// them...
			if (!resultMaps.isEmpty() && nonNull(resultMaps.get(0))) {
				Map<String, Object> map = resultMaps.get(0);

				for (int i = 0; nonNull(map.get(REV + i)); i++) {
					FileSpec fSpec = new FileSpec(map, server, i);
					fSpec.setChangelistId(id);
					fileList.add(fSpec);
				}
			}
		}
		return fileList;
	}

	/**
	 * Parses the graph command result map as file specs.
	 *
	 * @param server     the server
	 * @param resultMaps the result maps
	 * @return the list
	 */
	public static List<IFileSpec> parseGraphCommandResultMapAsFileSpecs(final IServer server, final List<Map<String, Object>> resultMaps) {

		List<IFileSpec> fileList = new ArrayList<>();
		if (nonNull(resultMaps)) {
			// NOTE: all the results are returned in *one* map, not an array of
			// them...
			if (!resultMaps.isEmpty() && nonNull(resultMaps.get(0))) {
				Map<String, Object> map = resultMaps.get(0);
				for (int i = 0; nonNull(map.get(DEPOT_FILE + i)); i++) {
					FileSpec fSpec = new FileSpec(map, server, i);
					fSpec.setCommitSha(parseString(map, COMMIT));
					fSpec.setTreeSha(parseString(map, TREE));
					fileList.add(fSpec);
				}
			}
		}
		return fileList;
	}

	/**
	 * Handle file error str.
	 *
	 * @param map the map
	 * @return the string
	 * @throws ConnectionException the connection exception
	 * @throws AccessException     the access exception
     * @deprecated {@link #handleFileErrors(IServerMessage)}
	 */
	// p4ic4idea: return an IServerMessage
	public static IServerMessage handleFileErrorStr(final Map<String, Object> map)
			throws ConnectionException, AccessException {
	    /*
		String errStr = getErrorOrInfoStr(map);
		if (isNotBlank(errStr)) {
			if (isAuthFail(errStr)) {
				throw new AccessException(errStr);
			} else {
				return errStr.trim();
			}
		}

		return null;
	     */
	    IServerMessage message = toServerMessage(map);
	    if (nonNull(message)) {
            if (nonNull(getAuthFailType(message))) {
                throw new AccessException(message);
            }
            return message;
        }
        return null;
	}

	public static void handleFileErrors(final @Nullable IServerMessage message)
            throws ConnectionException, AccessException {
	    if (nonNull(message)) {
            AuthenticationFailedException.ErrorType authFailType = getAuthFailType(message);
            if (nonNull(authFailType)) {
                throw new AccessException(message);
            }
        }
    }

	/**
	 * Unfortunately, the p4 command version returns a valid map for
	 * non-existent clients/labels/users; the only way we can detect that the
	 * client/label/user doesn't exist is to see if the Update or Access map
	 * entries exist -- if they do, the client/label/user is (most likely) a
	 * valid client/label/user on this server. This seems less than optimal to
	 * me... -- HR.
	 *
	 * @param map the map
	 * @return true, if is exist client or label or user
	 */
	public static boolean isExistClientOrLabelOrUser(final Map<String, Object> map) {
		Validate.notNull(map);
		return map.containsKey(MapKeys.UPDATE_KEY) || map.containsKey(MapKeys.ACCESS_KEY);
	}

	/**
	 * Unfortunately, the p4 command version returns a valid map for
	 * non-existent clients/labels/users; the only way we can detect that the
	 * client/label/user doesn't exist is to see if the Update or Access map
	 * entries exist -- if they do, the client/label/user is (most likely) a
	 * valid client/label/user on this server. This seems less than optimal to
	 * me... -- HR.
	 *
	 * @param map the map
	 * @return true, if is non exist client or label or user
	 */
	public static boolean isNonExistClientOrLabelOrUser(final @Nonnull Map<String, Object> map) {
		Validate.notNull(map);
		return !isExistClientOrLabelOrUser(map);
	}

	// p4ic4idea: new method
	@Nullable
	public static IServerMessage toServerMessage(final @Nullable Map<String, Object> map) {
        if (nonNull(map)) {
            int index = 0;
            String code = (String) map.get(CODE + index);
            List<ISingleServerMessage> singleMessages = new ArrayList<ISingleServerMessage>();
            while (isNotBlank(code)) {
                ISingleServerMessage msg = new ServerMessage.SingleServerMessage(code, index, map);
                singleMessages.add(msg);
                index++;
                code = parseString(map, CODE + index);
            }
            if (!singleMessages.isEmpty()) {
                return new ServerMessage(singleMessages);
            }
        }
        return null;
    }

	// p4ic4idea: new method
    public static List<IServerMessage> toServerMessageList(@Nullable List<Map<String, Object>> maps)
            throws AccessException, RequestException {
	    if (isNull(maps)) {
	        return Collections.emptyList();
        }
        List<IServerMessage> ret = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            IServerMessage message = toServerMessage(map);
            if (nonNull(message)) {
                ret.add(message);
            }
        }
        return ret;
    }

	// p4ic4idea: new method
    @Nullable
    public static IServerMessage toServerMessage(@Nullable List<Map<String, Object>> maps) {
	    if (isNull(maps)) {
	        return null;
        }
        List<ISingleServerMessage> singleMessages = new ArrayList<ISingleServerMessage>();
        for (Map<String, Object> map : maps) {
            int index = 0;
            String code = (String) map.get(CODE + index);
            while (isNotBlank(code)) {
                ISingleServerMessage msg = new ServerMessage.SingleServerMessage(code, index, map);
                singleMessages.add(msg);
                index++;
                code = parseString(map, CODE + index);
            }
        }
        if (singleMessages.isEmpty()) {
            return null;
        }
        return new ServerMessage(singleMessages);
    }

    // p4ic4idea change: returning an IServerMessage instead of a string.
    private static IServerMessage getServerMessage(Map<String, Object> map, int minimumCode) {
        if (nonNull(map)) {
            int index = 0;
            String code = (String) map.get(CODE + index);
            List<ISingleServerMessage> singleMessages = new ArrayList<ISingleServerMessage>();
            while (isNotBlank(code)) {
                ISingleServerMessage msg = new ServerMessage.SingleServerMessage(code, index, map);
                int severity = getSeverity(code);
                if (severity >= minimumCode) {
                    singleMessages.add(msg);
                }
                index++;
                code = parseString(map, CODE + index);
            }
            if (!singleMessages.isEmpty()) {
                return new ServerMessage(singleMessages);
            }
        }
        return null;
    }
}
