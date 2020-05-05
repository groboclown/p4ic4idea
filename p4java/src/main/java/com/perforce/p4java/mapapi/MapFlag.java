package com.perforce.p4java.mapapi;

public enum MapFlag {
	MfMap(0),        // map
	MfUnmap(1),    // -map
	MfRemap(2),    // +map
	MfHavemap(3),    // $map
	MfChangemap(4),    // @map
	MfAndmap(5),    // &map
	// empty
	// empty
	// empty
	// empty
	MfStream1(10),    // Stream, Paths
	MfStream2(11),    // Stream, Remapped
	MfStream3(12),    // Stream, Ignored
	MfStream5(14),    // Stream, Paths: shared
	MfStream6(15),    // Stream, Paths: public
	MfStream7(16),    // Stream, Paths: private
	MfStream8(17);    // Stream, Paths: excluded

	MapFlag(int i) {
		this.code = i;
	}

	public int code;
}
