/**
 * 
 */
package com.perforce.p4java.exception;

// p4ic4idea: use IServerMessage to maintain original errors.
import com.perforce.p4java.server.IServerMessage;

/**
 * Exception thrown by P4Java methods when access to data or services has
 * been denied by the Perforce server. This is usually a login or
 * permissions issue.
 * 
 *
 */
// p4ic4idea: abstract to better use the more specific sub-types.
public abstract class AccessException extends P4JavaException {

	private static final long serialVersionUID = 1L;

	private IServerMessage err;

	public AccessException(final IServerMessage err) {
		super(err.getLocalizedMessage());
		this.err = err;
	}

	public AccessException(final AccessException err) {
		super(err.getServerMessage().getLocalizedMessage(), err);
		this.err = err.getServerMessage();
	}

	// p4ic4idea: for testing purposes
	@Deprecated
	AccessException() {
		super("", new Exception());
	}
	// p4ic4idea: for testing purposes
	public static class AccessExceptionForTests extends AccessException {}

	public IServerMessage getServerMessage() {
		return err;
	}

	public int getGenericCode() {
		if (err != null) {
			return err.getGeneric();
		}
		return -1;
	}

	public int getSubCode() {
		if (err != null) {
			return err.getSubCode();
		}
		return -1;
	}

	/**
	 * @deprecated use the server code instead
	 */
	@Deprecated
	public boolean hasMessageFragment(String fragment) {
		return err == null
				? (getMessage() != null && getMessage().toLowerCase().equals(fragment.toLowerCase()))
				: err.hasMessageFragment(fragment);
	}
}
