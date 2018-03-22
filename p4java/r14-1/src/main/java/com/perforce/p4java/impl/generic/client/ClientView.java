/**
 * 
 */
package com.perforce.p4java.impl.generic.client;

import java.util.List;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.impl.generic.core.MapEntry;

/**
 * Simple default generic implementation class for the IClientView and
 * associated IClientViewMapping interfaces. Relies heavily
 * on being a simple extension of the underlying ViewMap class.
 */

public class ClientView extends ViewMap<IClientViewMapping> {
	
	/**
	 * Simple extension of the basic MapEntry class to provide convenience
	 * methods based on "depot" and "client" rather than "left" and "right".
	 */
	public static class ClientViewMapping extends MapEntry implements IClientViewMapping {
		
		public static final int NO_MAP_ORDER = MapEntry.ORDER_UNKNOWN;
		
		/**
		 * Default constructor. Sets all fields to null or false.
		 */
		public ClientViewMapping() {
			super();
		}
		
		/**
		 * Constructs a new view mapping by calling the superclass's
		 * corresponding constructor.
		 */
		public ClientViewMapping(int order, String mappingString) {
			super(order, mappingString);
		}
		
		/**
		 * Constructs a new view mapping by calling the superclass's
		 * corresponding constructor.
		 */
		public ClientViewMapping(int order, String depotSpec, String client) {
			super(order, depotSpec, client);
		}
		
		/**
		 * Constructs a new view mapping by calling the superclass's
		 * corresponding constructor.
		 */
		public ClientViewMapping(int order, EntryType type, String depotSpec, String clientSpec) {
			super(order, type, depotSpec, clientSpec);
		}
		
		public String getDepotSpec() {
			return this.left;
		}
		public String getDepotSpec(boolean quoteBlanks) {
			return this.getLeft(quoteBlanks);
		}
		public void setDepotSpec(String depotSpec) {
			this.left = depotSpec;
		}
		public String getClient() {
			return this.right;
		}
		public void setClient(String client) {
			this.right = client;
		}
		public String getClient(boolean quoteBlanks) {
			return this.getRight(quoteBlanks);
		}
	}

	private IClient client = null;
	
	/**
	 * Default constructor; simply calls the superclass
	 * default constructor and sets this.client to null.
	 */
	public ClientView() {
		super();
	}
	
	/**
	 * Construct a ClientView from the passed-in client and mapping list.
	 */
	public ClientView(IClient client, List<IClientViewMapping> mapping) {
		super(mapping);
		this.client = client;
	}


	/**
	 * Get the client object (not path) associated with this view, if any.
	 */
	public IClient getClient() {
		return this.client;
	}
	
	/**
	 * Set the client object (not path) associated with this view, if any.
	 */
	public void setClient(IClient client) {
		this.client = client;
	}
}
