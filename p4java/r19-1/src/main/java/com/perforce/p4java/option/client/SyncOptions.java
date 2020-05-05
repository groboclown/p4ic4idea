/**
 *
 */
package com.perforce.p4java.option.client;

import java.util.List;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Simple default options object for IClient.sync.
 */
public class SyncOptions extends Options {

        /**
         * Options: -f, -n, -k, -p, -q, -s
         */
        public static final String OPTIONS_SPECS = "b:f b:n b:k b:p b:q b:s";

        /** If true, force the update (corresponds to p4 -f flag) */
        protected boolean forceUpdate = false; // -f

        /** If true, don't actually do the update (corresponds to p4 -n flag) */
        protected boolean noUpdate = false;		// -n

        /** If true, bypass the client (corresponds to p4 -k flag) */
        protected boolean clientBypass = false;	// -k

        /** If true, bypass the server (corresponds to p4 -p flag) */
        protected boolean serverBypass = false;	// -p

        /**
         * If true, suppresses normal output messages. Messages regarding
	     * errors or exceptional conditions are not suppressed.
	     * (corresponds to p4 -q flag)
	     */
        protected boolean quiet = false;	// -q

        /**
         * If true, do a safety check before sending content to the client
         * workspace (corresponds to p4 -s flag)
         */
        protected boolean safetyCheck = false; // -s

        /**
         * Default constructor.
         */
        public SyncOptions() {
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
        public SyncOptions(String... options) {
                super(options);
        }

        /**
         * Explicit value constructor.
         */
        public SyncOptions(boolean forceUpdate, boolean noUpdate,
                        boolean clientBypass, boolean serverBypass) {
                super();
                this.forceUpdate = forceUpdate;
                this.noUpdate = noUpdate;
                this.clientBypass = clientBypass;
                this.serverBypass = serverBypass;
        }

        /**
         * Explicit value constructor.
         */
        public SyncOptions(boolean forceUpdate, boolean noUpdate,
                        boolean clientBypass, boolean serverBypass,
                        boolean safetyCheck) {
                super();
                this.forceUpdate = forceUpdate;
                this.noUpdate = noUpdate;
                this.clientBypass = clientBypass;
                this.serverBypass = serverBypass;
                this.safetyCheck = safetyCheck;
        }

        /**
         * @see com.perforce.p4java.option.Options#processOptions(com.perforce.p4java.server.IServer)
         */
        @Override
        public List<String> processOptions(IServer server) throws OptionsException {
                this.optionList = this.processFields(OPTIONS_SPECS,
                                                                this.forceUpdate,
                                                                this.noUpdate,
                                                                this.clientBypass,
                                                                this.serverBypass,
                                                                this.quiet,
                                                                this.safetyCheck);
                return this.optionList;
        }

        public boolean isForceUpdate() {
                return forceUpdate;
        }

        public SyncOptions setForceUpdate(boolean forceUpdate) {
                this.forceUpdate = forceUpdate;
                return this;
        }

        public boolean isNoUpdate() {
                return noUpdate;
        }

        public SyncOptions setNoUpdate(boolean noUpdate) {
                this.noUpdate = noUpdate;
                return this;
        }

        public boolean isClientBypass() {
                return clientBypass;
        }

        public SyncOptions setClientBypass(boolean clientBypass) {
                this.clientBypass = clientBypass;
                return this;
        }

        public boolean isServerBypass() {
                return serverBypass;
        }

        public SyncOptions setServerBypass(boolean serverBypass) {
                this.serverBypass = serverBypass;
                return this;
        }

        public boolean isQuiet() {
                return quiet;
        }

        public SyncOptions setQuiet(boolean quiet) {
                this.quiet = quiet;
                return this;
        }
    
        public boolean isSafetyCheck() {
                return safetyCheck;
        }

        public SyncOptions setSafetyCheck(boolean safetyCheck) {
                this.safetyCheck = safetyCheck;
                return this;
        }
}
