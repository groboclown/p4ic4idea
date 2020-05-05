package com.perforce.p4java.impl.mapbased.client.cmd;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.delegator.IWhereDelegator;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.impl.mapbased.server.Server;
import com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder;
import com.perforce.p4java.mapapi.MapTable;
import com.perforce.p4java.mapapi.MapTableBuilder;
import com.perforce.p4java.mapapi.MapTableT;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WhereDelegator implements IWhereDelegator {

	IServer server;
	IClient client;

	public WhereDelegator(IServer server, IClient client) {
		this.server = server;
		this.client = client;
	}

	/**
	 * @see com.perforce.p4java.client.delegator.IWhereDelegator#where(java.util.List)
	 */
	@Override
	public List<IFileSpec> where(List<IFileSpec> fileSpecs) throws ConnectionException, AccessException {

		List<IFileSpec> resultList = new ArrayList<IFileSpec>();

		List<Map<String, Object>> resultMaps = this.server.execMapCmdList(
				CmdSpec.WHERE, Server.getPreferredPathArray(null, fileSpecs), null);

		if (resultMaps != null) {
			for (Map<String, Object> result : resultMaps) {
				resultList.add(ResultListBuilder.handleFileReturn(result, server));
			}
		}

		return resultList;
	}

	/**
	 * @see com.perforce.p4java.client.delegator.IWhereDelegator#localWhere(java.util.List)
	 */
	@Override
	public List<IFileSpec> localWhere(List<IFileSpec> fileSpecs) {

		List<IFileSpec> resultList = new ArrayList<>();

		for (IFileSpec spec : fileSpecs) {
			MapTable mt = MapTableBuilder.buildMapTable(client);
			if (spec.getOriginalPath() != null) {
				if (!spec.getOriginalPathString().startsWith("//")) {
					if (spec.getLocalPath() == null) {
						spec.setLocalPath(spec.getOriginalPathString());
					}
				} else if (spec.getOriginalPathString().contains(client.getName())) {
					if (spec.getClientPath() == null) {
						spec.setClientPath(spec.getOriginalPathString());
					}
				} else {
					if (spec.getDepotPath() == null) {
						spec.setDepotPath(spec.getOriginalPathString());
					}
				}
			}
			if (spec.getLocalPath() != null) {
				spec = localPathToClientPath(spec, client);
				String s = spec.getClientPathString();
				for (int i = 0; i < mt.count; i++) {
					if (mt.translate(mt.get(i), MapTableT.RHS, s) != null) {
						String depotPath = mt.translate(mt.get(i), MapTableT.RHS, s);
						spec.setDepotPath(depotPath);
					}
				}
				resultList.add(spec);
			} else if (spec.getDepotPath() != null) {
				String s = spec.getDepotPathString();
				for (int i = 0; i < mt.count; i++) {
					if (mt.translate(mt.get(i), MapTableT.LHS, s) != null) {
						String clientPath = mt.translate(mt.get(i), MapTableT.LHS, s);
						spec.setClientPath(clientPath);
					}
				}
				spec = clientPathToLocalPath(spec, client);
				resultList.add(spec);
			} else if (spec.getClientPath() != null) {
				String s = spec.getClientPathString();
				for (int i = 0; i < mt.count; i++) {
					if (mt.translate(mt.get(i), MapTableT.RHS, s) != null) {
						String depotPath = mt.translate(mt.get(i), MapTableT.RHS, s);
						spec.setDepotPath(depotPath);
					}
				}
				spec = clientPathToLocalPath(spec, client);
				resultList.add(spec);
			} else {
				//add nothing
			}

		}

		return resultList;
	}

	private IFileSpec clientPathToLocalPath(IFileSpec spec, IClient client) {
		String clientPath = spec.getClientPathString();
		String localPath = clientPath.replace("//" + client.getName(), client.getRoot());
		spec.setLocalPath(localPath);
		return spec;
	}

	private IFileSpec localPathToClientPath(IFileSpec spec, IClient client) {
		String localPath = spec.getLocalPathString();
		String clientPath = localPath.replace(client.getRoot(), "//" + client.getName());
		spec.setClientPath(clientPath);
		return spec;
	}

}
