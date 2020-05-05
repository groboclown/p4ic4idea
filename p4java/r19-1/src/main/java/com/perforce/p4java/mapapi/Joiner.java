package com.perforce.p4java.mapapi;

public class Joiner {

	public Joiner() {
		clear();
	}

	public void insert() {
	}

	public void extend(String ex) {
		data += ex;
	}

	public void extend(char ex) {
		data += ex;
	}

	public int length() {
		return data.length();
	}

	public void setLength(int length) {
		data = data.substring(0, length);
	}

	public void clear() {
		data = "";
		params = new MapParams();
		params2 = new MapParams();
		badJoin = false;
	}

	boolean badJoin;
	MapParams params;
	MapParams params2;
	String data;
}
