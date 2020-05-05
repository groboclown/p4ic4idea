/**
 * 
 */
package com.perforce.p4java.core;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.BranchSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.impl.generic.core.Job;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.impl.generic.core.UserGroup;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.server.IOptionsServer;

/**
 * A lightweight factory class with convenience methods for
 * creating common P4Java objects using the default implementation
 * classes with common default values.<p>
 * 
 * Note that this is really just a useful convenience wrapper
 * for the standard static factory methods on each core implementation
 * class, but the point of this class is that users do not
 * typically have to deal with implementation classes
 * directly at all.<p>
 * 
 * Each method typically includes a boolean createOnServer
 * parameter, which, if true, tries to create the object
 * (Client, Changelist, whatever...) on the server as well.
 * 
 * @since 2011.1
 */
public class CoreFactory {

	/**
	 * Create a new client local object and optionally also create it on the server.<p>
	 *  
	 * Only the fields corresponding to the parameters here can be explicitly set
	 * on creation; all others are given defaults that can be changed
	 * or added later. These defaults are as given for the default Client
	 * and ClientSummary constructors; exceptions include the client's user
	 * name, which is set to server.getUserName (which may cause issues later
	 * down the line if that wasn't set).<p>
	 * 
	 * Note: users of this method are responsible for ensuring that the client you're asking
	 * to be created does not already exist -- if it does exist, this method will succeed,
	 * but the result will be an updated version of the existing client on the server, which
	 * may not be what you wanted.<p>
	 * 
	 * Note: this does <i>not</i> set the server's current client to the returned value;
	 * you have to do that yourself...
	 * 
	 * @param server non-null IOptionsServer to be associated with the client.
	 * @param name non-null client name.
	 * @param description if not null, the client description field to be used; if null,
	 * 			DEFAULT_DESCRIPTION will be used as a default.
	 * @param root if not null, use this as the new client's root; if null, use the server's
	 * 			working directory if its getWorkingDirectory method returns non-null,
	 * 			otherwise use the JVM's current working directory as determine by the
	 * 			user.dir system property.
	 * @param paths if not null, use this as the list of view map depot / client
	 * 			paths, in the order given, and according to the format in
	 * 			MapEntry.parseViewMappingString; defaults to a single entry,
	 * 			"//depot/... //clientname/depot/..." if not given.
	 * @param createOnServer if true, also create the client on the server; the
	 * 			returned client will in this case be the IClient corresponding
	 * 			to the on-server client (if the creation succeeded...).
	 * @throws P4JavaException if anything went wrong during creation on the server.
	 */
	public static IClient createClient(IOptionsServer server, String name, String description,
			String root, String[] paths, boolean createOnServer) throws P4JavaException {
		
		Client client = Client.newClient(server, name, description, root, paths);
		
		if (client == null) {
			throw new NullPointerError("null client object returned from Client.newClient method");
		}
		
		if (!createOnServer) {
			return client;
		}
		
		// server is usable and not null -- otherwise we'd have had a NullPointerError
		// in Client.newClient() ...
		
		@SuppressWarnings("unused") // used for debugging
		String createString = server.createClient(client);
		return server.getClient(name);
	}
	
	/**
	 * Create a new changelist object locally and optionally also create
	 * it on the server using the passed-in client for default values.<p>
	 * 
	 * The changelist's user field will be set to the current user; other
	 * fields and semantics are as given in the Changelist implementation
	 * class static factory methods which are called directly through this
	 * method.<p>
	 * 
	 * @param client non-null client to be associated with the changelist; this
	 * 				client object must contain a valid server field if
	 * 				createOnServer is true.
	 * @param description if not null, the changelist description string; if null,
	 * 				defaults to Changelist.DEFAULT_DESCRIPTION
	 * @param createOnServer if true, also create the client on the server.
	 * @return if createOnServer is false, the local object created; otherwise,
	 * 				will return the results of an IClient.createChangelist using
	 * 				the new local object.
	 * @throws P4JavaException if anything went wrong during object creation
	 * 				on the server.
	 */
	
	public static IChangelist createChangelist(IClient client, String description,
							boolean createOnServer) throws P4JavaException {
		Changelist changelist = Changelist.newChangelist(client, description);
		
		if (!createOnServer) {
			return changelist;
		}
		
		// server is usable and not null -- otherwise we'd have had a NullPointerError
		// in the static method above ...
		
		return client.createChangelist(changelist);
	}
	
	/**
	 * Simple convenience factory method to create a new local or in-server job.
	 * 
	 * @param server non-null server to be associated with the job.
	 * @param map non-null job fields map.
	 * @param createOnServer if true, create the job on the server, otherwise
	 * 			simply return a suitably-created local job object.
	 * @return new job object.
	 * @throws P4JavaException if anything went wrong during object creation
	 * 				on the server.
	 */
	public static IJob createJob(IOptionsServer server, Map<String, Object> map,
												boolean createOnServer) throws P4JavaException {
		if (!createOnServer) {
			return Job.newJob(server, map);
		} else {
			if (server == null) {
				throw new NullPointerError("null server passed to Factory.createJob()");
			}
			return server.createJob(map);
		}		
	}
	
