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
	 * @param repo - the repo containing the extracted pack content
	 */
	public void setRepo(String repo) {
		this.repo = repo;
	}

	/**
	 * @return the repo containing the extracted pack content
	 */
	public String getRepo() {
		return repo;
	}

	/**
	 * The user who owns the pack file
	 *
	 * @param user - the user who owns the pack file
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the user who owns the pack file
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Sets the path to the pack file
	 *
	 * @param file - The path to the pack file itself
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * @return - The path to the pack file itself
	 */
	public String getFile() {
		return file;
	}

	/**
	 * The SHA reference to the master commit
	 *
	 * @return Sets the -r option value
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * @param ref - Sets the -r option value
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * Sets the verbosity, option -v, of the command
	 *
	 * @param verbose - true or false
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * @return verbosity is set to true or false
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * @return Returns the SHA reference to the commit to receive
	 */
	public String getPackedRef() {
		return packedRef;
	}

	/**
	 * Sets the SHA representing the master pack
	 *
	 * @param packedRef - the SHA reference to the commit to receive
	 */
	public void setPackedRef(String packedRef) {
		this.packedRef = packedRef;
	}

	/**
	 * @return the SHA set for the -F option
	 */
	public String getForceRef() {
		return forceRef;
	}

	/**
	 * Sets the SHA for -F option
	 *
	 * @param forceRef - the SHA set for the -F option
	 */
	public void setForceRef(String forceRef) {
		this.forceRef = forceRef;
	}

	/**
	 * @return the SHA for -p option
	 */
	public String getForcePackedRef() {
		return forcePackedRef;
	}

	/**
	 * Sets the SHA for -p option
	 *
	 * @param forcePackedRef - the SHA for -p option
	 */
	public void setForcePackedRef(String forcePackedRef) {
		this.forcePackedRef = forcePackedRef;
	}
}
