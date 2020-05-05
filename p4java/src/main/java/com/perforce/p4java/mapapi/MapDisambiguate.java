package com.perforce.p4java.mapapi;

import static com.perforce.p4java.mapapi.MapFlag.MfUnmap;

public class MapDisambiguate extends MapJoiner {

	public void insert() {
		newLhs = map.lhs().expand(this.data, params2);
		newRhs = map.rhs().expand(this.data, params2);
		m0.insertNoDups(newLhs, newRhs, MfUnmap);
	}
}
