/**
 * 
 */
package com.perforce.p4java.impl.mapbased.server;

import java.util.ArrayList;
import java.util.List;

import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Internal helper class for Server parameter processing using the Options class
 * and other typical server command parameters. Methods here are usually
 * aimed at producing the string arrays passed to the various IServer / Server
 * execMapCmd, etc., methods.<p>
 * 
 * This class is not intended for general use by developers or users, but is
 * documented anyway as it's a public class due to package constraints. Using this
 * class is not supported by anything other than the P4Java implementation itself
 * internally.
 */

public class Parameters {
	
	/**
	 * Process options and filespecs arguments for common methods that use a single file
	 * spec list and an options object.
	 * Will use file annotation on the passed-in file specs (if they exist).
	 * 
	 * @param opts possibly-null Options object; if null, no options are set.
	 * @param fileSpecs possibly-null list of file specs; if null, no file specs are added;
	 * 			any non-valid file specs in the list are silently ignored.
	 * @param server possibly-null server object; default behaviour when this is null
	 * 				is to ignore it, and no parameter validation will be done.
	 * @return non-null but possibly empty array of strings suitable for using as-is
	 * 				with the IServer.execMapCmd etc. methods.
	 * @throws P4JavaException thrown if any error is detected.
	 */
	public static String[] processParameters(Options opts, List<IFileSpec> fileSpecs, IServer server)
										throws P4JavaException {
		return processParameters(opts, fileSpecs, null, true, server);
	}

	/**
	 * Version of processParameters for those rare methods that have
	 * no non-Options arguments.
	 * 
	 * @param opts possibly-null Options object; if null, no options are set.
	 * @param server possibly-null server object; default behaviour when this is null
	 * 				is to ignore it, and no parameter validation will be done.
	 * @return non-null but possibly empty array of strings suitable for using as-is
	 * 				with the IServer.execMapCmd etc. methods.
	 * @throws P4JavaException thrown if any error is detected.
	 */
	public static String[] processParameters(Options opts, IServer server) throws P4JavaException {
		return processParameters(opts, null, null, true, server);
	}
	
	/**
	 * Process options, filespecs arguments, and string arguments for common methods.
	 * Will use file annotation on the passed-in file specs (if they exist).
	 *
	 * @param opts possibly-null Options object; if null, no options are set.
	 * @param fileSpecs possibly-null list of file specs; if null, no file specs are added;
	 * 			any non-valid file specs in the list are silently ignored.
	 * @param stringParams possibly-null string arrays to be added element by element.
	 * @param server possibly-null server object; default behaviour when this is null
	 * 				is to ignore it, and no parameter validation will be done.
	 * @return non-null but possibly empty array of strings suitable for using as-is
	 * 				with the IServer.execMapCmd etc. methods.
	 * @throws P4JavaException thrown if any error is detected.
	 */
	public static String[] processParameters(Options opts, List<IFileSpec> fileSpecs,
			String[] stringParams, IServer server) throws P4JavaException {
		return processParameters(opts, fileSpecs, stringParams, true, server);
	}
	
	/**
	 * Process options, filespecs arguments, and a single string argument for common methods.
	 * Will use file annotation on the passed-in file specs (if they exist).
	 *
	 * @param opts possibly-null Options object; if null, no options are set.
	 * @param fileSpecs possibly-null list of file specs; if null, no file specs are added;
	 * 			any non-valid file specs in the list are silently ignored.
	 * @param stringParam possibly-null string parameter to be added.
	 * @param server possibly-null server object; default behaviour when this is null
	 * 				is to ignore it, and no parameter validation will be done.
	 * @return non-null but possibly empty array of strings suitable for using as-is
	 * 				with the IServer.execMapCmd etc. methods.
	 * @throws P4JavaException thrown if any error is detected.
	 */
	public static String[] processParameters(Options opts, List<IFileSpec> fileSpecs,
			String stringParam, IServer server) throws P4JavaException {
		return processParameters(opts, fileSpecs, new String[] { stringParam }, true, server);
	}
	
