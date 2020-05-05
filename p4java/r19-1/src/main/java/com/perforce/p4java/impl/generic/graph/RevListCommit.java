package com.perforce.p4java.impl.generic.graph;

import com.perforce.p4java.graph.IRevListCommit;

/**
 *
 * Represents a list of revision items made through Graph Depot functionality
 */
public class RevListCommit implements IRevListCommit {


    private final String commit;

    /**
     * Default constructor
     */
    public RevListCommit(final String commit)
    {
        this.commit = commit;
    }


    @Override
    public String getCommit() {
        return commit;
    }
}
