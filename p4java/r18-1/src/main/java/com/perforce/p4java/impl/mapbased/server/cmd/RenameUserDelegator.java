package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.RENAMEUSER;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IRenameUserDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the RenameUser command.
 */
public class RenameUserDelegator extends BaseDelegator implements IRenameUserDelegator {
    /**
     * Instantiate a new RenameUserDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public RenameUserDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String renameUser(String oldUserName, String newUserName) throws P4JavaException {
        Validate.notBlank(oldUserName, "Old user name shouldn't null or empty");
        Validate.notBlank(newUserName, "New user name shouldn't null or empty");

        List<Map<String, Object>> resultMaps = execMapCmdList(
                RENAMEUSER,
                new String[]{"--from=" + oldUserName, "--to=" + newUserName},
                null);

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }
}
