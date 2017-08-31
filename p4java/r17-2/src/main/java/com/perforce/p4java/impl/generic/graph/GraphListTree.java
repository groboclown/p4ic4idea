package com.perforce.p4java.impl.generic.graph;

import com.perforce.p4java.graph.IGraphListTree;
import org.apache.commons.lang3.Validate;

public class GraphListTree implements IGraphListTree {

	private final int mode;
	private final String type;
	private final String sha;
	private final String name;

	public GraphListTree(int mode, String type, String sha, String name) {
		Validate.notBlank(name, "Name should not be null or empty");
		Validate.notBlank(sha, "SHA should not be null or empty");
		Validate.notBlank(type, "Type should not be null or empty");

		this.mode = mode;
		this.type = type;
		this.sha = sha;
		this.name = name;
	}

	@Override
	public int getMode() {
		return mode;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getSha() {
		return sha;
	}

	@Override
	public String getName() {
		return name;
	}
}
