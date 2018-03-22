package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.ListData;
import com.perforce.p4java.impl.mapbased.server.Parameters;
import com.perforce.p4java.option.server.ListOptions;
import com.perforce.p4java.server.IOptionsServer;

import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.LABEL;
import static com.perforce.p4java.impl.mapbased.rpc.func.RpcFunctionMapKey.TOTALFILECOUNT;

/**
 * This class acts as a delegator that executes the command 'p4 list'
 */
public class ListDelegator extends BaseDelegator implements IListDelegator {

	/**
	 * Instantiates a new list delegator.
	 *
	 * @param server the server
	 */
	public ListDelegator(final IOptionsServer server) {
		super(server);
	}

	/**
	 * Returns the data retrieved as part of the 'p4 list -l'
	 *
	 * @param fileSpecs List of file paths to be labeled
	 * @param options   Options as required by the command p4 list
	 * @return
	 * @throws P4JavaException
	 */
	@Override
	public ListData getListData(List<IFileSpec> fileSpecs, ListOptions options) throws P4JavaException {

		Map<String, Object>[] resultMap = server.execMapCmd("list",
				Parameters.processParameters(options, fileSpecs, server), null);

		if (!nonNull(resultMap)) {
			return null;
		}

		String label = null;
		long fileCount = 0;

		for (Map<String, Object> map : resultMap) {

			ResultMapParser.handleErrorStr(map);

			try {
				if (map.containsKey(LABEL)) {
					label = parseString(map, LABEL);
				}
				if (map.containsKey(TOTALFILECOUNT)) {
					fileCount = parseLong(map, TOTALFILECOUNT);
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}
		}
		return new ListData(fileCount, label);
	}

	@Override
	public ListData getListData(List<IFileSpec> fileSpecs, ListOptions options, String clientName) throws P4JavaException {

		if (!server.getCurrentClient().getName().equals(clientName)) {
			server.setCurrentClient(server.getClient(clientName));
		}

		options.setLimitClient(true);
		return getListData(fileSpecs, options);
	}
}
