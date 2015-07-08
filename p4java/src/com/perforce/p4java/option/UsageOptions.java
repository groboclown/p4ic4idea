/**
 * 
 */
package com.perforce.p4java.option;

import java.util.Properties;

import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;

/**
 * Global server usage options class.<p>
 * 
 * Intended to implement some of the options described in
 * the main Perforce p4 usage and p4 undoc documentation on
 * a per-IOptionsServer object basis, and also implements some of
 * the broader environment settings (such as the client name used
 * by the P4Java server implementation when no client has been
 * associated with the server).<p>
 * 
 * The UsageOptions object associated with a server is read and used
 * for a small number of usage values (currently programName, programVersion,
 * unsetUserName, and unsetClientName) each time a command is issued to the
 * corresponding Perforce server, so updates to the UsageOptions object
 * and / or any associated Properties object will be reflected
 * at the next command execution except where noted. Note that this means that
 * UsageOption objects shared between multiple servers are sensitive to such
 * changes, and that changes that occur when a server is processing command
 * requests may cause unexpected results.<p>
 * 
 * A UsageOption object is associated with a server instance when
 * the server is issued by the server factory; this can be the default
 * object or one passed-in to the server factory specifically for that
 * server.<p>
 * 
 * Note that the UsageOptions class should be used with some
 * care as the possible side effects of setting some of the
 * usage parameters to the wrong value can lead to unexpected or
 * odd behaviour.
 */

public class UsageOptions {
	
	/**
	 * The name of the system property used to determine the JVM's current
	 * working directory.
	 */
	public static final String WORKING_DIRECTORY_PROPNAME = "user.dir";
	
	/**
	 * Properties object used to get default field values from. Note that
	 * these properties are potentially accessed for each command, so any
	 * changes in the properties will be reflected the next time the options
	 * object is used.
	 */
	protected Properties props = null;
	
	/**
	 * If not null, will be used to identify the P4Java application's
	 * program name to the Perforce server.
	 */
	protected String programName = null;
	
	/**
	 * If not null, will be used to identify the P4Java application's
	 * program version to the Perforce server.
	 */
	protected String programVersion = null;
	
	/**
	 * If not null, this specifies the Perforce server's idea of each command's
	 * working directory for the associated server object. Corresponds to
	 * the p4 -d usage option.<p>
	 * 
	 * This affects all commands on the associated server from this point on,
	 * and the passed-in path should be both absolute and valid, otherwise
	 * strange errors may appear from the server. If workingDirectory is null,
	 * the Java VM's actual current working directory <b>at the time this object
	 * is constructed</b> is used instead (which is almost always a safe option unless
	 * you're using Perforce alt roots).<p>
	 * 
	 * Note: no checking is done at any time for correctness (or otherwise)
	 * of the workingDirectory option.
	 */
	protected String workingDirectory = null;
	
	/**
	 * If not null, specifies the host name used by the server's commands.
	 * Set to null by the default constructor. Corresponds to the p4 -H
	 * usage option. HostName is not live -- that is, unlike many other
	 * UsageOption fields, its value is only read once when the associated
	 * server is created; subsequent changes will not be reflected in the
	 * associated server.
	 */
	protected String hostName = null;
	
	/**
	 * If not null, use this field to tell the server which language to
	 * use in text messages it sends back to the client. Corresponds to
	 * the p4 -L option, with the same limitations. Set to null by
	 * the default constructor.
	 */
	protected String textLanguage = null;
	
	/**
	 * What will be sent to the Perforce server with each command as the user
	 * name if no user name has been explicitly set for servers associated with
	 * this UsageOption.
	 */
	protected String unsetUserName = null;
	
	/**
	 * If set, this will be used as the name of the client when no
	 * client has actually been explicitly set for the associated server(s).
	 */
	protected String unsetClientName = null;
	
	/**
	 * Default working directory from the JVM to fall back to if not working
	 * directory is set on the usage options
	 */
	protected String defaultWorkingDirectory = null;
	

	/**
	 * Default constructor. Sets props field then calls setFieldDefaults
	 * to set appropriate field default values; otherwise does nothing.
	 */
	public UsageOptions(Properties props) {
		if (props == null) {
			this.props = new Properties();
		} else {
			this.props = props;
		}
		setFieldDefaults(getProps());
	}

