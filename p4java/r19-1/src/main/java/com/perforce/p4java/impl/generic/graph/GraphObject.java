package com.perforce.p4java.impl.generic.graph;

import com.perforce.p4java.graph.IGraphObject;
import org.apache.commons.lang3.Validate;

public class GraphObject implements IGraphObject {

	private final String sha;
	private final String type;

	public GraphObject(String sha, String type) {
		Validate.notBlank(sha, "SHA should not be null or empty");
		Validate.notBlank(type, "Type should not be null or empty");

		this.sha = sha;
		this.type = type;
	}

	@Override
	public String getSha() {
		return sha;
	}

	@Override
	public String getType() {
		return type;
	}
}
