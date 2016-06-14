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
 * Options class for IOptionsServer.getClientTemplate method.
 * 
 * @see com.perforce.p4java.server.IOptionsServer#getClientTemplate(java.lang.String, com.perforce.p4java.option.server.GetClientTemplateOptions)
 */
public class GetClientTemplateOptions extends Options {
	
	/**
	 * Options: -S[stream], -c[changelist]
	 */
	public static final String OPTIONS_SPECS = "s:S i:c:gtz";

	/**
	 * If not null, '-S stream' flag can be used with '-o -c change' to inspect
	 * an old stream client view. It yields the client spec that would have been
	 * created for the stream at the moment the change was recorded. The stream's
	 * path in a stream depot, of the form //depotname/streamname.
	 */
	protected String stream = null;
	
	/**
	 * If positive, it yields the client spec that would have been created for
	 * the stream at the moment the change was recorded.
	 */
	protected int changelistId = IChangelist.DEFAULT;
	
	/**
	 * If true, return a client even if it exists. Note that this option is not
	 * processed; this option is used solely post-command-issuance in Server
	 */
	protected boolean allowExistent = false;

	/**
	 * Default constructor.
	 */
	public GetClientTemplateOptions() {
		super();
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
	public GetClientTemplateOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit value constructor.
	 */
	public GetClientTemplateOptions(boolean allowExistent) {
		super();
		this.allowExistent = allowExistent;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.getStream(),
											this.getChangelistId());
		return this.optionList;
	}

	public String getStream() {
		return stream;
	}

	public GetClientTemplateOptions setStream(String stream) {
		this.stream = stream;
		return this;
	}

	public int getChangelistId() {
		return changelistId;
	}

	public GetClientTemplateOptions setChangelistId(int changelistId) {
		this.changelistId = changelistId;
		return this;
	}
	
	public boolean isAllowExistent() {
		return allowExistent;
	}

	public GetClientTemplateOptions setAllowExistent(boolean allowExistent) {
		this.allowExistent = allowExistent;
		return this;
	}
}
