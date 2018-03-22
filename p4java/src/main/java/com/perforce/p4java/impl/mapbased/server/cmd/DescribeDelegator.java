package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.file.DiffType;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.DescribeOptions;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDescribeDelegator;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static com.perforce.p4java.common.base.FileDiffUtils.setFileDiffsOptionsByDiffType;
import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4JavaExceptions.throwRequestExceptionIfPerforceServerVersionOldThanExpected;
import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.server.CmdSpec.DESCRIBE;

/**
 * @author Sean Shou
 * @since 22/09/2016
 */
public class DescribeDelegator extends BaseDelegator implements IDescribeDelegator {

	public DescribeDelegator(IOptionsServer server) {
		super(server);
	}

	@Override
	public InputStream getChangelistDiffs(final int changelistId,
	                                      final GetChangelistDiffsOptions opts) throws P4JavaException {

		return execStreamCmd(DESCRIBE,
				processParameters(opts, null, String.valueOf(changelistId), server));
	}

	@Override
	public InputStream getChangelistDiffsStream(final int id, final DescribeOptions options)
			throws ConnectionException, RequestException, AccessException {

		DiffType diffType = null;
		boolean shelvedDiffs = false;
		if (nonNull(options)) {
			diffType = options.getType();
			shelvedDiffs = options.isOutputShelvedDiffs();
		}

		// Shelved file diffs are only support in server version 2009.2+
		throwRequestExceptionIfPerforceServerVersionOldThanExpected(
				shelvedDiffs && server.getServerVersion() >= 20092,
				"Shelved file diffs are not supported by this version of the Perforce server");

		try {
			GetChangelistDiffsOptions opts = new GetChangelistDiffsOptions();
			opts.setOutputShelvedDiffs(shelvedDiffs);

			setFileDiffsOptionsByDiffType(diffType, opts);
			return getChangelistDiffs(id, opts);
		} catch (final ConnectionException | AccessException | RequestException exc) {
			throw exc;
		} catch (P4JavaException exc) {
			throw new RequestException(exc.getMessage(), exc);
		}
	}

	@Override
	public List<IFileSpec> getShelvedFiles(final int changelistId) throws P4JavaException {
		List<Map<String, Object>> resultMaps = execMapCmdList(DESCRIBE,
				new String[]{"-s", "-S", String.valueOf(changelistId)}, null);

		return ResultMapParser.parseCommandResultMapAsFileSpecs(changelistId, server, resultMaps);
	}

	/*
	 * implemented on behalf of Iserver
	 */
	public InputStream getChangelistDiffs(final int id, final DiffType diffType)
			throws ConnectionException, RequestException, AccessException {

		return getChangelistDiffsStream(id, new DescribeOptions(diffType));
	}

	/*
	 * implemented on behalf of Iserver
	 */
	public List<IFileSpec> getChangelistFiles(final int id)
			throws ConnectionException, RequestException, AccessException {
		// NOTE: do NOT change the location or order of the "-s" flag below, as
		// its
		// existence is used to signal to the underlying RPC implementation(s)
		// that tagged
		// output must (or must not) be used with this particular "describe"
		// command. See
		// OneShotServerImpl.useTags() for a canonical example of this...
		try {
			List<Map<String, Object>> resultMaps = execMapCmdList(DESCRIBE,
					new String[]{"-s", String.valueOf(id)}, null);

			return ResultMapParser.parseCommandResultMapAsFileSpecs(id, server, resultMaps);
		} catch (P4JavaException p4je) {
			throw new RequestException(p4je);
		}
	}

	public List<IFileSpec> getCommitFiles(final String repo, final String commit)
			throws ConnectionException, RequestException, AccessException {
		try {
			List<Map<String, Object>> resultMaps = execMapCmdList(DESCRIBE,
					new String[]{"-s", "-n", repo, commit}, null);

			return ResultMapParser.parseGraphCommandResultMapAsFileSpecs(server, resultMaps);
		} catch (P4JavaException p4je) {
			throw new RequestException(p4je);
		}
	}
}
