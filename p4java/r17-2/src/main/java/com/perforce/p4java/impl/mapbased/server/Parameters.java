package com.perforce.p4java.impl.mapbased.server;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.core.file.FileSpecOpStatus.VALID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;
import org.apache.commons.lang3.Validate;

/**
 * Internal helper class for Server parameter processing using the Options class
 * and other typical server command parameters. Methods here are usually aimed
 * at producing the string arrays passed to the various IServer / Server
 * execMapCmd, etc., methods.
 * <p>
 * <p>
 * This class is not intended for general use by developers or users, but is
 * documented anyway as it's a public class due to package constraints. Using
 * this class is not supported by anything other than the P4Java implementation
 * itself internally.
 */

public class Parameters {
    /**
     * Version of processParameters for those rare methods that have no
     * non-Options arguments.
     *
     * @param opts   possibly-null Options object; if null, no options are set.
     * @param server possibly-null server object; default behaviour when this is
     *               null is to ignore it, and no parameter validation will be
     *               done.
     * @return non-null but possibly empty array of strings suitable for using
     * as-is with the IServer.execMapCmd etc. methods.
     * @throws P4JavaException thrown if any error is detected.
     */
    public static String[] processParameters(final Options opts, final IServer server)
            throws P4JavaException {
        return processParameters(opts, null, null, true, server);
    }

    /**
     * Process options and filespecs arguments for common methods that use a
     * single file spec list and an options object. Will use file annotation on
     * the passed-in file specs (if they exist).
     *
     * @param opts      possibly-null Options object; if null, no options are set.
     * @param fileSpecs possibly-null list of file specs; if null, no file specs are
     *                  added; any non-valid file specs in the list are silently
     *                  ignored.
     * @param server    possibly-null server object; default behaviour when this is
     *                  null is to ignore it, and no parameter validation will be
     *                  done.
     * @return non-null but possibly empty array of strings suitable for using
     * as-is with the IServer.execMapCmd etc. methods.
     * @throws P4JavaException thrown if any error is detected.
     */
    public static String[] processParameters(final Options opts, final List<IFileSpec> fileSpecs,
                                             final IServer server) throws P4JavaException {
        return processParameters(opts, fileSpecs, null, true, server);
    }

    /**
     * Process options, filespecs arguments, and string arguments for common
     * methods. Will use file annotation on the passed-in file specs (if they
     * exist).
     *
     * @param opts         possibly-null Options object; if null, no options are set.
     * @param fileSpecs    possibly-null list of file specs; if null, no file specs are
     *                     added; any non-valid file specs in the list are silently
     *                     ignored.
     * @param stringParams possibly-null string arrays to be added element by element.
     * @param server       possibly-null server object; default behaviour when this is
     *                     null is to ignore it, and no parameter validation will be
     *                     done.
     * @return non-null but possibly empty array of strings suitable for using
     * as-is with the IServer.execMapCmd etc. methods.
     * @throws P4JavaException thrown if any error is detected.
     */
    public static String[] processParameters(final Options opts, final List<IFileSpec> fileSpecs,
                                             final String[] stringParams, final IServer server) throws P4JavaException {
        return processParameters(opts, fileSpecs, stringParams, true, server);
    }

    /**
     * Process options, filespecs arguments, and a single string argument for
     * common methods. Will use file annotation on the passed-in file specs (if
     * they exist).
     *
     * @param opts        possibly-null Options object; if null, no options are set.
     * @param fileSpecs   possibly-null list of file specs; if null, no file specs are
     *                    added; any non-valid file specs in the list are silently
     *                    ignored.
     * @param stringParam possibly-null string parameter to be added.
     * @param server      possibly-null server object; default behaviour when this is
     *                    null is to ignore it, and no parameter validation will be
     *                    done.
     * @return non-null but possibly empty array of strings suitable for using
     * as-is with the IServer.execMapCmd etc. methods.
     * @throws P4JavaException thrown if any error is detected.
     */
    public static String[] processParameters(final Options opts, final List<IFileSpec> fileSpecs,
                                             final String stringParam, final IServer server) throws P4JavaException {
        return processParameters(opts, fileSpecs, new String[]{stringParam}, true, server);
    }

    /**
     * Omnibus processParameters method.
     *
     * @param opts          possibly-null Options object; if null, no options are set.
     * @param fileSpecs     possibly-null list of file specs; if null, no file specs are
     *                      added; any non-valid file specs in the list are silently
     *                      ignored.
     * @param stringParams  possibly-null string array whose contents are to be added
     *                      element by element in the array index order.
     * @param annotateFiles if true, use the fileSpec's getAnnotatedPreferredPathString
     *                      method rather than its getPreferredPathString.
     * @param server        possibly-null server object; default behaviour when this is
     *                      null is to ignore it, and no parameter validation will be
     *                      done.
     * @return non-null but possibly empty array of strings suitable for using
     * as-is with the IServer.execMapCmd etc. methods.
     */
    public static String[] processParameters(final Options opts, final List<IFileSpec> fileSpecs,
                                             final @Nullable String[] stringParams, final boolean annotateFiles,
                                             final IServer server) throws P4JavaException {
        List<String> args = new ArrayList<>();
        addOpts(args, opts, server);

        if (nonNull(stringParams)) {
            for (String param : stringParams) {
                if (isNotBlank(param)) {
                    args.add(param);
                }
            }
        }

        if (annotateFiles) {
            return addFileSpecs(args, fileSpecs);
        } else {
            return addUnannotatedFileSpecs(args, fileSpecs);
        }
    }

