package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.MapKeys.LF;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.NEW_PASSWORD;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.NEW_PASSWORD2;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.OLD_PASSWORD;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.PASSWD;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IPasswdDelegator;

/**
 * Implementation to handle the Passwd command.
 */
public class PasswdDelegator extends BaseDelegator implements IPasswdDelegator {

    /**
     * Instantiate a new PasswdDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public PasswdDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String changePassword(
            final String oldPassword,
            final String newPassword,
            final String userName) throws P4JavaException {

        String oldPasswordPlusLF = oldPassword;
        if (isNotBlank(oldPassword)) {
            oldPasswordPlusLF += LF;
        }

        String newPasswordPlusLF = LF;
        if (isNotBlank(newPassword)) {
            newPasswordPlusLF = newPassword + LF;
        }

        Map<String, Object> pwdMap = new HashMap<>();
        pwdMap.put(OLD_PASSWORD, oldPasswordPlusLF);
        pwdMap.put(NEW_PASSWORD, newPasswordPlusLF);
        pwdMap.put(NEW_PASSWORD2, newPasswordPlusLF);

        String[] args = {};
        if (isNotBlank(userName)) {
            args = new String[]{userName};
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(PASSWD, args, pwdMap);

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }


}
