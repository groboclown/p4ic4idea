package com.perforce.p4java.impl.generic.core;

/**
 * Holds data fetched from p4 list command
 */
public class ListData {

	private long totalFileCount;
	private String lable;

	public ListData() {
	}

	public ListData(long totalFileCount, String lable) {
		this.totalFileCount = totalFileCount;
		this.lable = lable;
	}

	public long getTotalFileCount() {
		return totalFileCount;
	}

	public void setTotalFileCount(long totalFileCount) {
		this.totalFileCount = totalFileCount;
	}

	public String getLable() {
		return lable;
	}

	public void setLable(String lable) {
		this.lable = lable;
	}
}
