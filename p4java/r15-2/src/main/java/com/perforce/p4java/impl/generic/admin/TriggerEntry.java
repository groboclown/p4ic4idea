/**
 * Copyright (c) 2014 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.admin;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.p4java.admin.ITriggerEntry;
import com.perforce.p4java.impl.generic.core.MapEntry;

/**
 * Default ITriggerEntry implementation class.
 * <p>
 * 
 * Note that the order of this trigger entry in the triggers table is part of
 * the trigger entry key when pass to the server for updating the triggers
 * table.
 * <p>
 * 
 * <pre>
 * Triggers0 example1 change-submit //depot/... "echo %changelist%"
 * Triggers1 example1 change-submit //depot/abc/... "echo %changelist%"
 * Triggers2 example2 form-save client "echo %client%"
 * Triggers3 example3 change-submit //depot/... "echo %changelist%"
 * Triggers4 example4 change-submit //depot/... "echo %changelist%"
 * </pre>
 */
public class TriggerEntry extends MapEntry implements ITriggerEntry {

    /**
     * Regular expression pattern for splitting a string by whitespace and
     * sequences of characters that begin and end with a quote.
     */
    private static final String TRIGGER_ENTRY_TOKEN_REGEX_PATTERN = "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'";

	/**
	 * The trigger name.
	 */
	private String name = null;

	/**
	 * The trigger type.
	 */
	private TriggerType triggerType = null;
	
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
	 */
	private String path = null;

	/**
	 * The trigger command. If the command contains spaces, enclose it in double
	 * quotes.
	 */
	private String command = null;

	/**
	 * Default constructor.
	 */
	public TriggerEntry() {
	}

	/**
	 * Explicit-value constructor.
	 */
	public TriggerEntry(int order, String name, TriggerType triggerType, String path, String command) {
		super(order, null);
		
		this.name = name;
		this.triggerType = triggerType;
		this.path = quoteWhitespaceString(path);
		this.command = quoteWhitespaceString(command);
	}

	/**
	 * Constructs a TriggerEntry from the passed-in trigger as a string and its
	 * order.
	 */
	public TriggerEntry(String triggerEntry, int order) {
		if (triggerEntry != null) {
			List<String> parts = parseTriggerEntry(triggerEntry);
			// Each trigger line must be indented with spaces or tabs in the
			// form. Each line has four elements: trigger name, a trigger type,
			// a depot file path pattern or form type, and a command to run
			// (command must be quoted if there is whitespace).
			if (parts != null && parts.size() == 4) {
				this.order = order;
				this.name = parts.get(0);
				this.triggerType = ITriggerEntry.TriggerType.fromString(parts.get(1));
				this.path = quoteWhitespaceString(parts.get(2));
				this.command = quoteWhitespaceString(parts.get(3));
			}
		}
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#getName()
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#getTriggerType()
	 */
	public TriggerType getTriggerType() {
		return this.triggerType;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#setTriggerType(com.perforce.p4java.admin.ITriggerEntry.TriggerType)
	 */
	public void setTriggerType(TriggerType triggerType) {
		this.triggerType = triggerType;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#getPath()
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#setPath(java.lang.String)
	 */
	public void setPath(String path) {
		this.path = quoteWhitespaceString(path);
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#getCommand()
	 */
	public String getCommand() {
		return this.command;
	}

	/**
	 * @see com.perforce.p4java.admin.ITriggerEntry#setCommand(java.lang.String)
	 */
	public void setCommand(String command) {
		this.command = quoteWhitespaceString(command);
	}

    private List<String> parseTriggerEntry(String triggerEntry) {
        List<String> list = new LinkedList<String>();
        // Tokenizing by whitespace and content inside quotes.
        if (triggerEntry != null) {
            // Split the string by whitespace and sequences of characters that
            // begin and end with a quote.
            Pattern pattern = Pattern.compile(TRIGGER_ENTRY_TOKEN_REGEX_PATTERN);
            Matcher regexMatcher = pattern.matcher(triggerEntry);
            while (regexMatcher.find()) {
                if (regexMatcher.groupCount() > 0) {
                    if (regexMatcher.group(1) != null) {
                        // Add double-quoted string without the quotes.
                        list.add(regexMatcher.group(1));
                    } else if (regexMatcher.group(2) != null) {
                        // Add single-quoted string without the quotes.
                        list.add(regexMatcher.group(2));
                    } else {
                        // Add unquoted word
                        list.add(regexMatcher.group());
                    }
                }
            }
        }
        return list;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.name != null) {
        	sb.append(this.name);
        }
        if (this.triggerType != null) {
        	sb.append(" ").append(this.triggerType.toString());
        }
        if (this.path != null) {
        	sb.append(" ").append(this.path);
        }
        if (this.command != null) {
        	sb.append(" ").append(this.command);
        }
        return sb.toString();
    }
}
