package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.VerifyFilesOptions;

/**
 * Interface to handle the Verify command.
 */
public interface IVerifyDelegator {
    /**
     * Verify that the server archives are intact.<p>
     * <p>
     * This method require that the user be an operator or have 'admin' access,
     * which is granted by 'p4 protect'.
     *
     * @param fileSpecs filespecs to be processed; if null or empty, an empty list is returned.
     * @param opts      VerifyFilesOptions object describing optional parameters; if null, no options
     *                  are set.
     * @return non-null (but possibly empty) list of files with revision-specific information and an
     * MD5 digest of the revision's contents.
     * @throws P4JavaException if an error occurs processing this method and its parameters.
     * @since 2014.1
     */
    List<IExtendedFileSpec> verifyFiles(
            List<IFileSpec> fileSpecs,
            VerifyFilesOptions opts) throws P4JavaException;
}