	/**
	 * Explicit value constructor. After setting any values explicitly,
	 * calls setFieldDefaults() to tidy up any still-null fields that shouldn't
	 * be null.
	 */
	public UsageOptions(Properties props, String programName, String programVersion,
			String workingDirectory, String hostName, String textLanguage,
			String unsetUserName, String noClientName) {
		if (props == null) {
			this.props = new Properties();
		} else {
			this.props = props;
		}
		this.programName = programName;
		this.programVersion = programVersion;
		this.workingDirectory = workingDirectory;
		this.hostName = hostName;
		this.textLanguage = textLanguage;
		this.unsetUserName = unsetUserName;
		this.unsetClientName = noClientName;
		setFieldDefaults(getProps());
	}
	
	/**
	 * Set any non-null default values when the object
	 * is constructed. Basically, this means running down the fields
	 * and if a field is null and it's not a field that should have a null
	 * default value, calling the corresponding getXXXXDefault method.<p>
	 * 
	 * Fields set here: workingDirectory.
	 */
	protected void setFieldDefaults(Properties props) {
		this.defaultWorkingDirectory = getWorkingDirectoryDefault(props);
	}
	
	/**
	 * Get a suitable default value for the programName field.
	 * This version tries to find a suitable value in the passed-in
	 * properties with the key PropertyDefs.PROG_NAME_KEY_SHORTFORM, then
	 * with the key PropertyDefs.PROG_NAME_KEY; if that comes up null,
	 * it uses the value of PropertyDefs.PROG_NAME_DEFAULT.
	 * 
	 * @return non-null default programName value.
	 */
	protected String getProgramNameDefault(Properties props) {
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return props.getProperty(PropertyDefs.PROG_NAME_KEY_SHORTFORM,
				props.getProperty(PropertyDefs.PROG_NAME_KEY,
						PropertyDefs.PROG_NAME_DEFAULT));
	}
	
	/**
	 * Get a suitable default value for the programVersion field.
	 * This version tries to find a suitable value in the passed-in
	 * properties with the key PropertyDefs.PROG_VERSION_KEY_SHORTFORM, then
	 * with the key PropertyDefs.PROG_VERSION_KEY; if that comes up null,
	 * it uses the value of PropertyDefs.PROG_VERSION_DEFAULT.
	 * 
	 * @return non-null default programVersion value.
	 */
	protected String getProgramVersionDefault(Properties props) {
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return props.getProperty(PropertyDefs.PROG_VERSION_KEY_SHORTFORM,
				props.getProperty(PropertyDefs.PROG_VERSION_KEY,
						PropertyDefs.PROG_VERSION_DEFAULT));
	}
	
	/**
	 * Get a suitable default value for the workingDirectory field. This
	 * is taken from the JVM's system properties using the WORKING_DIRECTORY_PROPNAME
	 * system properties key (which is normally user.dir).
	 * 
	 * @return non-null working directory.
	 */
	protected String getWorkingDirectoryDefault(Properties props) {
		String cwd = System.getProperty(WORKING_DIRECTORY_PROPNAME);
		if (cwd == null) {
			// Oh dear. This should never happen...
			throw new P4JavaError(
					"Unable to retrieve current working directory from JVM system properties");
		}
			
		return cwd;
	}
	
	/**
	 * Get a suitable default value for the unsetUserName field. This version
	 * returns the value of the property associated with the PropertyDefs.USER_UNSET_NAME_KEY
	 * if it exists, or PropertyDefs.USER_UNSET_NAME_DEFAULT if not.
	 *
	 * @return non-null default unsetUserName value.
	 */
	protected String getUnsetUserNameDefault(Properties props) {
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return this.props.getProperty(PropertyDefs.USER_UNSET_NAME_KEY,
				PropertyDefs.USER_UNSET_NAME_DEFAULT);
	}
	
	/**
	 * Get a suitable default value for the unsetClientName field. This version
	 * returns the value of the property associated with the PropertyDefs.CLIENT_UNSET_NAME_KEY
	 * if it exists, or PropertyDefs.CLIENT_UNSET_NAME_DEFAULT if not.
	 *
	 * @return non-null default unsetClientName value.
	 */
	protected String getUnsetClientNameDefault(Properties props) {
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return this.props.getProperty(PropertyDefs.CLIENT_UNSET_NAME_KEY,
				PropertyDefs.CLIENT_UNSET_NAME_DEFAULT);
	}