    private static void addOpts(@Nonnull List<String> args, @Nullable Options opts, IServer server)
            throws P4JavaException {
        if (nonNull(opts)) {
            if (opts.isImmutable() && nonNull(opts.getOptions())) {
                args.addAll(opts.getOptions());
            } else {
                args.addAll(opts.processOptions(server));
            }
        }
    }

    private static String[] addFileSpecs(@Nonnull final List<String> args,
                                         @Nullable final List<IFileSpec> fileSpecs) {
        Validate.notNull(args);

        if (nonNull(fileSpecs)) {
            for (IFileSpec fSpec : fileSpecs) {
                addFileSpec(args, fSpec);
            }
        }

        return args.toArray(new String[args.size()]);
    }

    private static void addFileSpec(@Nonnull final List<String> args,
                                    @Nullable final IFileSpec fileSpec) {
        Validate.notNull(args);

        if (nonNull(fileSpec)) {
            if ((fileSpec.getOpStatus() == VALID)) {
                args.add(fileSpec.getAnnotatedPreferredPathString());
            }
        }
    }

    private static String[] addUnannotatedFileSpecs(@Nonnull final List<String> args,
                                                    @Nullable final List<IFileSpec> fileSpecs) {
        Validate.notNull(args);
        if (nonNull(fileSpecs)) {
            for (IFileSpec fileSpec : fileSpecs) {
                if (nonNull(fileSpec) && fileSpec.getOpStatus() == VALID) {
                    args.add(fileSpec.getPreferredPathString());
                }
            }
        }

        return args.toArray(new String[args.size()]);
    }

    /**
     * Specialized parameter processing method for commands with 'fromFile' and
     * 'toFiles' parameters.
     */
    public static String[] processParameters(final Options opts, final IFileSpec fromFileSpec,
                                             final List<IFileSpec> toFileSpecs, final String[] stringParams, final IServer server)
            throws P4JavaException {
        List<String> args = new ArrayList<>();

        addOpts(args, opts, server);
        if (nonNull(stringParams)) {
            for (String param : stringParams) {
                if (isNotBlank(param)) {
                    args.add(param);
                }
            }
        }

        addFileSpec(args, fromFileSpec);
        return addFileSpecs(args, toFileSpecs);
    }

    /**
     * Specialized parameter processing method for the Client.integrateFiles and
     * IOptionsServer.getFileDiffs methods.
     * <p>
     * <p>
     * Used due to the need to preserve parameter order in the resulting array;
     * don't use or change this method unless you really know what you're
     * doing...
     */
    public static String[] processParameters(final Options opts, @Nullable final IFileSpec fromFile,
                                             @Nullable final IFileSpec toFile, final String branchSpec, final IServer server)
            throws P4JavaException {
        List<IFileSpec> fromFiles = new ArrayList<>();
        List<IFileSpec> toFiles = new ArrayList<>();
        if (nonNull(fromFile)) {
            fromFiles.add(fromFile);
        }
        if (nonNull(toFile)) {
            toFiles.add(toFile);
        }
        return processParameters(opts, fromFiles, toFiles, branchSpec, server);
    }

    /**
     * Specialized parameter processing method for the
     * IOptionsServer.getInterchanges method. Used due to the need to preserve
     * parameter order in the resulting array; don't use or change this method
     * unless you really know what you're doing...
     * <p>
     */
    public static String[] processParameters(final Options opts,
                                             @Nullable final List<IFileSpec> fromFiles, @Nullable final List<IFileSpec> toFiles,
                                             final String branchSpec, final IServer server) throws P4JavaException {
        String dashS = "-s";
        // Ensure that the -s option, if it exists, is the last option before
        // the
        // fromFile parameter; interpolate the -b option (if branchSpec isn't
        // null)
        // before the -s.
        List<String> args = new ArrayList<>();
        addOpts(args, opts, server);

        // Now back out any -s:
        boolean hadS = false;
        if (args.contains(dashS)) {
            args.remove(dashS);
            hadS = true;
        }
        // See if we need to interpolate a "-b" in there at the end:
        if (isNotBlank(branchSpec)) {
            args.add("-b");
            args.add(branchSpec);
        }

        // Add the "-s" back at the end if needed:
        if (hadS) {
            args.add(dashS);
        }

        addFileSpecIfValidFileSpec(args, fromFiles);
        addFileSpecIfValidFileSpec(args, toFiles);
        return args.toArray(new String[args.size()]);
    }

    private static void addFileSpecIfValidFileSpec(@Nonnull final List<String> args,
                                                   @Nullable final List<IFileSpec> fileSpecs) {
        Validate.notNull(args);

        if (nonNull(fileSpecs)) {
            for (IFileSpec fileSpec : fileSpecs) {
                if (fileSpec.getOpStatus() == VALID) {
                    args.add(fileSpec.getAnnotatedPreferredPathString());
                }
            }
        }
    }
}
