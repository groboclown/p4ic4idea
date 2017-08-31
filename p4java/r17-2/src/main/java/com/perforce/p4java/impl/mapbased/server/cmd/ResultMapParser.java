package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.server.IServer;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.exception.MessageSeverityCode.E_FAILED;
import static com.perforce.p4java.exception.MessageSeverityCode.E_INFO;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.COMMIT;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DEPOT_FILE;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.FMT0;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.REV;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TREE;
import static com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage.CODE;
import static com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage.FMT;
import static com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage.getSeverity;
import static com.perforce.p4java.impl.mapbased.rpc.msg.RpcMessage.interpolateArgs;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Utility to parse a result map and test for info/error messages etc.
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
	 * Array of access error messages.
	 */
	private static final String[] ACCESS_ERR_MSGS = {CORE_AUTH_FAIL_STRING_1,
			CORE_AUTH_FAIL_STRING_2, CORE_AUTH_FAIL_STRING_3, CORE_AUTH_FAIL_STRING_4,
			AUTH_FAIL_STRING_1, AUTH_FAIL_STRING_2};

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
	public static String parseCommandResultMapIfIsInfoMessageAsString(
			@Nonnull final List<Map<String, Object>> resultMaps)
			throws AccessException, RequestException {
		StringBuilder retVal = new StringBuilder(INITIAL_STRING_BUILDER);
		if (nonNull(resultMaps)) {
			for (Map<String, Object> map : resultMaps) {
				handleErrorStr(map);
				if (isInfoMessage(map)) {
					if (retVal.length() != 0) {
						retVal.append("\n");
					}

					retVal.append(getInfoStr(map));
				}
			}
		}
		return retVal.toString();
	}

	/**
	 * Checks if is info message.
	 *
	 * @param map the map
	 * @return true, if is info message
	 */
	public static boolean isInfoMessage(final Map<String, Object> map) {
		return nonNull(map) && (getSeverity(parseCode0ErrorString(map)) == E_INFO);
	}

	/**
	 * Tests the map for errors and throws an exception if found.
	 *
	 * @param map the map
	 * @return true, if successful
	 * @throws RequestException the request exception
	 * @throws AccessException  the access exception
	 */
	public static boolean handleErrorStr(final Map<String, Object> map)
			throws RequestException, AccessException {
		String errStr = getErrorStr(map);

		if (isNotBlank(errStr)) {
			if (isAuthFail(errStr)) {
				throw new AccessException(errStr);
			} else {
				throw new RequestException(errStr, parseCode0ErrorString(map));
			}
		}
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
	public static String getErrorStr(final Map<String, Object> map) {
		return getString(map, E_FAILED);
	}

	/**
	 * Checks to see if an error String is as a result of an auth fail.
	 *
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
	 */
	public static String getInfoStr(final Map<String, Object> map) {
		if (nonNull(map)) {
			String code0 = parseCode0ErrorString(map);
			int severity = getSeverity(code0);
			if (severity == E_INFO) {
				String fmtStr = parseString(map, FMT0);
				if (isBlank(fmtStr)) {
					return EMPTY;
				}

				if (!contains(fmtStr, "%")) {
					return fmtStr;
				}
				return interpolateArgs(fmtStr, map);
			}
		}

		return null;
	}

	/**
	 * Gets the string.
	 *
	 * @param map         the map
	 * @param minimumCode the minimum code
	 * @return the string
	 */
	private static String getString(final Map<String, Object> map, final int minimumCode) {

		if (nonNull(map)) {
			int index = 0;
			String code = (String) map.get(CODE + index);
			// Return if no code0 key found
			if (isBlank(code)) {
				return null;
			}

			boolean foundCode = false;
			StringBuilder codeString = new StringBuilder(INITIAL_STRING_BUILDER);
			while (isNotBlank(code)) {
				int severity = getSeverity(code);
				if (severity >= minimumCode) {
					foundCode = true;
					String fmtStr = parseString(map, FMT + index);
					if (isNotBlank(fmtStr)) {
						if (indexOf(fmtStr, '%') != -1) {
							fmtStr = interpolateArgs(fmtStr, map);
						}
						// Insert latest message at beginning of error string
						// since server structures them this way
						codeString.insert(0, fmtStr);
						codeString.insert(fmtStr.length(), '\n');
					}
				}
				index++;
				code = parseString(map, CODE + index);
			}

			// Only return a string if at least one severity code was found
			if (foundCode) {
				return codeString.toString();
			}
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
	public static String getErrorOrInfoStr(final Map<String, Object> map) {
		return getString(map, E_INFO);
	}

	/**
	 * Throw request exception if error message found.
	 *
	 * @param map the map
	 * @throws RequestException the request exception
	 */
	public static void throwRequestExceptionIfErrorMessageFound(final Map<String, Object> map)
			throws RequestException {
		String errStr = getErrorStr(map);
		if (isNotBlank(errStr)) {
			throw new RequestException(errStr, parseCode0ErrorString(map));
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
		String errStr = getErrorOrInfoStr(map);

		if (isNotBlank(errStr)) {
			if (isAuthFail(errStr)) {
				throw new AccessException(errStr);
			} else {
				throw new RequestException(errStr, parseCode0ErrorString(map));
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
				handleErrorStr(map);
				if (retVal.length() != 0) {
					retVal.append("\n");
				}

				retVal.append(getInfoStr(map));
			}
		} else {
			Log.warn("Null map array is returned when execute Helix command");
		}

		return retVal.toString();
	}

	/**
	 * Parses the command result map as file specs.
	 *
	 * @param id         the id
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
	 */
	public static String handleFileErrorStr(final Map<String, Object> map)
			throws ConnectionException, AccessException {
		String errStr = getErrorOrInfoStr(map);
		if (isNotBlank(errStr)) {
			if (isAuthFail(errStr)) {
				throw new AccessException(errStr);
			} else {
				return errStr.trim();
			}
		}

		return null;
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
	public static boolean isNonExistClientOrLabelOrUser(final Map<String, Object> map) {
		Validate.notNull(map);
		return !isExistClientOrLabelOrUser(map);
	}
}
