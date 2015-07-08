/*
 * Copyright 2011 Perforce Software Inc., All Rights Reserved.
 */
package com.perforce.p4java.impl.generic.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IStreamIntegrationStatus;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.core.IStreamSummary.Type;

/**
 * Default implementation class for the IStreamIntegrationStatus interface.
 */
public class StreamIntegrationStatus implements IStreamIntegrationStatus {

	protected String stream = null;
	protected String parent = null;
	protected IStreamSummary.Type type = null;
	protected IStreamSummary.Type parentType = null;
	protected boolean firmerThanParent = false;
	protected boolean changeFlowsToParent = false;
	protected boolean changeFlowsFromParent = false;
	protected boolean integToParent = false;
	protected String integToParentHow = null;
	protected String toResult = null;
	protected boolean integFromParent = false;
	protected String integFromParentHow = null;
	protected String fromResult = null;

	protected List<ICachedState> cachedStates = new ArrayList<ICachedState>();

	/**
	 * Simple default generic ICachedState implementation class.
	 */
	public static class CachedState implements ICachedState {

		private int change = IChangelist.UNKNOWN;
		private int parentChange = IChangelist.UNKNOWN;
		private int copyParent = IChangelist.UNKNOWN;
		private int mergeParent = IChangelist.UNKNOWN;
		private int mergeHighVal = IChangelist.UNKNOWN;
		private int branchHash = 0;
		private int status = 0;

