/**
 * Copyright 2012 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.perforce.p4java.Log;
import com.perforce.p4java.impl.generic.sys.ISystemFileCommandsHelper;

/**
 * Abstract helper class for dynamically determine and use symbolic link support
 * in the Java NIO package (JDK 7 or above).<p>
 * 
 * Note that for Windows systems, hard links are available as of Windows 2000,
 * and symbolic links as of Windows Vista. Therefore, for symbolic link support
 * the Windows version needs to be Windows Vista or above.<p>
 * 
 * The creation of symbolic links during the sync operation requires the link
 * path and target path to be valid on the operating platform.<p>
 * 
 * If a file changes its type to a symlink in Perforce, the content (data) of
 * the file will be used as the link target. In this case, most likely the
 * content (string representation) would not be a valid path.<p>
 *  
 * As of this writing, the Perforce server and client treat hard links as normal
 * files/dirs (Perforce cannot tell the difference).
 */
@SuppressWarnings("unchecked")
public abstract class SymbolicLinkHelper implements ISystemFileCommandsHelper {

	public static final String FILE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	
	public static final String FILE_SYSTEMS_CLASS_NAME = "java.nio.file.FileSystems";
	public static final String FILE_SYSTEM_CLASS_NAME = "java.nio.file.FileSystem";
	public static final String PATH_CLASS_NAME = "java.nio.file.Path";
	public static final String FILES_CLASS_NAME = "java.nio.file.Files";
	public static final String FILE_ATTRIBUTE_CLASS_NAME = "java.nio.file.attribute.FileAttribute";
	public static final String LINK_OPTION_CLASS_NAME = "java.nio.file.LinkOption";
	public static final String FILE_TIME_CLASS_NAME = "java.nio.file.attribute.FileTime";
	public static final String COPY_OPTION_CLASS_NAME = "java.nio.file.CopyOption";

	public static final String FILE_SYSTEMS_GET_DEFAULT_METHOD_NAME = "getDefault";
	public static final String FILE_SYSTEM_GET_PATH_METHOD_NAME = "getPath";
	public static final String FILES_IS_SYMBOLIC_LINK_METHOD_NAME = "isSymbolicLink";
	public static final String FILES_EXISTS_METHOD_NAME = "exists";
	public static final String FILES_CREATE_SYMBOLIC_LINK_METHOD_NAME = "createSymbolicLink";
	public static final String FILES_READ_SYMBOLIC_LINK_METHOD_NAME = "readSymbolicLink";
	public static final String FILES_GET_LAST_MODIFIED_TIME_METHOD_NAME = "getLastModifiedTime";
	public static final String FILE_TIME_TO_MILLIS_METHOD_NAME = "toMillis";
	public static final String FILES_MOVE_METHOD_NAME = "move";

	private static Class<?> fileSystemsClass = null;
	private static Class<?> fileSystemClass = null;
	private static Class<?> pathClass = null;
	private static Class<?> filesClass = null;
	private static Class<?> fileAttributeClass = null;
	private static Class<? extends Enum<?>> linkOptionClass = null;
	private static Class<?> fileTimeClass = null;
	private static Class<?> copyOptionClass = null;
	
	private static Method getDefaultMethod = null;
	private static Method getPathMethod = null;
	private static Method isSymbolicLinkMethod = null;
	private static Method existsMethod = null;
	private static Method createSymbolicLink = null;
	private static Method readSymbolicLink = null;
	private static Method getLastModifiedTime = null;
	private static Method toMillis = null;
	private static Method move = null;

	private static Object fileSystem = null;

	private static Object linkOptionsArray = null;

	private static boolean symbolicLinkCapable = false;