	/**
	 * Return the program name. The current program name is determined by
	 * first seeing if there's been one explicitly set in this options object;
	 * if so, it's returned, otherwise the associated properties are searched
	 * for a value with the key PropertyDefs.PROG_NAME_KEY_SHORTFORM, then
	 * with the key PropertyDefs.PROG_NAME_KEY; if that comes up null,
	 * it returns the value of PropertyDefs.PROG_NAME_DEFAULT.
	 */
	public String getProgramName() {
		if (this.programName != null) {
			return this.programName;
		}
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return props.getProperty(PropertyDefs.PROG_NAME_KEY_SHORTFORM,
				props.getProperty(PropertyDefs.PROG_NAME_KEY,
						PropertyDefs.PROG_NAME_DEFAULT));
	}

	public UsageOptions setProgramName(String programName) {
		this.programName = programName;
		return this;
	}

	/**
	 * Return the program version. The current program version is determined by
	 * first seeing if there's been one explicitly set in this options object;
	 * if so, it's returned, otherwise the associated properties are searched
	 * for a value with the key PropertyDefs.PROG_VERSION_KEY_SHORTFORM, then
	 * with the key PropertyDefs.PROG_VERSION_KEY; if that comes up null,
	 * it returns the value of PropertyDefs.PROG_VERSION_DEFAULT.
	 */
	public String getProgramVersion() {
		if (this.programVersion != null) {
			return this.programVersion;
		}
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return props.getProperty(PropertyDefs.PROG_VERSION_KEY_SHORTFORM,
				props.getProperty(PropertyDefs.PROG_VERSION_KEY,
						PropertyDefs.PROG_VERSION_DEFAULT));
	}

	public UsageOptions setProgramVersion(String programVersion) {
		this.programVersion = programVersion;
		return this;
	}

	/**
	 * Return the current value of the working directory; this can be dynamically
	 * set explicitly using the setter method or implicitly when the object is
	 * constructed using the JVM's working directory as reflected in the
	 * System properties.
	 */
	public String getWorkingDirectory() {
		return workingDirectory != null ? workingDirectory
				: defaultWorkingDirectory;
	}

	public UsageOptions setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
		return this;
	}

	public String getHostName() {
		return hostName;
	}

	/**
	 * Set the host name. Calling this method has no effect at all after
	 * any associated server object is created.
	 */
	public UsageOptions setHostName(String hostName) {
		this.hostName = hostName;
		return this;
	}

	public String getTextLanguage() {
		return textLanguage;
	}

	public UsageOptions setTextLanguage(String textLanguage) {
		this.textLanguage = textLanguage;
		return this;
	}

	public Properties getProps() {
		return props;
	}

	public UsageOptions setProps(Properties props) {
		this.props = props;
		return this;
	}

	/**
	 * Return the unset client name. The current value is determined by
	 * first seeing if there's been one explicitly set in this options object;
	 * if so, it's returned; otherwise the associated properties are searched
	 * for a value with the key PropertyDefs.CLIENT_UNSET_NAME_KEY; if that comes
	 * up null, it returns the value of PropertyDefs.CLIENT_UNSET_NAME_DEFAULT.
	 */
	public String getUnsetClientName() {
		if (this.unsetClientName != null) {
			return this.unsetClientName;
		}
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return props.getProperty(PropertyDefs.CLIENT_UNSET_NAME_KEY,
						PropertyDefs.CLIENT_UNSET_NAME_DEFAULT);
	}

	public UsageOptions setUnsetClientName(String unsetClientName) {
		this.unsetClientName = unsetClientName;
		return this;
	}

	/**
	 * Return the unset user name. The current value is determined by
	 * first seeing if there's been one explicitly set in this options object;
	 * if so, it's returned; otherwise the associated properties are searched
	 * for a value with the key PropertyDefs.USER_UNSET_NAME_KEY; if that comes
	 * up null, it returns the value of PropertyDefs.USER_UNSET_NAME_DEFAULT.
	 */
	public String getUnsetUserName() {
		if (this.unsetUserName != null) {
			return this.unsetUserName;
		}
		if (props == null) {
			throw new NullPointerError("Null properties in UsageOptions");
		}
		return props.getProperty(PropertyDefs.USER_UNSET_NAME_KEY,
						PropertyDefs.USER_UNSET_NAME_DEFAULT);
	}

	public UsageOptions setUnsetUserName(String unsetUserName) {
		this.unsetUserName = unsetUserName;
		return this;
	}
}