		/**
		 * Explicit-value all-fields constructor.
		 */
		public CachedState(int change, int parentChange, int copyParent,
				int mergeParent, int mergeHighVal, int branchHash, int status) {

			this.change = change;
			this.parentChange = parentChange;
			this.copyParent = copyParent;
			this.mergeParent = mergeParent;
			this.mergeHighVal = mergeHighVal;
			this.branchHash = branchHash;
			this.status = status;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getChange()
		 */

		public int getChange() {
			return change;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getParentChange()
		 */

		public int getParentChange() {
			return parentChange;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getCopyParent()
		 */

		public int getCopyParent() {
			return copyParent;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getMergeParent()
		 */

		public int getMergeParent() {
			return mergeParent;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getMergeHighVal()
		 */

		public int getMergeHighVal() {
			return mergeHighVal;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getBranchHash()
		 */

		public int getBranchHash() {
			return branchHash;
		}

		/**
		 * @see com.perforce.p4java.core.IStreamIntegrationStatus.ICachedState#getStatus()
		 */

		public int getStatus() {
			return status;
		}
	}

	/**
	 * Explicit-value all-fields constructor.
	 */
	public StreamIntegrationStatus(String stream, String parent,
			IStreamSummary.Type type, IStreamSummary.Type parentType,
			boolean firmerThanParent, boolean changeFlowsToParent,
			boolean changeFlowsFromParent, boolean integToParent,
			String integToParentHow, String toResult, boolean integFromParent,
			String integFromParentHow, String fromResult,
			List<ICachedState> cachedStates) {
		this.stream = stream;
		this.parent = parent;
		this.type = type;
		this.parentType = parentType;
		this.firmerThanParent = firmerThanParent;
		this.changeFlowsToParent = changeFlowsToParent;
		this.changeFlowsFromParent = changeFlowsFromParent;
		this.integToParent = integToParent;
		this.integToParentHow = integToParentHow;
		this.toResult = toResult;
		this.integFromParent = integFromParent;
		this.integFromParentHow = integFromParentHow;
		this.fromResult = fromResult;
		this.cachedStates = cachedStates;
	}

	/**
	 * Constructor for use with maps passed back from the Perforce server only.
	 */
	public StreamIntegrationStatus(Map<String, Object> map) {
		if (map != null) {
			try {
				this.stream = (String) map.get("stream");
				this.parent = (String) map.get("parent");
				this.type = IStreamSummary.Type.fromString(((String) map
						.get("type")).toUpperCase());
				if (map.containsKey("parentType")) {
					this.parentType = IStreamSummary.Type.fromString(((String) map
							.get("parentType")).toUpperCase());
				}
				this.firmerThanParent = new Boolean(
						(String) map.get("firmerThanParent"));
				this.changeFlowsToParent = new Boolean(
						(String) map.get("changeFlowsToParent"));
				this.changeFlowsFromParent = new Boolean(
						(String) map.get("changeFlowsFromParent"));
				this.integToParent = new Boolean(
						(String) map.get("integToParent"));
				this.integToParentHow = (String) map.get("integToParentHow");
				this.toResult = (String) map.get("toResult");
				this.integFromParent = new Boolean(
						(String) map.get("integFromParent"));
				this.integFromParentHow = (String) map
						.get("integFromParentHow");
				this.fromResult = (String) map.get("fromResult");

				if (map.containsKey("change")) {
					this.cachedStates = new ArrayList<ICachedState>();

					this.cachedStates.add(new CachedState(new Integer(
							((String) map.get("change"))
									.equalsIgnoreCase("default") ? "0"
									: (String) map.get("change")),
							new Integer(map.containsKey("parentChange")
									? ((String) map.get("parentChange"))
									.equalsIgnoreCase("default") ? "0"
									: (String) map.get("parentChange")
									: "0"),
							new Integer(((String) map.get("copyParent"))
									.equalsIgnoreCase("default") ? "0"
									: (String) map.get("copyParent")),
							new Integer(((String) map.get("mergeParent"))
									.equalsIgnoreCase("default") ? "0"
									: (String) map.get("mergeParent")),
							new Integer(((String) map.get("mergeHighVal"))
									.equalsIgnoreCase("default") ? "0"
									: (String) map.get("mergeHighVal")),
							new Integer((String) map.get("branchHash")),
							new Integer((String) map.get("status"))));

					// Both directions ('istat -a -s').
					if (map.containsKey("change0")) {
						this.cachedStates.add(new CachedState(new Integer(
								((String) map.get("change0"))
										.equalsIgnoreCase("default") ? "0"
										: (String) map.get("change0")),
								new Integer(map.containsKey("parentChange")
										? (((String) map.get("parentChange0"))
										.equalsIgnoreCase("default") ? "0"
										: (String) map.get("parentChange0"))
										: "0"),
								new Integer(((String) map.get("copyParent0"))
										.equalsIgnoreCase("default") ? "0"
										: (String) map.get("copyParent0")),
								new Integer(((String) map.get("mergeParent0"))
										.equalsIgnoreCase("default") ? "0"
										: (String) map.get("mergeParent0")),
								new Integer(((String) map.get("mergeHighVal0"))
										.equalsIgnoreCase("default") ? "0"
										: (String) map.get("mergeHighVal0")),
								new Integer((String) map.get("branchHash0")),
								new Integer((String) map.get("status0"))));
					}
				}
			} catch (Throwable thr) {
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getStream()
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getParent()
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getType()
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#isFirmerThanParent()
	 */
	public boolean isFirmerThanParent() {
		return firmerThanParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#isChangeFlowsToParent()
	 */

	public boolean isChangeFlowsToParent() {
		return changeFlowsToParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#isChangeFlowsFromParent()
	 */

	public boolean isChangeFlowsFromParent() {
		return changeFlowsFromParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#isIntegToParent()
	 */

	public boolean isIntegToParent() {
		return integToParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getIntegToParentHow()
	 */

	public String getIntegToParentHow() {
		return integToParentHow;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getToResult()
	 */

	public String getToResult() {
		return toResult;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#isIntegFromParent()
	 */

	public boolean isIntegFromParent() {
		return integFromParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getIntegFromParentHow()
	 */

	public String getIntegFromParentHow() {
		return integFromParentHow;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getFromResult()
	 */

	public String getFromResult() {
		return fromResult;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamIntegrationStatus#getCachedStates()
	 */
	public List<ICachedState> getCachedStates() {
		return cachedStates;
	}
}
