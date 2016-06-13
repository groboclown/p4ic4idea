/**
 *
 */
package com.perforce.p4java.option.server;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Options class for the server's getClients method.
 *
 * @see com.perforce.p4java.server.IOptionsServer#getClients(com.perforce.p4java.option.server.GetClientsOptions)
 */
public class GetClientsOptions extends Options {

        /**
         * Options: -m[max], -u[user], -e[nameFilter], -E[nameFilter], -t, -S[stream]
         * <p>
         * Options: -U
         */
        public static final String OPTIONS_SPECS = "i:m:gtz s:u s:e s:E b:t s:S b:U";

        /**
         * If non-null, limit qualifying clients to those owned by the named user.
         * Corresponds to -uname flag.
         */
        protected String userName = null;

        /**
         * If non-null, limits output to clients whose name matches
         * the nameFilter pattern. Corresponds to -enameFilter flag
         */
        protected String nameFilter = null;

        /**
         * If non-null, limits output to clients whose name matches (case-insensitive)
         * the nameFilter pattern. Corresponds to -EnameFilter flag
         */
        protected String caseInsensitiveNameFilter = null;

        /**
         * If greater than zero, limit output to the first maxResults
         * number of clients. Corresponds to -m flag.
         */
        protected int maxResults = 0;

        /**
         * If true, displays the time as well as the date. Corresponds to -t flag.
         */
        protected boolean showTime = false;

        /**
         * If non-null, limits output to the client workspaces dedicated to the
         * stream. Corresponds to the "-S stream" flag.
         */
        protected String stream = null;
        
        /**
         * If true, lists unloaded clients. Corresponds to -U flag.
         */
        protected boolean unloaded = false;

        /**
         * Default constructor.
         */
        public GetClientsOptions() {
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
        public GetClientsOptions(String... options) {
                super(options);
        }

        /**
         * Explicit-value constructor.
         */
        public GetClientsOptions(int maxResults, String userName, String nameFilter) {
                super();
                this.maxResults = maxResults;
                this.userName = userName;
                this.nameFilter = nameFilter;
        }

        /**
         * Explicit-value constructor.
         */
        public GetClientsOptions(int maxResults, String userName, String nameFilter, boolean showTime) {
                super();
                this.maxResults = maxResults;
                this.userName = userName;
                this.nameFilter = nameFilter;
                this.showTime = showTime;
        }

        /**
         * Explicit-value constructor.
         */
        public GetClientsOptions(int maxResults, String userName, String nameFilter, boolean showTime, String stream) {
                super();
                this.maxResults = maxResults;
                this.userName = userName;
                this.nameFilter = nameFilter;
                this.showTime = showTime;
                this.stream = stream;
        }

        /**
         * Explicit-value constructor.
         */
        public GetClientsOptions(boolean showTime, String userName, String caseInsensitiveNameFilter, int maxResults, String stream) {
                super();
                this.showTime = showTime;
                this.userName = userName;
                this.caseInsensitiveNameFilter = caseInsensitiveNameFilter;
                this.maxResults = maxResults;
                this.stream = stream;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                										this.getMaxResults(),
                										this.getUserName(),
                										this.getNameFilter(),
                										this.getCaseInsensitiveNameFilter(),
                										this.isShowTime(),
                										this.getStream(),
                										this.isUnloaded());
                return this.optionList;
        }

        public int getMaxResults() {
                return maxResults;
        }

        public GetClientsOptions setMaxResults(int maxResults) {
                this.maxResults = maxResults;
                return this;
        }

        public String getUserName() {
                return userName;
        }

        public GetClientsOptions setUserName(String userName) {
                this.userName = userName;
                return this;
        }

        public String getNameFilter() {
                return nameFilter;
        }

        public GetClientsOptions setNameFilter(String nameFilter) {
                this.nameFilter = nameFilter;
                return this;
        }

        public String getCaseInsensitiveNameFilter() {
                return caseInsensitiveNameFilter;
        }

        public GetClientsOptions setCaseInsensitiveNameFilter(String caseInsensitiveNameFilter) {
                this.caseInsensitiveNameFilter = caseInsensitiveNameFilter;
                return this;
        }

        public boolean isShowTime() {
                return showTime;
        }

        public GetClientsOptions setShowTime(boolean showTime) {
                this.showTime = showTime;
                return this;
        }

        public String getStream() {
                return stream;
        }

        public GetClientsOptions setStream(String stream) {
                this.stream = stream;
                return this;
        }

        public boolean isUnloaded() {
            return unloaded;
        }

        public GetClientsOptions setUnloaded(boolean unloaded) {
            this.unloaded = unloaded;
            return this;
        }
}
