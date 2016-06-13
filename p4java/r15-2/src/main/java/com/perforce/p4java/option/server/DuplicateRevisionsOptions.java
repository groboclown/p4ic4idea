/**
 *
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the IOptionsServer duplicateRevisions method.
 *
 * @since 2012.2
 */
public class DuplicateRevisionsOptions extends Options {

        /**
         * Options: -n, -q
         */
        public static final String OPTIONS_SPECS = "b:n b:q";

        /** If true, don't actually do the duplicate. Corresponds to -n flag. */
        protected boolean noUpdate = false;

        /**
         * If true, suppresses the warning about target revisions already
         * existing. Corresponds to -q flag.
         */
        protected boolean suppressWarning = false;

        /**
         * Default constructor.
         */
        public DuplicateRevisionsOptions() {
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
        public DuplicateRevisionsOptions(String... options) {
                super(options);
        }

        /**
         * Explicit-value constructor.
         */
        public DuplicateRevisionsOptions(boolean noUpdate, boolean suppressWarning) {
                super();
                this.noUpdate = noUpdate;
                this.suppressWarning = suppressWarning;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                					this.isNoUpdate(),
                					this.isSuppressWarning());
                return this.optionList;
        }

        public boolean isNoUpdate() {
                return noUpdate;
        }

        public DuplicateRevisionsOptions setNoUpdate(boolean noUpdate) {
                this.noUpdate = noUpdate;
                return this;
        }

        public boolean isSuppressWarning() {
                return suppressWarning;
        }

        public DuplicateRevisionsOptions setSuppressWarning(boolean suppressWarning) {
                this.suppressWarning = suppressWarning;
                return this;
        }
}
