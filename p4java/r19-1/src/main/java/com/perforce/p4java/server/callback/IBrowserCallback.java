package com.perforce.p4java.server.callback;

import com.perforce.p4java.exception.InvalidUrlException;

public interface IBrowserCallback {

	/**
	 *
	 * @param url
	 */
	public void launchBrowser(String url) throws Exception;

}
