/**
 * 
 */
package com.perforce.p4java.exception;

import com.perforce.p4java.server.IServerMessage;

/**
 * Exception thrown by P4Java methods when access to data or services has
 * been denied by the Perforce server. This is usually a login or
 * permissions issue.
 * 
 *
 */

public class AccessException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	private IServerMessage err;

	public AccessException(final IServerMessage err) {
		super(err.getLocalizedMessage());
		this.err = err;
	}

	public IServerMessage getServerMessage() {
		return err;
	}
}
