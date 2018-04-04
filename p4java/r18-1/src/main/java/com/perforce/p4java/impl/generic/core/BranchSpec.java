/*
 * Copyright 2009 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IBranchMapping;
import com.perforce.p4java.core.IBranchSpec;
import com.perforce.p4java.core.IBranchSpecSummary;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.mapbased.MapKeys;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServer;

/**
 * Simple default implementation class for the IBranchSpec interface.
 */

public class BranchSpec extends BranchSpecSummary implements IBranchSpec {
	
	protected ViewMap<IBranchMapping> branchView = null;
	
	/**
	 * Default description for use in newBranchSpec method when no explicit
	 * description is given.
	 */
	public static final String DEFAULT_DESCRIPTION = "New branchspec created by P4Java";
	
	/**
	 * Simple factory / convenience method for creating a new local BranchSpec object
	 * with defult values.
	 * 
	 * @param server non-null server to be associated with the new branch spec.
	 * @param name non-null branch spec name.
	 * @param description if not null, used as the new branc spec's description field;
	 * 			if null, uses the BranchSpec.DEFAULT_DESCRIPTION field.
	 * @param branches if not null, use this as the list of branch spec
	 * 			paths, in the order given, and according to the format in
	 * 			MapEntry.parseViewMappingString; unlike many other core object
	 * 			factory methods, this one does not default if null.
	 * @return new local BranchSpec object.
	 */
	public static BranchSpec newBranchSpec(IOptionsServer server, String name, String description,
												String[] branches) {
		if (name == null) {
			throw new NullPointerError("null branch spec name in BranchSpec.newBranchSpec()");
		}
		if (server == null) {
			throw new NullPointerError("null server in BranchSpec.newBranchSpec()");
		}
		if (branches == null) {
			throw new NullPointerError("null branch view in BranchSpec.newBranchSpec()");
		}
		
		ViewMap<IBranchMapping> branchView = new ViewMap<IBranchMapping>();
		
		int i = 0;
		for (String mapping : branches) {
			if (mapping == null) {
				throw new NullPointerError("null mapping string passed to Client.newClient");
			}
			branchView.addEntry(new BranchViewMapping(i, mapping));
			i++;
		}
		
		return new BranchSpec(
					name,
					server.getUserName(),
					description == null ? BranchSpec.DEFAULT_DESCRIPTION : description,
					false,
					null,
					null,
					branchView
				);
	}
	
	/**
	 * Simple default implementation of the IViewMapping interface.
	 */
	
	static public class BranchViewMapping extends MapEntry implements IBranchMapping {
		
		/**
		 * Default constructor -- calls super() only.
		 */
		public BranchViewMapping() {
			super();
		}
		
		/**
		 * Explicit value constructor -- calls super(order, sourceSpec, targetSpec).
		 */
		public BranchViewMapping(int order, String sourceSpec,
				String targetSpec) {
			super(order, sourceSpec, targetSpec);
		}
		
		/**
		 * Construct a mapping from the passed-in string, which is assumed
		 * to be in the format described in MapEntry.parseViewString(String).
		 */
		public BranchViewMapping(int order, String viewString) {
			super(order, viewString);
		}

		/**
		 * @see com.perforce.p4java.core.IBranchMapping#getSourceSpec()
		 */
		public String getSourceSpec() {
			return this.left;
		}
		/**
		 * @see com.perforce.p4java.core.IBranchMapping#setSourceSpec(java.lang.String)
		 */
		public void setSourceSpec(String sourceSpec) {
			this.left = sourceSpec;
		}
		/**
		 * @see com.perforce.p4java.core.IBranchMapping#getTargetSpec()
		 */
		public String getTargetSpec() {
			return this.right;
		}
		/**
		 * @see com.perforce.p4java.core.IBranchMapping#setTargetSpec(java.lang.String)
		 */
		public void setTargetSpec(String targetSpec) {
			this.right = targetSpec;
		}
	}
	