	/**
	 * Create a new ILabel object locally and optionally on the server.<p>
	 * 
	 * Note: users of this method are responsible for ensuring that the label you're asking
	 * to be created does not already exist -- if it does exist, this method will succeed,
	 * but the result will be an updated version of the existing label on the server, which
	 * may not be what you wanted.<p>
	 * 
	 * @param server non-null server to be associated with this label.
	 * @param name non-null label name.
	 * @param description if not null, use this as the label's description field;
	 * 				if null, use Label.DEFAULT_DESCRIPTION.
	 * @param mapping if not null, use the passed-in string array as the map (in order);
	 * 				if null, use the single map defined in Label.DEFAULT_MAPPING.
	 * @param createOnServer if true, create the label on the server, otherwise
	 * 			simply return a suitably-created local label object.
	 * @return new label object
	 * @throws P4JavaException if anything went wrong during object creation
	 * 				on the server.
	 */
	public static ILabel createLabel(IOptionsServer server, String name, String description,
							String[] mapping, boolean createOnServer) throws P4JavaException {
		ILabel label = Label.newLabel(server, name, description, mapping);
		
		if (!createOnServer) {
			return label;
		}
		
		server.createLabel(label);
		
		return server.getLabel(name);
	}
	
	/**
	 * Create a new IUser object locally and optionally on the server.<p>
	 * 
	 * Note: users of this method are responsible for ensuring that the user you're asking
	 * to be created does not already exist -- if it does exist, this method will succeed,
	 * but the result will be an updated version of the existing user on the server, which
	 * may not be what you wanted.<p>
	 * 
	 * Note also that if createOnServer is true, user creation will fail on the server
	 * unless you have the right to create the new user -- this method uses the equivalent
	 * of 'p4 user -f' under the covers.
	 * 
	 * @param server server to be associated with this user; must not be null if
	 * 			createOnServer is true.
	 * @param name non-null user name.
	 * @param email user's email address.
	 * @param fullName user's full name.
	 * @param password user's password (usually ignored).
	 * @param createOnServer if true, create the user on the server, otherwise
	 * 			simply return a suitably-created local user object.
	 * @return new user object; may be null if creation on server didn't work.
	 * @throws P4JavaException if anything went wrong during object creation
	 * 				on the server.
	 */
	public static IUser createUser(IOptionsServer server, String name, String email, String fullName,
								String password, boolean createOnServer) throws P4JavaException {
		IUser user = User.newUser(name, email, fullName, password);
		
		if (!createOnServer) {
			return user;
		}
		
		if (server == null) {
			throw new NullPointerException("null server passed to Factory.createUser()");
		}
		server.createUser(user, true);
		return server.getUser(name);
	}
	
	/**
	 * Create a new user group locally and / or on the server, using "sensible" default
	 * values for non-parameters.<p>
	 * 
	 * Note: users of this method are responsible for ensuring that the user group you're asking
	 * to be created does not already exist -- if it does exist, this method will succeed,
	 * but the result will be an updated version of the existing user grup on the server, which
	 * may not be what you wanted.<p>
	 * 
	 * @param server server to be associated with this user group; must not be null if
	 * 			createOnServer is true.
	 * @param name non-null user group name.
	 * @param users possibly-null list of user group users.
	 * @param createOnServer if true, create the user group on the server, otherwise
	 * 			simply return a suitably-created local user group object.
	 * @return new user group object; may be null if creation on server didn't work.
	 * @throws P4JavaException if anything went wrong during object creation
	 * 				on the server.
	 */
	public static IUserGroup createUserGroup(IOptionsServer server, String name,
								List<String> users, boolean createOnServer) throws P4JavaException {
		IUserGroup group = UserGroup.newUserGroup(name, users);
		
		if (!createOnServer) {
			return group;
		}
		
		if (server == null) {
			throw new NullPointerException("null server passed to Factory.createUserGroup()");
		}
		server.createUserGroup(group, null);
		return server.getUserGroup(name);
	}
	
	/**
	 * Create a new branch spec locally and / or on the server with default values for
	 * non-parameter fields.<p>
	 * 
	 * Note: users of this method are responsible for ensuring that the spec you're asking
	 * to be created does not already exist -- if it does exist, this method will succeed,
	 * but the result will be an updated version of the existing branch spec on the server, which
	 * may not be what you wanted.<p>
	 * 
	 * @param server non-null server to be associated with the new branch spec.
	 * @param name non-null branch spec name.
	 * @param description if not null, used as the new branc spec's description field;
	 * 			if null, uses the BranchSpec.DEFAULT_DESCRIPTION field.
	 * @param branches if not null, use this as the list of branch spec
	 * 			paths, in the order given, and according to the format in
	 * 			MapEntry.parseViewMappingString; unlike many other core object
	 * 			factory methods, this one does not default if null.
	 * @param createOnServer if true, create the branch spec on the server, otherwise
	 * 			simply return a suitably-created local branch spec object.
	 * @return new branch spec object; may be null if server-side creation failed.
	 * @throws P4JavaException if anything went wrong during object creation
	 * 				on the server.
	 */
	public static IBranchSpec newBranchSpec(IOptionsServer server, String name, String description,
							String[] branches, boolean createOnServer) throws P4JavaException {
		IBranchSpec branchSpec = BranchSpec.newBranchSpec(server, name, description, branches);

		if (!createOnServer) {
			return branchSpec;
		}
		
		server.createBranchSpec(branchSpec);
		return server.getBranchSpec(name);
	}
}
