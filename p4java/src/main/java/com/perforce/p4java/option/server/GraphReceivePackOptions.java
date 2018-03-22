package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * Options required by the graph receve-pack command
 * Usage: receive-pack -n repo [-u user -v] -i files... [ -r refs... | -F refs... | -p packed-refs ]
 * - option -i refer to the .pack file it self e.g ./p4 graph receive-pack -n //graph/scm-plugin -i scm-api-plugin.git/objects/pack/pack-156db553fe00511509f8395aaeb0eed2f0871e9c.pack
 * - option -r refer to the SHA value of the master commit e.g ./p4 graph receive-pack -n //graph/scm-plugin -r master=5631932f5cdf6c3b829911b6fe5ab42d436d74da
 * - option -p refer     to the packed-refs
 */
public class GraphReceivePackOptions extends Options {

	public static final String GRAPH_RECEIVE_PACK_COMMAND_PART = "receive-pack";
	public static final String OPTIONS_SPECS = "s:n s:u s:i s:r s:F s:p s:P b:v";

	/**
	 * Field representing option -n
	 */
	private String repo;

	/**
	 * Field representing option -u
	 */
	private String user;

	/**
	 * Field representing option -i
	 */
	private String file;

	/**
	 * Field representing option -r
	 */
	private String ref;

	/**
	 * Field representing option -F
	 */
	private String forceRef;

	/**
	 * Field representing option -p
	 */
	private String packedRef;

	/**
	 * Field representing option -P
	 */
	private String forcePackedRef;

	/**
	 * Field representing option -v
	 */
	private boolean verbose;

	/**
	 * Constructs receive pack option with the given arguments
	 *
	 * @param repo           Graph repo (-n repo)
	 * @param user           Owner (-u user)
	 * @param file           Pack file (-i file)
	 * @param ref            SHA reference (-r refs...)
	 * @param forceRef       Import at reference (-F refs...)
	 * @param packedRef      Packed Reference (-p packed-refs)
	 * @param forcePackedRef Force option (-P)
	 * @param verbose        Verbose output (-v)
	 * @since 2017.1
	 */
	public GraphReceivePackOptions(String repo, String user, String file, String ref,
	                               String forceRef, String packedRef, String forcePackedRef, boolean verbose) {
		this.repo = repo;
		this.user = user;
		this.file = file;
		this.ref = ref;
		this.forceRef = forceRef;
		this.packedRef = packedRef;
		this.forcePackedRef = forcePackedRef;
		this.verbose = verbose;
	}

	/**
	 * Default constructor
	 */
	public GraphReceivePackOptions() {

	}

	@Override
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.repo,
				this.user, this.file, this.ref, this.forceRef,
				this.packedRef, this.forcePackedRef, this.verbose);

		this.optionList.add(0, GRAPH_RECEIVE_PACK_COMMAND_PART);
		return this.optionList;
	}

	/**
	 * Sets the repository that must exist or will be created by the receive-pack command
	 *
	 * @param repo
	 */
	public void setRepo(String repo) {
		this.repo = repo;
	}

	/**
	 * Returns the repo containing the extracted pack content
	 *
	 * @return
	 */
	public String getRepo() {
		return repo;
	}

	/**
	 * The user who owns the pack file
	 *
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Returns the user who owns the pack file
	 *
	 * @return
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the path to the pack file
	 *
	 * @param file
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * The path to the pack file itself
	 *
	 * @return
	 */
	public String getFile() {
		return file;
	}

	/**
	 * The SHA reference to the master commit
	 *
	 * @return
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * Sets the -r option value
	 *
	 * @param ref
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * Sets the verbosity, option -v, of the command
	 *
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Returns of verbosity is set to true or false
	 *
	 * @return
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * Returns the SHA reference to the commit to receive
	 *
	 * @return
	 */
	public String getPackedRef() {
		return packedRef;
	}

	/**
	 * Sets the SHA representing the master pack
	 *
	 * @param packedRef
	 */
	public void setPackedRef(String packedRef) {
		this.packedRef = packedRef;
	}

	/**
	 * Returns the SHA set for the -F option
	 *
	 * @return
	 */
	public String getForceRef() {
		return forceRef;
	}

	/**
	 * Sets the SHA for -F option
	 *
	 * @param forceRef
	 */
	public void setForceRef(String forceRef) {
		this.forceRef = forceRef;
	}

	/**
	 * Returns the value set for -p option
	 *
	 * @return
	 */
	public String getForcePackedRef() {
		return forcePackedRef;
	}

	/**
	 * Sets the SHA for -p option
	 *
	 * @param forcePackedRef
	 */
	public void setForcePackedRef(String forcePackedRef) {
		this.forcePackedRef = forcePackedRef;
	}
}
