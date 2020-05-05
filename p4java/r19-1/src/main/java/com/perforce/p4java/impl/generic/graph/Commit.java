package com.perforce.p4java.impl.generic.graph;

import com.perforce.p4java.graph.CommitAction;
import com.perforce.p4java.graph.ICommit;
import org.apache.commons.lang3.Validate;

import java.util.Date;
import java.util.List;

public class Commit implements ICommit {

	private final String commit;
	private final String tree;
	private final CommitAction action;
	private final List<String> parent;
	private final String author;
	private final String authorEmail;
	private final Date date;
	private final String committer;
	private final String committerEmail;
	private final Date committerDate;
	private final String description;

	public Commit(String commit, String tree, CommitAction action, List<String> parent,
	              String author, String authorEmail, Date date,
	              String committer, String committerEmail, Date committerDate,
	              String description) {

		Validate.notBlank(commit, "commit should not be null or empty");
		Validate.notBlank(tree, "tree should not be null or empty");
		Validate.notNull(author, "author should not be null");
		Validate.notNull(committer, "committer should not be null");
		Validate.notNull(description, "description should not be null");

		this.commit = commit;
		this.tree = tree;
		this.action = action;
		this.parent = parent;
		this.author = author;
		this.authorEmail = authorEmail;
		this.date = date;
		this.committer = committer;
		this.committerEmail = committerEmail;
		this.committerDate = committerDate;
		this.description = description;
	}

	@Override
	public String getCommit() {
		return commit;
	}

	@Override
	public String getTree() {
		return tree;
	}

	@Override
	public CommitAction getAction() {
		return action;
	}

	@Override
	public List<String> getParents() {
		return parent;
	}

	@Override
	public String getAuthor() {
		return author;
	}

	@Override
	public String getAuthorEmail() {
		return authorEmail;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String getCommitter() {
		return committer;
	}

	@Override
	public String getCommitterEmail() {
		return committerEmail;
	}

	@Override
	public Date getCommitterDate() {
		return committerDate;
	}

	@Override
	public String getDescription() {
		return description;
	}
}
