/**
 * 
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Combined Options subclass for the server getStreamCachedIntegrationStatus
 * methods.
 */

public class StreamIntegrationStatusOptions extends Options {

	/**
	 * Options: -r, -a, -c, -s
	 */
	public static final String OPTIONS_SPECS = "b:r b:a b:c b:s";

	/**
	 * If true, shows the status of integration to the stream from its parent.
	 * By default, status of integration in the other direction is shown, from
	 * the stream to its parent. Corresponds to the -r flag.
	 */
	public boolean parentToStream = false;

	/**
	 * If true, shows status of integration in both directions. Corresponds to
	 * the -a flag.
	 */
	public boolean bidirectional = false;

	/**
	 * If true, forces 'istat' to assume the cache is stale; it causes a search
	 * for pending integrations. Use of this flag can impact server performance.
	 * Corresponds to the -c flag.
	 * */
	public boolean forceUpdate = false;

	/**
	 * If true, shows the cached state without refreshing stale data.
	 * Corresponds to the -s flag.
	 */
	public boolean noRefresh = false;

	/**
	 * Default constructor.
	 */
	public StreamIntegrationStatusOptions() {
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
	public StreamIntegrationStatusOptions(String... options) {
		super(options);
	}

	/**
	 * Explicit-value constructor.
	 */
	public StreamIntegrationStatusOptions(boolean parentToStream,
			boolean bidirectional, boolean forceUpdate, boolean noRefresh) {
		super();
		this.parentToStream = parentToStream;
		this.bidirectional = bidirectional;
		this.forceUpdate = forceUpdate;
		this.noRefresh = noRefresh;
	}

	/**
	 * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
	 */
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS,
											this.isParentToStream(),
											this.isBidirectional(),
											this.isForceUpdate(),
											this.isNoRefresh());
		return this.optionList;
	}

	public boolean isParentToStream() {
		return parentToStream;
	}

	public StreamIntegrationStatusOptions setParentToStream(
			boolean parentToStream) {
		this.parentToStream = parentToStream;
		return this;
	}

	public boolean isBidirectional() {
		return bidirectional;
	}

	public StreamIntegrationStatusOptions setBidirectional(
			boolean bidirectional) {
		this.bidirectional = bidirectional;
		return this;
	}

	public boolean isForceUpdate() {
		return forceUpdate;
	}

	public StreamIntegrationStatusOptions setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
		return this;
	}

	public boolean isNoRefresh() {
		return noRefresh;
	}

	public StreamIntegrationStatusOptions setNoRefresh(boolean noRefresh) {
		this.noRefresh = noRefresh;
		return this;
	}
}
