package com.perforce.p4java.mapapi;

public enum MapTableT {
    LHS(0), 		// do operation on left-hand-side strings
    RHS(1); 		// do operation on right-hand-side strings

    MapTableT(int i) {
        this.dir = i;
    }

    public int dir;
}
