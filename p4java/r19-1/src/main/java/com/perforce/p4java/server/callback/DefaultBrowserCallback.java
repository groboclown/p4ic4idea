package com.perforce.p4java.server.callback;

import com.perforce.p4java.exception.InvalidUrlException;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DefaultBrowserCallback implements IBrowserCallback {

	private Desktop desktop;

	public DefaultBrowserCallback() {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			this.desktop = Desktop.getDesktop();
		} else {
			desktop = null;
		}
	}

	@Override
	public void launchBrowser(String urlString) throws Exception {
		if (desktop == null) {
			throw new InvalidUrlException("Client browser not supported");
		}

		if (!urlString.contains("https://") && !urlString.contains("http://")) {
			throw new InvalidUrlException();
		}

		URI uri;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			throw new InvalidUrlException(e);
		}

		try {
			desktop.browse(uri);
		} catch (IOException e) {
			throw new InvalidUrlException(e);
		}

	}

}