	static {
		Log.info("Checking this Java for symbolic link support...");

		try {
			// Find classes
			fileSystemsClass = Class.forName(FILE_SYSTEMS_CLASS_NAME);
			fileSystemClass = Class.forName(FILE_SYSTEM_CLASS_NAME);
			pathClass = Class.forName(PATH_CLASS_NAME);
			filesClass = Class.forName(FILES_CLASS_NAME);
			fileAttributeClass = Class.forName(FILE_ATTRIBUTE_CLASS_NAME);
			linkOptionClass = (Class<? extends Enum<?>>) Class.forName(LINK_OPTION_CLASS_NAME);
			fileTimeClass = Class.forName(FILE_TIME_CLASS_NAME);
			copyOptionClass = Class.forName(COPY_OPTION_CLASS_NAME);

			// Find methods
			getDefaultMethod = fileSystemsClass
					.getMethod(FILE_SYSTEMS_GET_DEFAULT_METHOD_NAME);
			getPathMethod = fileSystemClass.getMethod(
					FILE_SYSTEM_GET_PATH_METHOD_NAME, new Class[] {
							String.class, String[].class });
			isSymbolicLinkMethod = filesClass.getMethod(
					FILES_IS_SYMBOLIC_LINK_METHOD_NAME, pathClass);
			existsMethod = filesClass.getMethod(FILES_EXISTS_METHOD_NAME,
							new Class[] {
									pathClass,
									Array.newInstance(linkOptionClass, 0)
											.getClass() });
			createSymbolicLink = filesClass
					.getMethod(FILES_CREATE_SYMBOLIC_LINK_METHOD_NAME,
							new Class[] {
									pathClass,
									pathClass,
									Array.newInstance(fileAttributeClass, 0)
											.getClass() });
			readSymbolicLink = filesClass
					.getMethod(FILES_READ_SYMBOLIC_LINK_METHOD_NAME, pathClass);
			getLastModifiedTime = filesClass
					.getMethod(FILES_GET_LAST_MODIFIED_TIME_METHOD_NAME,
							new Class[] {
									pathClass,
									Array.newInstance(linkOptionClass, 0)
											.getClass() });
			toMillis = fileTimeClass
					.getMethod(FILE_TIME_TO_MILLIS_METHOD_NAME);

			move = filesClass
					.getMethod(FILES_MOVE_METHOD_NAME,
							new Class[] {
									pathClass,
									pathClass,
									Array.newInstance(copyOptionClass, 0)
											.getClass() });

			// Invoke methods
			fileSystem = getDefaultMethod.invoke(null);

			// Extract the 'NOFOLLOW_LINKS' link option
			if (linkOptionClass.getEnumConstants() != null) {
				linkOptionsArray = Array.newInstance(linkOptionClass, 1);
				for (Object obj : linkOptionClass.getEnumConstants()) {
					if (obj.toString().equals("NOFOLLOW_LINKS")) {
						Array.set(linkOptionsArray, 0, obj);
						break;
					}
				}
			}
			
			// Symbolic link capable?
			if (getDefaultMethod != null && getPathMethod != null
					&& fileSystem != null && isSymbolicLinkMethod != null
					&& createSymbolicLink != null && readSymbolicLink != null) {
					symbolicLinkCapable = true;
				Log.info("It seems this Java supports symbolic links.");
				Log.info("Symbolic link support at the OS level will be determined at runtime...");
			}

		} catch (ClassNotFoundException cnfe) {
			Log.error("Unable to find class: " + cnfe.getLocalizedMessage());
			Log.exception(cnfe);
		} catch (NoSuchMethodException nsme) {
			Log.error("No such method for class: " + nsme.getLocalizedMessage());
			Log.exception(nsme);
		} catch (InvocationTargetException ite) {
			Log.error("Cannot invoke target method: "
					+ ite.getLocalizedMessage());
			Log.exception(ite);
		} catch (Throwable thr) {
			Log.error("Unexpected exception introspecting class: "
					+ thr.getLocalizedMessage());
			Log.exception(thr);
		}
	}

	/**
	 * Checks if is symbolic link capable.
	 * 
	 * @return true, if is symbolic link capable
	 */
	public static boolean isSymbolicLinkCapable() {
		return symbolicLinkCapable;
	}

