package com.perforce.p4java.graph;

import java.util.Date;
import java.util.List;

public interface ICommit {

	String getCommit();

	String getTree();

	CommitAction getAction();

	List<String> getParents();

	String getAuthor();

	String getAuthorEmail();

	Date getDate();

	String getCommitter();

	String getCommitterEmail();

	Date getCommitterDate();

	String getDescription();
}
