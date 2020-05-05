package com.perforce.p4java.graph;

/**
 *
 */
public enum CommitAction {

	ADD("add"), EDIT("edit"), DELETE("delete"), MERGE("merge"), UNKNOWN("unknown");

	private String key;

	CommitAction(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	public static CommitAction parse(String key)
	{
		for(CommitAction action : values())
		{
			if(key.equalsIgnoreCase(action.toString()))
			{
				return action;
			}
		}
		return UNKNOWN;
	}
}
