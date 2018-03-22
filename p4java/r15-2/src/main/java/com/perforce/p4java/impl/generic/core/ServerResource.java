/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.core.IServerResource;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.UnimplementedError;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

/**
 * Abstract implementation class for the IServerResource interface.<p>
 * 
 * Implementations of interfaces that extend IServerResource should
 * use this as a superclass unless there are good reasons not to.
 */

public abstract class ServerResource implements IServerResource {
	/**
	 * Refreshable flag
	 */
	protected boolean refreshable = false;
	
	/**
	 * Updateable flag
	 */
	protected boolean updateable = false;
	
	/**
	 * Server instance
	 */
	protected IServer server = null;

	/**
	 * Default constructor -- sets complete to true,
	 * completable, refreshable, and updateable to false,
	 * and server to null.
	 */
	protected ServerResource() {
	}

	/**
	 * Sets complete to true, completable, refreshable, and
	 * updateable to false, and server to the passed-in value.
	 */
	protected ServerResource(IServer server) {
		this.server = server;
	}

	/**
	 * Explicit some-value constructor; sets server to null.
	 */
	protected ServerResource(boolean refreshable, boolean updateable) {
		this(refreshable, updateable, null);
	}

	/**
	 * Explicit all-value constructor.
	 */
	protected ServerResource(
			boolean refreshable, boolean updateable, IServer server) {
		this(server);
		this.refreshable = refreshable;
		this.updateable = updateable;
	}


	/**
	 * @see com.perforce.p4java.core.IServerResource#canRefresh()
	 */
	public boolean canRefresh() {
		return this.refreshable && this.server != null;
	}
	
	/**
	 * @see com.perforce.p4java.core.IServerResource#canUpdate()
	 */
	public boolean canUpdate() {
		return this.updateable && this.server != null;
	}

	/**
	 * @see com.perforce.p4java.core.IServerResource#complete()
	 */
	public void complete() throws ConnectionException, RequestException, AccessException {
		throw new UnimplementedError("called default IServerResourceImpl.complete");
	}

	/**
	 * @see com.perforce.p4java.core.IServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException, AccessException {
		throw new UnimplementedError("called default IServerResourceImpl.refresh");
	}
	
	/**
	 * @see com.perforce.p4java.core.IServerResource#update()
	 */
	public void update() throws ConnectionException, RequestException, AccessException {
		throw new UnimplementedError("called default IServerResourceImpl.update");
	}

	/**
	 * @see com.perforce.p4java.core.IServerResource#update(boolean)
	 */
	public void update(boolean force) throws ConnectionException, RequestException, AccessException {
		throw new UnimplementedError("called IServerResourceImpl.update(force)");
	}

	/**
	 * @see com.perforce.p4java.core.IServerResource#update(com.perforce.p4java.option.Options)
	 */
	public void update(Options opts) throws ConnectionException, RequestException, AccessException {
		throw new UnimplementedError("called IServerResourceImpl.update(opts)");
	}

	/**
	 * Set the resource as refreshable
	 * 
	 * @param refreshable
	 */
	public void setRefreshable(boolean refreshable) {
		this.refreshable = refreshable;
	}
	
	/**
	 * @see com.perforce.p4java.core.IServerResource#setServer(com.perforce.p4java.server.IServer)
	 */
	public void setServer(IServer server) {
		this.server = server;
	}
}
