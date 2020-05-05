/**
 *
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getStreams method.
 *
 * @see com.perforce.p4java.server.IOptionsServer#getStreams(List, GetStreamsOptions)
 */
public class GetStreamsOptions extends Options {

        /**
         * Options: -U -F["filter"], -T["fields"], -m[max]
         */
        public static final String OPTIONS_SPECS = "b:U s:F s:T i:m:gtz";
        
        /**
         * If true, lists unloaded task streams (see 'p4 help unload').
         * Corresponds to -U flag.
         */
        protected boolean unloaded = false;

        /**
         * The -F filter flag limits the output to files satisfying the
         * expression given as 'filter'. This filter expression is similar to
         * the one used by 'jobs -e jobview',  except that fields must match
         * those above and are case sensitive. <p>
         * 
         *  e.g. -F "Parent=//Ace/MAIN & Type=development"
         */
        protected String filter = null;
     
        /**
         * The -T fields flag (used with tagged output) limits the fields output
         * to those specified by a list given as 'fields'. These field names can
         * be separated by a space or a comma. <p>
         * 
         * e.g. -T "Stream, Owner"
         */
        protected String fields = null;
        
        /**
         * If greater than zero, limit output to the first maxResults
         * number of branches. Corresponds to -m flag.
         */
        protected int maxResults = 0;

        /**
         * Default constructor.
         */
        public GetStreamsOptions() {
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
        public GetStreamsOptions(String... options) {
                super(options);
        }

        /**
         * Explicit-value constructor.
         */
        public GetStreamsOptions(String filter, String fields,
                        int maxResults) {
                super();
                this.filter = filter;
                this.fields = fields;
                this.maxResults = maxResults;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
													this.isUnloaded(),
                									this.getFilter(),
                									this.getFields(),
                									this.getMaxResults());
                return this.optionList;
        }

        public boolean isUnloaded() {
            	return unloaded;
        }

        public GetStreamsOptions setUnloaded(boolean unloaded) {
            	this.unloaded = unloaded;
            	return this;
    	}
        
        public String getFilter() {
                return filter;
        }

        public GetStreamsOptions setFilter(String filter) {
                this.filter = filter;
                return this;
        }

        public String getFields() {
                return fields;
        }

        public GetStreamsOptions setFields(String fields) {
                this.fields = fields;
                return this;
        }

        public int getMaxResults() {
                return maxResults;
        }

        public GetStreamsOptions setMaxResults(int maxResults) {
                this.maxResults = maxResults;
                return this;
        }
}
