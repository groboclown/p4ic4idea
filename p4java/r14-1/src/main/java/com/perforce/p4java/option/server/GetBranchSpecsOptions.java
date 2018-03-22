/**
 *
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for IOptionsServer.getBranchSpecs method.
 *
 * @see com.perforce.p4java.server.IOptionsServer#getBranchSpecs(com.perforce.p4java.option.server.GetBranchSpecsOptions)
 */
public class GetBranchSpecsOptions extends Options {

        /**
         * Options: -u[user], -e[nameFilter], -E[nameFilter], -m[max], -t
         */
        public static final String OPTIONS_SPECS = "s:u s:e s:E i:m:gtz b:t";

        /**
         * If non-null, limit qualifying branches to those owned by the named user.
         * Corresponds to -uname flag.
         */
        protected String userName = null;

        /**
         * If non-null, limits output to branches whose name matches
         * the nameFilter pattern. Corresponds to -enameFilter flag
         */
        protected String nameFilter = null;

        /**
         * If non-null, limits output to branches whose name matches (case-insensitive)
         * the nameFilter pattern. Corresponds to -EnameFilter flag
         */
        protected String caseInsensitiveNameFilter = null;

        /**
         * If greater than zero, limit output to the first maxResults
         * number of branches. Corresponds to -m flag.
         */
        protected int maxResults = 0;

        /**
         * If true, displays the time as well as the date. Corresponds to -t flag.
         */
        protected boolean showTime = false;

        /**
         * Default constructor; sets all fields to null, zero, or false.
         */
        public GetBranchSpecsOptions() {
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
        public GetBranchSpecsOptions(String... options) {
                super(options);
        }

        /**
         * Explicit-value constructor.
         */
        public GetBranchSpecsOptions(String userName, String nameFilter,
                        int maxResults) {
                super();
                this.userName = userName;
                this.nameFilter = nameFilter;
                this.maxResults = maxResults;
        }

        /**
         * Explicit-value constructor.
         */
        public GetBranchSpecsOptions(String userName, String nameFilter,
                        int maxResults, boolean showTime) {
                super();
                this.userName = userName;
                this.nameFilter = nameFilter;
                this.maxResults = maxResults;
                this.showTime = showTime;
        }

        /**
         * Explicit-value constructor.
         */
        public GetBranchSpecsOptions(boolean showTime, String userName, String caseInsensitiveNameFilter, int maxResults) {
                super();
                this.showTime = showTime;
                this.userName = userName;
                this.caseInsensitiveNameFilter = caseInsensitiveNameFilter;
                this.maxResults = maxResults;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                										this.getUserName(),
                										this.getNameFilter(),
                										this.getCaseInsensitiveNameFilter(),
                										this.getMaxResults(),
                										this.isShowTime());
                return this.optionList;
        }

        public String getUserName() {
                return userName;
        }

        public GetBranchSpecsOptions setUserName(String userName) {
                this.userName = userName;
                return this;
        }

        public String getNameFilter() {
                return nameFilter;
        }

        public GetBranchSpecsOptions setNameFilter(String nameFilter) {
                this.nameFilter = nameFilter;
                return this;
        }

        public String getCaseInsensitiveNameFilter() {
                return caseInsensitiveNameFilter;
        }

        public GetBranchSpecsOptions setCaseInsensitiveNameFilter(String caseInsensitiveNameFilter) {
                this.caseInsensitiveNameFilter = caseInsensitiveNameFilter;
                return this;
    	}

        public int getMaxResults() {
                return maxResults;
        }

        public GetBranchSpecsOptions setMaxResults(int maxResults) {
                this.maxResults = maxResults;
                return this;
        }

        public boolean isShowTime() {
                return showTime;
        }

        public GetBranchSpecsOptions setShowTime(boolean showTime) {
                this.showTime = showTime;
                return this;
        }
}
