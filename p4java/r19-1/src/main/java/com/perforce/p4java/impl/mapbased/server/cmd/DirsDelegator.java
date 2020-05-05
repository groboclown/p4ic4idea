package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseCode0ErrorString;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.core.file.FileSpecOpStatus.ERROR;
import static com.perforce.p4java.core.file.FileSpecOpStatus.INFO;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.DIRS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetDirectoriesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.delegator.IDirsDelegator;
import org.apache.commons.lang3.Validate;

/**
 * Implementation for a delegator to handle 'p4 dirs'.
 */
public class DirsDelegator extends BaseDelegator implements IDirsDelegator {

    /**
     * Instantiates a new dirs delegator.
     *
     * @param server
     *            the server
     */
    public DirsDelegator(final IOptionsServer server) {
        super(server);
    }

    @Override
    public List<IFileSpec> getDirectories(@Nonnull final List<IFileSpec> fileSpecs,
            final boolean clientOnly, final boolean deletedOnly, final boolean haveListOnly)
            throws ConnectionException, AccessException {

        Validate.notNull(fileSpecs);

        try {
            GetDirectoriesOptions directoriesOptions = new GetDirectoriesOptions()
                    .setClientOnly(clientOnly).setDeletedOnly(deletedOnly)
                    .setHaveListOnly(haveListOnly);
            return getDirectories(fileSpecs, directoriesOptions);
        } catch (final ConnectionException | AccessException exc) {
            throw exc;
        } catch (P4JavaException exc) {
            // TODO Why isn't RequestException handled the same as Access and
            // Connection?
            Log.warn("Unexpected exception in IServer.getDirectories: %s", exc);
            return Collections.emptyList();
        }
    }

    @Override
    public List<IFileSpec> getDirectories(final List<IFileSpec> fileSpecs,
            final GetDirectoriesOptions opts) throws P4JavaException {

        // It seems the tagged result doesn't return an error message
        // for non-existing dirs. See job050447 for details.
        // We're turning off tagged output for the "dirs" command
        HashMap<String, Object> inMap = new HashMap<>();
        inMap.put(IServer.IN_MAP_USE_TAGS_KEY, "no");
        List<Map<String, Object>> resultMaps = execMapCmdList(DIRS,
                processParameters(opts, fileSpecs, server), inMap);
        List<IFileSpec> specList = new ArrayList<>();

        if (nonNull(resultMaps)) {
            for (Map<String, Object> map : resultMaps) {
                if (nonNull(map)) {
                    String code0String = parseCode0ErrorString(map);
                    String errStr = ResultMapParser.handleFileErrorStr(map);
                    if (isBlank(errStr) || ResultMapParser.isInfoMessage(map)) {
                        String dirName = parseString(map, "dirName");
                        String dir = parseString(map, "dir");
                        if (isNotBlank(dirName)) {
                            specList.add(new FileSpec(dirName));
                        } else if (isNotBlank(dir)) {
                            specList.add(new FileSpec(dir));
                        } else {
                            if (ResultMapParser.isInfoMessage(map)) {
                                specList.add(new FileSpec(INFO, errStr, code0String));
                            }
                        }
                    } else {
                        specList.add(new FileSpec(ERROR, errStr, code0String));
                    }
                }
            }
        }

        return specList;
    }
}
