/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer getInterchanges methods. Serves
 * for both getInterchanges method signatures, but not all options
 * are honored by both methods -- check the corresponding main Perforce
 * documentation.
 */
public class GetInterchangesOptions extends Options {
	
	/**
	 * Options: -f, -l, -C[changelist], -b[branch], -S[stream], -P[parentStream], -r, -s
	 * <p>
	 * 
	 * NOTE: not all options will be recognized by the specific version of the
	 * getInterchanges method being called.
	 */
	public static final String OPTIONS_SPECS = "b:f b:l i:C:gtz s:b s:S s:P b:r b:s";
	
	/**
	 * If true, show the individual files that would require integration.
	 * Corresponds to -f flag.
	 */
	protected boolean showFiles = false;
	
	/**
	 * If true, produce long output with the full text of the
	 * changelist descriptions. Corresponds to -l flag.
	 */
	protected boolean longDesc = false;
	
	/**
	 * If greater than zero, only consider integration history from changelists
	 * at or below the given number. Corresponds to the undoc -C flag.
	 */
	protected int maxChangelistId = IChangelist.UNKNOWN;
	
	/**
	 * If true, reverse the mappings in the branch view, with the
	 * target files and source files exchanging place. Correspsonds
	 * to the -r flag.
	 */
	protected boolean reverseMapping = false;
	
	/**
	 * If true, causes the branch view to work
	 * bidirectionally, where the scope of the command is limited to
	 * integrations whose 'from' files match fromFile[revRange].
	 * Corresponds to the -s flag.
	 */
	protected boolean biDirectional = false;

    /**
     * If non-null, use a user-defined branch view. The source is the left
     * side of the branch view and the target is the right side. With -r,
     * the direction is reversed. Corresponds to -b flag.
     */
    protected String branch = null;

    /**
     * If not null, makes 'p4 interchanges' use a stream's branch view. The
     * source is the stream itself, and the target is the stream's parent.
     * With -r, the direction is reversed.  -P can be used to specify a
     * parent stream other than the stream's actual parent. Note that to
     * submit integrated stream files, the current client must be dedicated
     * to the target stream. Corresponds to -S flag.
     */
    protected String stream = null;
    
    /**
     * If non-null, specify a parent stream other than the stream's actual
     * parent. Corresponds to -P flag.
     */
    protected String parentStream = null;

    /**
	 * Default constructor. 
	 */
	public GetInterchangesOptions() {
	}

	/**
	 * Strings-based constructor; see 'p4 help [command]' for possible options.
	 * <p>
	 * 
	 * <b>WARNING: you should not pass more than one option or argument in each
	 * string parameter. Each option or argument should be passed-in as its own
	 * separate string parameter, without any spaces between the option and the
	 * option value (if any).<b>
	 * <p>
	 * 
	 * <b>NOTE: setting options this way always bypasses the internal options
	 * values, and getter methods against the individual values corresponding to
	 * the strings passed in to this constructor will not normally reflect the
	 * string's setting. Do not use this constructor unless you know what you're
	 * doing and / or you do not also use the field getters and setters.</b>
	 * 
	 * @see com.perforce.p4java.option.Options#Options(java.lang.String...)
	 */
	public GetInterchangesOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public GetInterchangesOptions(boolean showFiles, boolean longDesc,
			int maxChangelistId, boolean reverseMapping, boolean biDirectional) {
		super();
		this.showFiles = showFiles;
		this.longDesc = longDesc;
		this.maxChangelistId = maxChangelistId;
		this.reverseMapping = reverseMapping;
		this.biDirectional = biDirectional;
	}

    /**
     * Explicit-value constructor for use with a branch.
     */
	public GetInterchangesOptions(boolean showFiles, boolean longDesc,
			int maxChangelistId, String branch, boolean reverseMapping,
			boolean biDirectional) {
		super();
		this.showFiles = showFiles;
		this.longDesc = longDesc;
		this.maxChangelistId = maxChangelistId;
		this.branch = branch;
		this.reverseMapping = reverseMapping;
		this.biDirectional = biDirectional;
	}

	/**
     * Explicit-value constructor for use with a stream.
     */
	public GetInterchangesOptions(boolean showFiles, boolean longDesc,
			int maxChangelistId, String stream, String parentStream,
			boolean reverseMapping, boolean biDirectional) {
		super();
		this.showFiles = showFiles;
		this.longDesc = longDesc;
		this.maxChangelistId = maxChangelistId;
		this.stream = stream;
		this.parentStream = parentStream;
		this.reverseMapping = reverseMapping;
		this.biDirectional = biDirectional;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
								this.isShowFiles(),
								this.isLongDesc(),
								this.getMaxChangelistId(),
								this.getBranch(),
								this.getStream(),
								this.getParentStream(),
								this.isReverseMapping(),
								this.isBiDirectional());
		return optionList;
	}

	public boolean isShowFiles() {
		return showFiles;
	}

	public GetInterchangesOptions setShowFiles(boolean showFiles) {
		this.showFiles = showFiles;
		return this;
	}

	public boolean isLongDesc() {
		return longDesc;
	}

	public GetInterchangesOptions setLongDesc(boolean longDesc) {
		this.longDesc = longDesc;
		return this;
	}

	public int getMaxChangelistId() {
		return maxChangelistId;
	}

	public GetInterchangesOptions setMaxChangelistId(int maxChangelistId) {
		this.maxChangelistId = maxChangelistId;
		return this;
	}

	public boolean isReverseMapping() {
		return reverseMapping;
	}

	public GetInterchangesOptions setReverseMapping(boolean reverseMapping) {
		this.reverseMapping = reverseMapping;
		return this;
	}

	public boolean isBiDirectional() {
		return biDirectional;
	}

	public GetInterchangesOptions setBiDirectional(boolean biDirectional) {
		this.biDirectional = biDirectional;
		return this;
	}

    public String getBranch() {
        return branch;
    }

    public GetInterchangesOptions setBranch(String branch) {
        this.branch = branch;
        return this;
    }

	public String getStream() {
		return stream;
	}
	
	public GetInterchangesOptions setStream(String stream) {
		this.stream = stream;
		return this;
	}
	
	public String getParentStream() {
		return parentStream;
	}
	
	public GetInterchangesOptions setParentStream(String parentStream) {
		this.parentStream = parentStream;
		return this;
	}

}