	/**
	 * Omnibus processParameters method.
	 * 
	 * @param opts possibly-null Options object; if null, no options are set.
	 * @param fileSpecs  possibly-null list of file specs; if null, no file specs are added;
	 * 			any non-valid file specs in the list are silently ignored.
	 * @param stringParams possibly-null string array whose contents are to be added element
	 * 				by element in the array index order.
	 * @param annotateFiles if true, use the fileSpec's getAnnotatedPreferredPathString method
	 * 				rather than its getPreferredPathString.
	 * @param server possibly-null server object; default behaviour when this is null
	 * 				is to ignore it, and no parameter validation will be done.
	 * @return non-null but possibly empty array of strings suitable for using as-is
	 * 				with the IServer.execMapCmd etc. methods.
	 * @throws P4JavaException
	 */
	public static String[] processParameters(Options opts, List<IFileSpec> fileSpecs,
			String stringParams[], boolean annotateFiles, IServer server) throws P4JavaException {
		List<String> args = new ArrayList<String>();
		
		addOpts(args, opts, server);
		
		if (stringParams != null) {
			for (String param : stringParams) {
				if (param != null) {
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
	
	/**
	 * Specialized parameter processing method for commands with 'fromFile' and
	 * 'toFiles' parameters. 
	 */
	public static String[] processParameters(Options opts, IFileSpec fromFileSpec,
												List<IFileSpec> toFileSpecs,
												String stringParams[],
												IServer server) throws P4JavaException {
		List<String> args = new ArrayList<String>();
		
		addOpts(args, opts, server);
		
		if (stringParams != null) {
			for (String param : stringParams) {
				if (param != null) {
					args.add(param);
				}
			}
		}
		
		addFileSpec(args, fromFileSpec);
		
		return addFileSpecs(args, toFileSpecs);
	}
	
	/**
	 * Specialized parameter processing method for the Client.integrateFiles and
	 * IOptionsServer.getFileDiffs methods.<p>
	 * 
	 * Used due to the need to preserve parameter order in the resulting array; don't
	 * use or change this method unless you really know what you're doing...
	 */
	public static String[] processParameters(Options opts, IFileSpec fromFile, IFileSpec toFile,
			String branchSpec, IServer server) throws P4JavaException {
		List<IFileSpec> fromFiles = new ArrayList<IFileSpec>();
		List<IFileSpec> toFiles = new ArrayList<IFileSpec>();
		if (fromFile != null) {
			fromFiles.add(fromFile);
		}
		if (toFile != null) {
			toFiles.add(toFile);
		}
		return processParameters(opts, fromFiles, toFiles, branchSpec, server);
	}
	
	/**
	 * Specialized parameter processing method for the IOptionsServer.getInterchanges method.
	 * Used due to the need to preserve parameter order in the resulting array; don't
	 * use or change this method unless you really know what you're doing...<p>
	 */
	
	public static String[] processParameters(Options opts,
								List<IFileSpec> fromFiles, List<IFileSpec> toFiles, String branchSpec, IServer server)
										throws P4JavaException {
		// Ensure that the -s option, if it exists, is the last option before the
		// fromFile parameter; interpolate the -b option (if branchSpec isn't null)
		// before the -s.
		
		List<String> args = new ArrayList<String>();
		
		addOpts(args, opts, server);
		
		// Now back out any -s:
		
		boolean hadS = false;
		if (args.contains("-s")) {
			args.remove("-s");
			hadS = true;
		}
		// See if we need to interpolate a "-b" in there at the end:
		
		if (branchSpec != null) {
			args.add("-b" + branchSpec);
		}
		
		// Add the "-s" back at the end if needed:
		if (hadS) {
			args.add("-s");
		}
		
		// Now add the file spec(s):
		
		if (fromFiles != null) {
			for (IFileSpec fromFile : fromFiles) {
				if ((fromFile.getOpStatus() == FileSpecOpStatus.VALID)) {
					args.add(fromFile.getAnnotatedPreferredPathString());
				}
			}
		}
		
		if (toFiles != null) {
			for (IFileSpec toFile : toFiles) {
				if ((toFile.getOpStatus() == FileSpecOpStatus.VALID)) {
					args.add(toFile.getAnnotatedPreferredPathString());
				}
			}
		}
		
		return args.toArray(new String[args.size()]);
	}
	
	protected static String[] addFileSpecs(List<String> args, List<IFileSpec> fileSpecs) {

		if (args == null) {
			throw new NullPointerError("Null args array passed to Parameters.addFileSpecs");
		}
		if (fileSpecs != null) {
			for (IFileSpec fSpec : fileSpecs) {
				addFileSpec(args, fSpec);
			}
		}
		
		return args.toArray(new String[args.size()]);
	}
	
	protected static String[] addFileSpec(List<String> args, IFileSpec fileSpec) {

		if (args == null) {
			throw new NullPointerError("Null args array passed to Parameters.addFileSpec");
		}
		if (fileSpec != null) {
			if ((fileSpec.getOpStatus() == FileSpecOpStatus.VALID)) {
				args.add(fileSpec.getAnnotatedPreferredPathString());
			}
		}
		
		return args.toArray(new String[args.size()]);
	}

	protected static String[] addUnannotatedFileSpecs(List<String> args, List<IFileSpec> fileSpecs) {
		if (args == null) {
			throw new NullPointerError("Null args array passed to Parameters.addUnannotatedFileSpecs");
		}
		if (fileSpecs != null) {
			for (IFileSpec fSpec : fileSpecs) {
				if (fSpec != null) {
					if ((fSpec.getOpStatus() == FileSpecOpStatus.VALID)) {
						args.add(fSpec.getPreferredPathString());
					}
				}
			}
		}
		
		return args.toArray(new String[args.size()]);
	}
	
	protected static void addOpts(List<String> args, Options opts, IServer server)
											throws P4JavaException {
		if (opts != null) {
			if (opts.isImmutable() && (opts.getOptions() != null)) {
				args.addAll(opts.getOptions());
			} else {
				args.addAll(opts.processOptions(server));
			}
		}
	}
}
