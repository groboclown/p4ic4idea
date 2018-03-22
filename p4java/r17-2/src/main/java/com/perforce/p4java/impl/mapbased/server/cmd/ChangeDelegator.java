package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.CHANGE;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IChangeDelegator;

/**
 * Implementation to handle the change command.
 */
public class ChangeDelegator extends BaseDelegator implements IChangeDelegator {

    /**
     * Instantiates a new change delegator.
     * 
     * @param server
     *            the server
     */
    public ChangeDelegator(final IOptionsServer server) {
        super(server);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IChangeDelegator#getChangelist(int)
     */
    @Override
    public IChangelist getChangelist(final int id)
            throws ConnectionException, RequestException, AccessException {
        try {
            return getChangelist(id, null);
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.server.delegator.IChangeDelegator#
     * deletePendingChangelist(int)
     */
    @Override
    public String deletePendingChangelist(final int id)
            throws ConnectionException, RequestException, AccessException {
        try {
            return deletePendingChangelist(id, null);
        } catch (final ConnectionException | RequestException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.perforce.p4java.server.delegator.IChangeDelegator#
     * deletePendingChangelist(int,
     * com.perforce.p4java.option.server.ChangelistOptions)
     */
    @Override
    public String deletePendingChangelist(final int id, final ChangelistOptions opts)
            throws P4JavaException {

        List<Map<String, Object>> resultMaps = execMapCmdList(CHANGE,
                processParameters(opts, null, new String[] { "-d", "" + id }, server), null);
        return ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IChangeDelegator#getChangelist(int,
     * com.perforce.p4java.option.server.ChangelistOptions)
     */
    @Override
    public IChangelist getChangelist(final int id, final ChangelistOptions opts)
            throws P4JavaException {
        // We just pick up the change metadata here, as users can get the file
        // list and diffs
        // separately through the various IChangelist methods and misc. methods
        // below.
        String[] args = new String[] { "-o", String.valueOf(id) };
        if (id == IChangelist.DEFAULT) {
            args = new String[] { "-o" };
        }

        List<Map<String, Object>> resultMaps = execMapCmdList(CHANGE,
                processParameters(opts, null, args, server), null);

        return ResultListBuilder.buildNullableObjectFromNonInfoMessageCommandResultMaps(
                resultMaps,
                new Function<Map, IChangelist>() {
                    @Override
                    public IChangelist apply(Map map) {
                        return new Changelist(map, server);
                    }
                }
        );
    }

}
