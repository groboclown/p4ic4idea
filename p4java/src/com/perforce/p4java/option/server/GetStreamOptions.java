/**
 *
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getStream method.
 *
 * @see com.perforce.p4java.server.IOptionsServer#getStream(java.lang.String, com.perforce.p4java.option.server.GetStreamOptions)
 */
public class GetStreamOptions extends Options {

        /**
         * Options: -v
         */
        public static final String OPTIONS_SPECS = "b:v";

    	/**
    	 * If true, expose the automatically generated client view for this stream.
    	 * Corresponds to -v.
    	 */
    	protected boolean exposeClientView = false;

        /**
         * Default constructor.
         */
        public GetStreamOptions() {
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
        public GetStreamOptions(String... options) {
                super(options);
        }

        /**
         * Explicit-value constructor.
         */
        public GetStreamOptions(boolean exposeClientView) {
                super();
                this.exposeClientView = exposeClientView;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                									this.isExposeClientView());
                return this.optionList;
        }

        public boolean isExposeClientView() {
                return exposeClientView;
        }

        public GetStreamOptions setExposeClientView(boolean exposeClientView) {
                this.exposeClientView = exposeClientView;
                return this;
        }
}
