package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.IUser;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.Login2Options;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ILogin2Delegator;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.DESC;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.ID;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.PASSWORD;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.LOGIN2;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

// p4ic4idea: use IServerMessage
import com.perforce.p4java.server.IServerMessage;

public class Login2Delegator extends BaseDelegator implements ILogin2Delegator {

	/**
	 * Instantiates a new login2 delegator.
	 *
	 * @param server the server
	 */
	public Login2Delegator(final IOptionsServer server) {
		super(server);
	}

	@Override
	public List<Map<String, Object>> login2(Login2Options opts, String user) throws P4JavaException {

		String[] params;
		if (user == null || user.isEmpty()) {
			params = processParameters(opts, server);
		} else {
			params = processParameters(opts, null, user, server);
		}
		List<Map<String, Object>> resultMaps = execMapCmdList(LOGIN2, params, null);

		return resultMaps;
	}

	// p4ic4idea: use IServerMessage
	@Override
	public IServerMessage getLogin2Status() throws P4JavaException {

		Login2Options opts = new Login2Options();
		opts.setStatus(true);

		List<Map<String, Object>> resultMaps = login2(opts, null);

		IServerMessage statusStr = null;
		if (nonNull(resultMaps) && !resultMaps.isEmpty()) {
			Map<String, Object> firstResultMap = resultMaps.get(0);
			statusStr = ResultMapParser.getErrorOrInfoStr(firstResultMap);
		}
		return statusStr;
	}

	// p4ic4idea: use IServerMessage
	@Override
	public IServerMessage getLogin2Status(IUser user) throws P4JavaException {

		Validate.notNull(user);
		Validate.notBlank(user.getLoginName(), "Login name shouldn't null or empty");

		Login2Options opts = new Login2Options();
		opts.setStatus(true);

		List<Map<String, Object>> resultMaps = login2(opts, user.getLoginName());

		IServerMessage statusStr = null;
		if (nonNull(resultMaps) && !resultMaps.isEmpty()) {
			Map<String, Object> firstResultMap = resultMaps.get(0);
			statusStr = ResultMapParser.getErrorOrInfoStr(firstResultMap);
		}
		return statusStr;
	}

	@Override
	public Map<String, String> login2ListMethods() throws P4JavaException {

		Login2Options opts = new Login2Options();
		opts.setState("list-methods");

		List<Map<String, Object>> resultMaps = login2(opts, null);

		Map<String, String> methods = new HashMap<>();
		for (Map<String, Object> map : resultMaps) {

			ResultMapParser.handleErrorStr(map);

			if (map.containsKey(ID) && map.containsKey(DESC)) {
				String id = parseString(map, ID);
				String desc = parseString(map, DESC);
				methods.put(id, desc);
			}
		}
		return methods;
	}

	@Override
	public String login2InitAuth(String method) throws P4JavaException {

		Login2Options opts = new Login2Options();
		opts.setState("init-auth");
		opts.setMethod(method);

		List<Map<String, Object>> resultMaps = login2(opts, null);
		String message = ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
		return message;
	}

	@Override
	public String login2CheckAuth(String auth, boolean persist) throws P4JavaException {

		Login2Options opts = new Login2Options();
		opts.setState("check-auth");
		opts.setPersist(persist);

		String authCheck = auth;
		if (isNotBlank(auth)) {
			authCheck = auth + "\n";
		}

		Map<String, Object> pwdMap = new HashMap<>();
		pwdMap.put(PASSWORD, authCheck);

		List<Map<String, Object>> resultMaps = execMapCmdList(LOGIN2, processParameters(opts, server), pwdMap);

		String message = ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
		return message;
	}

	@Override
	public String login2(IUser user, Login2Options opts) throws P4JavaException {

		Validate.notNull(user);
		Validate.notBlank(user.getLoginName(), "Login name shouldn't null or empty");

		List<Map<String, Object>> resultMaps = login2(opts, user.getLoginName());
		String message = ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
		return message;
	}
}
