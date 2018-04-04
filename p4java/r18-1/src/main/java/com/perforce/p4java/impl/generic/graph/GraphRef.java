package com.perforce.p4java.impl.generic.graph;

import com.perforce.p4java.graph.IGraphRef;

public class GraphRef implements IGraphRef {

	private final String repo;
	private final String type;
	private final String sha;
	private final String name;

	/**
	 * Default constructor
	 */
	public GraphRef(final String repo, final String type, final String sha, final String name) {
		this.repo = repo;
		this.type = type;
		this.sha = sha;
		this.name = name;
	}

	@Override
	public String getRepo() {
		return repo;
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
