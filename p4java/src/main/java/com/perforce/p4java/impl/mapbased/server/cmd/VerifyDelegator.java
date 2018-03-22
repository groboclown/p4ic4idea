package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.impl.mapbased.server.Parameters.processParameters;
import static com.perforce.p4java.impl.mapbased.server.cmd.ResultListBuilder.buildNonNullExtendedFileSpecListFromCommandResultMaps;
import static com.perforce.p4java.server.CmdSpec.VERIFY;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.VerifyFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IVerifyDelegator;


/**
 * Implementation to handle the Verify command.
 */
public class VerifyDelegator extends BaseDelegator implements IVerifyDelegator {
  /**
   * Instantiate a new VerifyDelegator, providing the server object that will be used to
   * execute Perforce Helix attribute commands.
   *
   * @param server a concrete implementation of a Perforce Helix Server
   */
  public VerifyDelegator(IOptionsServer server) {
    super(server);
  }

  @Override
  public List<IExtendedFileSpec> verifyFiles(
      final List<IFileSpec> fileSpecs,
      final VerifyFilesOptions opts) throws P4JavaException {

    List<Map<String, Object>> resultMaps = execMapCmdList(
        VERIFY,
            processParameters(opts, fileSpecs, server),
        null);

    return buildNonNullExtendedFileSpecListFromCommandResultMaps(resultMaps, server);
  }
}
