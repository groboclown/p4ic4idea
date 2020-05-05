package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.PROTECT;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.admin.IProtectionEntry;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.ProtectionsTable;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IProtectDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Protect command.
 */
public class ProtectDelegator extends BaseDelegator implements IProtectDelegator {
    /**
     * Instantiate a new ProtectDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public ProtectDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String createProtectionEntries(@Nonnull final List<IProtectionEntry> entryList)
            throws P4JavaException {
        Validate.notNull(entryList);

        List<Map<String, Object>> resultMaps = execMapCmdList(PROTECT, new String[] { "-i" },
                InputMapper.map(new ProtectionsTable(entryList)));
        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    @Override
    public String updateProtectionEntries(@Nonnull final List<IProtectionEntry> entryList)
            throws P4JavaException {
        return createProtectionEntries(entryList);
    }

    @Override
    public InputStream getProtectionsTable() throws P4JavaException {
        return execStreamCmd(PROTECT, new String[] { "-o" });
    }
}
