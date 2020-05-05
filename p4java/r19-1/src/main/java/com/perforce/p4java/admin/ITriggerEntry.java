/**
 * Copyright (c) 2010 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.admin;

import com.perforce.p4java.core.IMapEntry;

/**
 * Describes a protection entry (line) in a Perforce triggers
 * table. These are described in more detail in the various
 * main Perforce admin documentation pages.<p>
 */

public interface ITriggerEntry extends IMapEntry {
	
	public enum TriggerType {
		ARCHIVE("archive"),
		AUTH_CHECK("auth-check"),
		AUTH_CHECK_SSO("auth-check-sso"),
		AUTH_SET("auth-set"),
		CHANGE_SUBMIT("change-submit"),
		CHANGE_CONTENT("change-content"),
		CHANGE_COMMIT("change-commit"),
		EDGE_SUBMIT("edge-submit"),
		EDGE_CONTENT("edge-content"),
		FIX_ADD("fix-add"),
		FIX_DELETE("fix-delete"),
		FORM_IN("form-in"),
		FORM_OUT("form-out"),
		FORM_SAVE("form-save"),
		FORM_COMMIT("form-commit"),
		FORM_DELETE("form-delete"),
		SERVICE_CHECK("service-check"),
		SHELVE_SUBMIT("shelve-submit"),
		SHELVE_COMMIT("shelve-commit"),
		SHELVE_DELETE("shelve-delete");
		
	    private final String triggerType;       

	    private TriggerType(String triggerType) {
	        this.triggerType = triggerType;
	    }

		/**
		 * Return a suitable Trigger type as inferred from the passed-in
		 * string, which is assumed to be the string form of a Depot type.
		 * Otherwise return the null.
		 */
		public static TriggerType fromString(String triggerType) {
			if (triggerType != null) {
				for (TriggerType tt : TriggerType.values()) {
					if (triggerType.equalsIgnoreCase(tt.toString())) {
						return tt;
					}
				}
			}
			return null;
		}
	    
	    public String toString(){
	       return this.triggerType;
	    }		
	};

	/**
	 * Gets the trigger name.
	 * 
	 * @return the trigger name
	 */
	String getName();
	
	/**
	 * Sets the trigger name.
	 * 
	 * @param name
	 *            the trigger name
	 */
	void setName(String name);
	
	/**
	 * Gets the trigger type.
	 * 
	 * @return the trigger type
	 */
	TriggerType getTriggerType();
	
	/**
	 * Sets the trigger type.
	 * 
	 * @param type
	 *            the trigger type
	 */
	void setTriggerType(TriggerType type);
	
	/**
	 * For change and submit triggers, a file pattern to match files in the
	 * changelist. This file pattern can be an exclusion mapping (-pattern), to
	 * exclude files. For form triggers, the name of the form (branch, client,
	 * etc). For fix triggers 'fix' is required as the path value. For
	 * authentication triggers, 'auth' is required as the path value. For
	 * archive triggers, a file pattern to match the name of the file being
	 * accessed in the archive. Note that, due to lazy copying when branching
	 * files, the name of the file in the archive can not be the same as the
	 * name of the file in the depot. For command triggers, use the name of the
	 * command to match, e.g. 'pre-user-$cmd' or a regular expression, e.g.
	 * '(pre|post)-user-add'. *
	 * 
	 * @return the depot file path pattern or form type
	 */
	String getPath();
	
	/**
	 * For change and submit triggers, a file pattern to match files in the
	 * changelist. This file pattern can be an exclusion mapping (-pattern), to
	 * exclude files. For form triggers, the name of the form (branch, client,
	 * etc). For fix triggers 'fix' is required as the path value. For
	 * authentication triggers, 'auth' is required as the path value. For
	 * archive triggers, a file pattern to match the name of the file being
	 * accessed in the archive. Note that, due to lazy copying when branching
	 * files, the name of the file in the archive can not be the same as the
	 * name of the file in the depot. For command triggers, use the name of the
	 * command to match, e.g. 'pre-user-$cmd' or a regular expression, e.g.
	 * '(pre|post)-user-add'. *
	 * 
	 * @param path
	 *            the depot file path pattern or form type
	 */
	void setPath(String path);

	/**
	 * Gets the trigger command. If the command contains spaces, enclose it in
	 * double quotes.
	 * 
	 * @return the trigger comamnd
	 */
	String getCommand();
	
	/**
	 * Sets the trigger command. If the command contains spaces, enclose it in
	 * double quotes.
	 * 
	 * @param command
	 *            the trigger command
	 */
	void setCommand(String command);
}