	/**
	 * Default constructor. All fields set to null or false.
	 */
	public BranchSpec() {
		super();
	}
	
	/**
	 * Construct a new BranchSpec from explicit field values.
	 */
	
	public BranchSpec(String name,
			String ownerName, String description, boolean locked,
			Date accessed, Date updated,
			ViewMap<IBranchMapping> branchView) {
		this.accessed = accessed;
		this.updated = updated;
		this.name = name;
		this.ownerName = ownerName;
		this.description = description;
		this.locked = locked;
		this.branchView = branchView;
	}

	/**
	 * Construct a BranchSpec from a map passed back from the Perforce
	 * server in response to a getBranchSpec command. 
	 */
	public BranchSpec(Map<String, Object> map, IServer server) {
		super(map, false);
		
		this.server = server;
		this.branchView = new ViewMap<IBranchMapping>();
		
		if (map != null) {
			String key = MapKeys.VIEW_KEY;
			for (int i = 0; ; i++) {
				if (!map.containsKey(key + i)) {
					break;
				} else if (map.get(key + i) != null) {
					try {
						String[] matchStrs = MapEntry.parseViewMappingString((String) map.get(key + i));
						
						this.branchView.getEntryList().add(new BranchViewMapping(i, matchStrs[0], matchStrs[1]));
						
					} catch (Throwable thr) {
						Log.error("Unexpected exception in BranchSpec map-based constructor: "
										+ thr.getLocalizedMessage());
						Log.exception(thr);
					}
				}
			}
		}
	}
	
	/**
	 * Construct a new BranchSpec from the passed-in summary branch spec. If
	 * the summary is null, this is equivalent to calling the default BranchSpec
	 * constructor; otherwise after name initialization a refresh() is done on the
	 * new (empty) BranchSpec.
	 * 
	 * @throws ConnectionException if the Perforce server is unreachable or is not
	 * 				connected.
	 * @throws RequestException if the Perforce server encounters an error during
	 * 				its processing of the request
	 * @throws AccessException if the Perforce server denies access to the caller
	 */
	
	public BranchSpec(IBranchSpecSummary summary)
					throws ConnectionException, RequestException, AccessException {
		super(false);
		this.branchView = new ViewMap<IBranchMapping>();
		if (summary != null) {
			this.setName(summary.getName());
			
			if (this.getName() != null) {
				this.refresh();
			}
		}
	}
	
	private void updateFlags() {
	}

	/**
	 * This method will refresh by getting the complete branch model. If this
	 * refresh is successful then this branch will be marked as complete.
	 * 
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException,
			AccessException {
		IServer refreshServer = this.server;
		String refreshName = this.name;
		if (refreshServer != null && refreshName != null) {
			IBranchSpec refreshedBranch = refreshServer
					.getBranchSpec(refreshName);
			if (refreshedBranch != null) {
				this.name = refreshedBranch.getName();
				this.accessed = refreshedBranch.getAccessed();
				this.updated = refreshedBranch.getUpdated();
				this.branchView = refreshedBranch.getBranchView();
				this.description = refreshedBranch.getDescription();
				this.ownerName = refreshedBranch.getOwnerName();
				this.locked = refreshedBranch.isLocked();
			}
		}
		updateFlags();
	}
	

	/**
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#update()
	 */
	public void update() throws ConnectionException, RequestException, AccessException {
		this.server.updateBranchSpec(this);
	}

	/**
	 * @see com.perforce.p4java.core.IBranchSpec#getBranchView()
	 */
	public ViewMap<IBranchMapping> getBranchView() {
		return this.branchView;
	}

	/**
	 * @see com.perforce.p4java.core.IBranchSpec#setBranchView(com.perforce.p4java.core.ViewMap)
	 */
	public void setBranchView(ViewMap<IBranchMapping> branchView) {
		this.branchView = branchView;
	}
}
