package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.MapKeys.TRIGGERS_KEY;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.TRIGGERS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.TriggerEntry;
import com.perforce.p4java.impl.generic.admin.TriggersTable;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ITriggersDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Triggers command.
 */
public class TriggersDelegator extends BaseDelegator implements ITriggersDelegator {
    /**
     * Instantiate a new TriggersDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public TriggersDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public String createTriggerEntries(@Nonnull final List<ITriggerEntry> entryList)
            throws P4JavaException {

        Validate.notNull(entryList);
        List<Map<String, Object>> resultMaps = execMapCmdList(
                TRIGGERS,
                new String[]{"-i"},
                InputMapper.map(new TriggersTable(entryList)));
        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    @Override
    public List<ITriggerEntry> getTriggerEntries() throws P4JavaException {
        List<ITriggerEntry> triggersList = new ArrayList<>();
        List<Map<String, Object>> resultMaps = execMapCmdList(
                TRIGGERS,
                new String[]{"-o"},
                null);
        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    if (!handleErrorStr(map)) {
                        for (int i = 0; ; i++) {
                            String entry = parseString(map, TRIGGERS_KEY + i);
                            if (isNotBlank(entry)) {
                                triggersList.add(new TriggerEntry(entry, i));
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
        }

        return triggersList;
    }

    @Override
    public String updateTriggerEntries(@Nonnull final List<ITriggerEntry> entryList)
            throws P4JavaException {

        return createTriggerEntries(entryList);
    }

    @Override
    public InputStream getTriggersTable() throws P4JavaException {
        return execStreamCmd(TRIGGERS, new String[]{"-o"});
    }
}
