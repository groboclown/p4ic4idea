/**
 * 
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;

/**
 * Default JDK 6 implementation of the ISystemFileCommandsHelper interface. Uses
 * introspection so it can be compiled (if not actually work) on JDK 5 systems.
 * Actual use of this on JDK 5 systems is OK to the extent that things like edit
 * or sync may end up with the wrong permissions on the client, but much else
 * will work just fine.
 */

@SuppressWarnings("unchecked")
public class RpcSystemFileCommandsHelper extends SymbolicLinkHelper implements
		ISystemFileCommandsHelper {

	public static final String IMPL_CLASS_NAME = "java.io.File";

	private static final String CAN_EXECUTE_METHOD_NAME = "canExecute";
	private static final String SET_EXECUTABLE_METHOD_NAME = "setExecutable";
	private static final String SET_WRITABLE_METHOD_NAME = "setWritable";
	private static final String SET_READABLE_METHOD_NAME = "setReadable";

	private static Class<ISystemFileCommandsHelper> implClass = null;
	private static Constructor<ISystemFileCommandsHelper> implClassConstructor = null;
	private static Method canExecuteMethod = null;
	private static Method setWritableMethod = null;
	private static Method setExecutableMethod = null;
	private static Method setReadableMethod = null;
	
	static {
		Log.info("initializing system file command helper class: " + IMPL_CLASS_NAME);

		try {
			implClass = (Class<ISystemFileCommandsHelper>) Class.forName(IMPL_CLASS_NAME);
		} catch (ClassNotFoundException cnfe) {
			Log.error("Unable to find class '" + IMPL_CLASS_NAME
					+ "': " + cnfe.getLocalizedMessage());
			Log.exception(cnfe);
		} catch (Throwable thr) {
			Log.error("Unable to load class '" + IMPL_CLASS_NAME
					+ "': " + thr.getLocalizedMessage());
			Log.exception(thr);
		}
		
		// NOTE: we do the following in two separate moves, as it's actually OK
		// (or at least not an error) if we can't get the executable methods (it
		// just makes things messier for the user down the line...).
		
		// NOTE also that things still work fine without <i>any</i> of the 
		// methods here for non-edit, non-sync (etc.) operations.
		
		if (implClass != null) {
			try {
				implClassConstructor = implClass.getConstructor(String.class);
	
				setWritableMethod = implClass.getDeclaredMethod(
							SET_WRITABLE_METHOD_NAME,
							new Class[] {boolean.class});
			} catch (NoSuchMethodException nsme) {
				Log.error("No such method for helper class: " + nsme.getLocalizedMessage());
				Log.exception(nsme);
			} catch (Throwable thr) {
				Log.error("Unexpected exception introspecting helper class: "
									+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
			
			try {
				canExecuteMethod = implClass.getDeclaredMethod(
						CAN_EXECUTE_METHOD_NAME,
						new Class[] {});
				setExecutableMethod = implClass.getDeclaredMethod(
						SET_EXECUTABLE_METHOD_NAME,
						new Class[] {boolean.class, boolean.class});
			} catch (NoSuchMethodException nsme) {
				Log.warn("No such method for helper class: " + nsme.getLocalizedMessage());
				Log.exception(nsme);
			} catch (Throwable thr) {
				Log.warn("Unexpected exception introspecting helper class: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
			
			try {
				setReadableMethod = implClass.getDeclaredMethod(
							SET_READABLE_METHOD_NAME,
							new Class[] {boolean.class, boolean.class});
			} catch (NoSuchMethodException nsme) {
				Log.error("No such method for helper class: " + nsme.getLocalizedMessage());
				Log.exception(nsme);
			} catch (Throwable thr) {
				Log.error("Unexpected exception introspecting helper class: "
									+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#canExecute(java.lang.String)
	 */
	public boolean canExecute(String fileName) {
		if (canExecuteMethod != null) {
			try {
				Object implObj = implClassConstructor.newInstance(fileName);
				return (Boolean) canExecuteMethod.invoke(implObj, (Object[]) null);
			} catch (InvocationTargetException ite) {
				
			} catch (Throwable thr) {
				Log.error(
						"Unexpected exception in RpcSystemFileCommandsHelper.canExecute: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
		
		return false;
	}

	/**
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#setExecutable(java.lang.String, boolean, boolean)
	 */
	public boolean setExecutable(String fileName, boolean executable, boolean ownerOnly) {
		if (setExecutableMethod != null) {
			try {
				Object implObj = implClassConstructor.newInstance(fileName);
				return (Boolean) setExecutableMethod.invoke(implObj,
									new Object[] {executable, ownerOnly});
			} catch (Throwable thr) {
				Log.error(
						"Unexpected exception in RpcSystemFileCommandsHelper.setExecutable: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
		
		return false;
	}

	/**
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#setWritable(java.lang.String, boolean)
	 */
	public boolean setWritable(String fileName, boolean writable) {
		if (setWritableMethod != null) {
			try {
				Object implObj = implClassConstructor.newInstance(fileName);
				return (Boolean) setWritableMethod.invoke(implObj,
									new Object[] {writable});
			} catch (Throwable thr) {
				Log.error(
						"Unexpected exception in RpcSystemFileCommandsHelper.setWritable: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
		
		return false;
	}

	/**
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#isSymlink(java.lang.String)
	 */
	public boolean isSymlink(String fileName) {
		return isSymbolicLink(fileName);
	}

	/**
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#setReadable(java.lang.String,
	 *      boolean, boolean)
	 */
	public boolean setReadable(String fileName, boolean readable,
			boolean ownerOnly) {
		if (setReadableMethod != null) {
			try {
				Object implObj = implClassConstructor.newInstance(fileName);
				return (Boolean) setReadableMethod.invoke(implObj,
						new Object[] { readable, ownerOnly });
			} catch (Throwable thr) {
				Log
						.error("Unexpected exception in RpcSystemFileCommandsHelper.setReadOnly: "
								+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return false;
	}
	
	/**
	 * @see com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper#setOwnerReadOnly(java.lang.String)
	 */
	public boolean setOwnerReadOnly(String fileName) {
		boolean set = setReadable(fileName, false, false);
		set &= setReadable(fileName, true, true);
		return set;
	}
	
}