	/**
	 * Tests whether a file is a symbolic link.
	 * 
	 * @param path
	 *            the path of the symbolic link
	 * @return true if the file is a symbolic link; false if the file does not
	 *         exist, is not a symbolic link, or it cannot be determined if the
	 *         file is a symbolic link or not.
	 */
	public static boolean isSymbolicLink(String path) {
		if (symbolicLinkCapable && path != null) {
			try {
				Object filePath = getPathMethod.invoke(fileSystem,
						new Object[] { path, new String[] {} });
				if (filePath != null) {
					return (Boolean) isSymbolicLinkMethod
							.invoke(null, filePath);
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return false;
	}

	/**
	 * Creates a symbolic link to a target.
	 * 
	 * @param link
	 *            the path of the symbolic link to create
	 * @param target
	 *            the target of the symbolic link
	 * @return path the path to the symbolic link
	 */
	public static String createSymbolicLink(String link, String target) {
		if (symbolicLinkCapable && link != null && target != null) {
			try {
				Object linkPath = getPathMethod.invoke(fileSystem,
						new Object[] { link, new String[] {} });
				Object targetPath = getPathMethod.invoke(fileSystem,
						new Object[] { target, new String[] {} });
				if (linkPath != null && targetPath != null) {
					Object pathObject = createSymbolicLink.invoke(null,
							linkPath, targetPath,
							Array.newInstance(fileAttributeClass, 0));
					if (pathObject != null) {
						return pathObject.toString();
					}
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return null;
	}
	
	/**
	 * Reads the target path of a symbolic link.
	 * 
	 * @param link
	 *            the path to the symbolic link
	 * @return path the target path of the symbolic link
	 */
	public static String readSymbolicLink(String link) {
		if (symbolicLinkCapable && link != null) {
			try {
				Object linkPath = getPathMethod.invoke(fileSystem,
						new Object[] { link, new String[] {} });
				if (linkPath != null) {
					Object pathObject = readSymbolicLink.invoke(null, linkPath);
					if (pathObject != null) {
						return pathObject.toString();
					}
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return null;
	}

	/**
	 * Gets the last modified time for a symbolic link.
	 * 
	 * Note: symbolic links are not followed (NOFOLLOW_LINKS LinkOption)
	 * 
	 * @param link
	 *            the path to the symbolic link
	 * @return last modified time of the symbolic link
	 */
	public static long getLastModifiedTime(String link) {
		if (symbolicLinkCapable && link != null) {
			try {
				Object linkPath = getPathMethod.invoke(fileSystem,
						new Object[] { link, new String[] {} });
				if (linkPath != null) {
					Object fileTimeObject = getLastModifiedTime.invoke(null,
							linkPath, linkOptionsArray);
					if (fileTimeObject != null) {
						return (Long)toMillis.invoke(fileTimeObject, (Object[])null);
					}
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return 0L;
	}
	/**
	 * Tests whether a file is a symbolic link.
	 * 
	 * Note: symbolic links are not followed (NOFOLLOW_LINKS LinkOption).
	 * 
	 * @param path
	 *            the path of the file or symbolic link
	 * @return true if the file or symbolic link exists; false if it does not
	 *         exist, or it cannot be determined.
	 */
	public static boolean exists(String path) {
		if (symbolicLinkCapable && path != null) {
			try {
				Object filePath = getPathMethod.invoke(fileSystem,
						new Object[] { path, new String[] {} });
				if (filePath != null) {
					return (Boolean) existsMethod.invoke(null, filePath,
							linkOptionsArray);
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return false;
	}

	/**
	 * Creates a symbolic link to a target.
	 * 
	 * @param source
	 *            the path of the path to the file to move
	 * @param target
	 *            the path to the target file
	 * @return the path to the target file
	 */
	public static String move(String source, String target) {
		if (symbolicLinkCapable && source != null && target != null) {
			try {
				Object sourcePath = getPathMethod.invoke(fileSystem,
						new Object[] { source, new String[] {} });
				Object targetPath = getPathMethod.invoke(fileSystem,
						new Object[] { target, new String[] {} });
				if (sourcePath != null && targetPath != null) {
					Object pathObject = move.invoke(null,
							sourcePath, targetPath,
							Array.newInstance(copyOptionClass, 0));
					if (pathObject != null) {
						return pathObject.toString();
					}
				}
			} catch (Throwable thr) {
				Log.error("Unexpected exception invoking method: "
						+ thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}

		return null;
	}

}
