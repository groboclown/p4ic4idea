package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.isNull;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.handleErrorStr;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isExistClientOrLabelOrUser;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.isInfoMessage;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapAsString;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultMapParser.parseCommandResultMapIfIsInfoMessageAsString;
import static com.perforce.p4java.server.CmdSpec.LABEL;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.InputMapper;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.DeleteLabelOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.ILabelDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation to handle the Label command.
 */
public class LabelDelegator extends BaseDelegator implements ILabelDelegator {
    /**
     * Instantiate a new LabelDelegator, providing the server object that will be used to
     * execute Perforce Helix attribute commands.
     *
     * @param server a concrete implementation of a Perforce Helix Server
     */
    public LabelDelegator(IOptionsServer server) {
        super(server);
    }

    @Override
    public ILabel getLabel(final String labelName)
            throws ConnectionException, RequestException, AccessException {

        Validate.notBlank(labelName, "Label name shouldn't null or empty.");
        ILabel label = null;
        final String OFLAG = "-o";
        List<Map<String, Object>> resultMaps = execMapCmdList(
                LABEL,
                new String[]{OFLAG, labelName},
                null);

        if (isNull(resultMaps)) {
            Log.warn("Unexpected null map array returned to ServerImpl.getLabel()");
        } else {
            // Note that the only way to tell whether the requested label
            // existed or not is to look for the returned Access and Update
            // fields -- if they're both missing it's probably not a real
            // label, just the public new label template coming back from
            // the server.
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    handleErrorStr(map);
                    if (!isInfoMessage(map)) {
                        if (isExistClientOrLabelOrUser(map)) {
                            label = new Label(map, server);
                        }
                    }
                }
            }
        }

        return label;
    }

    @Override
    public String createLabel(final ILabel label)
            throws ConnectionException, RequestException, AccessException {

        Validate.notNull(label);
        List<Map<String, Object>> resultMaps = execMapCmdList(
                LABEL,
                new String[]{"-i"},
                InputMapper.map(label));

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }

    @Override
    public String updateLabel(final ILabel label)
            throws ConnectionException, RequestException, AccessException {

        Validate.notNull(label);

        List<Map<String, Object>> resultMaps = execMapCmdList(
                LABEL,
                new String[]{"-i"},
                InputMapper.map(label));

        return parseCommandResultMapAsString(resultMaps);
    }

    @Override
    public String deleteLabel(final String labelName, final boolean force)
            throws ConnectionException, RequestException, AccessException {

        try {
            return deleteLabel(labelName, new DeleteLabelOptions().setForce(force));
        } catch (final ConnectionException | AccessException | RequestException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            throw new RequestException(exc.getMessage(), exc);
        }
    }

    @Override
    public String deleteLabel(final String labelName, final DeleteLabelOptions opts)
            throws P4JavaException {

        Validate.notBlank(labelName, "Label name shouldn't null or empty.");
        List<Map<String, Object>> resultMaps = execMapCmdList(
                LABEL,
                processParameters(opts, null, new String[]{"-d", labelName}, server),
                null);

        return parseCommandResultMapIfIsInfoMessageAsString(resultMaps);
    }
}
