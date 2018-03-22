package com.perforce.p4java.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser;
import org.apache.commons.lang3.Validate;

public abstract class HelixCommandExecutor implements IHelixCommandExecutor {

    public List<Map<String, Object>> execMapCmdList(
            @Nonnull final CmdSpec cmdSpec,
            String[] cmdArgs,
            Map<String, Object> inMap) throws ConnectionException, AccessException {

        Validate.notNull(cmdSpec);
        try {
            return execMapCmdList(cmdSpec.toString(), cmdArgs, inMap);
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            return Collections.emptyList();
        }
    }

    /**
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#handleFileErrorStr(Map)}
     */
    @Deprecated
    public String handleFileErrorStr(final Map<String, Object> map)
            throws ConnectionException, AccessException {
        return ResultMapParser.handleFileErrorStr(map);
    }

    /**
     * @deprecated use {@link com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser#handleErrorStr(Map)}
     */
    @Deprecated
    public boolean handleErrorStr(Map<String, Object> map)
            throws RequestException, AccessException {
        return ResultMapParser.handleErrorStr(map);
    }
}
