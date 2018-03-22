/**
 * 
 */
package com.perforce.p4java.impl.generic.core;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IStreamSummary;
import com.perforce.p4java.impl.mapbased.MapKeys;

/**
 * Default implementation class for the IStreamSummary interface.
 */
public class StreamSummary extends ServerResource implements IStreamSummary {

	protected String stream = null;
	protected Date accessed = null;
	protected Date updated = null;
	protected String name = null;
	protected String ownerName = null;
	protected String description = null;
	protected String parent = null;
	protected Type type = null;
	protected IOptions options = new Options();
	protected String baseParent = null;
	protected boolean firmerThanParent = false;
	protected boolean changeFlowsToParent = false;
	protected boolean changeFlowsFromParent = false;
	protected boolean unloaded = false;
	
	/**
	 * Simple default generic IOptions implementation class.
	 */
	public static class Options implements IOptions {

		private boolean ownerSubmit = false;
		private boolean locked = false;
		private boolean noToParent = false;
		private boolean noFromParent = false;

		/**
		 * Default constructor; sets all fields to false.
		 */
		public Options() {
		}

		/**
		 * Explicit-value constructor.
		 */
		public Options(boolean ownerSubmit, boolean locked, boolean noToParent,
				boolean noFromParent) {
			this.ownerSubmit = ownerSubmit;
			this.locked = locked;
			this.noToParent = noToParent;
			this.noFromParent = noFromParent;
		}

		/**
		 * Attempts to construct a stream Options object from a typical p4 cmd
		 * options string, e.g.
		 * "allsubmit/ownersubmit, [un]locked, [no]toparent, [no]fromparent". If
		 * optionsString is null, this is equivalent to calling the default
		 * constructor.
		 */
		public Options(String optionsString) {

			if (optionsString != null) {
				String opts[] = optionsString.split(" ");
				for (String str : opts) {
					if (str.equalsIgnoreCase("ownersubmit")) {
						this.ownerSubmit = true;
					} else if (str.equalsIgnoreCase("locked")) {
						this.locked = true;
					} else if (str.equalsIgnoreCase("notoparent")) {
						this.noToParent = true;
					} else if (str.equalsIgnoreCase("nofromparent")) {
						this.noFromParent = true;
					}
				}
			}
		}

		/**
		 * Return a Perforce-standard representation of these options. This
		 * string is in the same format as used by the stream Options(String
		 * optionsString) constructor.
		 */
		public String toString() {
			return (this.ownerSubmit ? "ownersubmit" : "allsubmit")
					+ (this.locked ? " locked" : " unlocked")
					+ (this.noToParent ? " notoparent" : " toparent")
					+ (this.noFromParent ? " nofromparent" : " fromparent");
		}

		public boolean isOwnerSubmit() {
			return ownerSubmit;
		}

		public void setOwnerSubmit(boolean ownerSubmit) {
			this.ownerSubmit = ownerSubmit;
		}

		public boolean isLocked() {
			return locked;
		}

		public void setLocked(boolean locked) {
			this.locked = locked;
		}

		public boolean isNoToParent() {
			return noToParent;
		}

		public void setNoToParent(boolean noToParent) {
			this.noToParent = noToParent;
		}

		public boolean isNoFromParent() {
			return noFromParent;
		}

		public void setNoFromParent(boolean noFromParent) {
			this.noFromParent = noFromParent;
		}
	}

	/**
	 * Default constructor -- sets all fields to null or false.
	 */
	public StreamSummary() {
	}

	/**
	 * Default constructor; same as no-argument default constructor, except that
	 * it sets the ServerResource superclass fields appropriately for summary
	 * only (everything false) or full stream spec (updateable and refreshable).
	 */
	public StreamSummary(boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);
	}

	/**
	 * Explicit-value constructor. If summaryOnly is true, refreshable and
	 * updeateable are set true in the ServerResource superclass, otherwise
	 * they're set false.
	 */
	public StreamSummary(boolean summaryOnly, String stream, Date accessed,
			Date updated, String name, String ownerName, String description,
			String parent, Type type, Options options) {
		super(!summaryOnly, !summaryOnly);
		this.stream = stream;
		this.accessed = accessed;
		this.updated = updated;
		this.name = name;
		this.ownerName = ownerName;
		this.description = description;
		this.parent = parent;
		this.type = type;
		this.options = options;
	}

	/**
	 * Construct a StreamSummary from a map returned by the Perforce server. If
	 * summaryOnly is true, this map was returned by the IOptionsServer
	 * geStreamSummaryList or similar summary-only method; otherwise it's
	 * assumed to be the full stream spec.
	 * <p>
	 * 
	 * If map is null, this is equivalent to calling the default
	 * summaryOnly-argument constructor.
	 */
	public StreamSummary(Map<String, Object> map, boolean summaryOnly) {
		super(!summaryOnly, !summaryOnly);

		if (map != null) {
			try {
				if (summaryOnly) {
					this.description = (String) map.get(MapKeys.DESC_LC_KEY);
					this.accessed = new Date(Long.parseLong((String) map
							.get(MapKeys.ACCESS_KEY)) * 1000);
					this.updated = new Date(Long.parseLong((String) map
							.get(MapKeys.UPDATE_KEY)) * 1000);
				} else {
					final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";
					this.description = (String) map
							.get(MapKeys.DESCRIPTION_KEY);
					if (map.containsKey(MapKeys.UPDATE_KEY)) {
						this.updated = new SimpleDateFormat(DATE_FORMAT)
								.parse((String) map.get(MapKeys.UPDATE_KEY));
					}
					if (map.containsKey(MapKeys.ACCESS_KEY)) {
						this.accessed = new SimpleDateFormat(DATE_FORMAT)
								.parse((String) map.get(MapKeys.ACCESS_KEY));
					}
				}
			} catch (Throwable thr) {
				Log.warn("Unexpected exception in StreamSummary constructor: "
						+ thr.getMessage());
				Log.exception(thr);
			}

			this.stream = (String) map.get(MapKeys.STREAM_KEY);
			this.name = (String) map.get(MapKeys.NAME_KEY);
			this.ownerName = (String) map.get(MapKeys.OWNER_KEY);
			this.parent = (String) map.get(MapKeys.PARENT_KEY);
			if (map.containsKey(MapKeys.TYPE_KEY)) {
				if (map.get(MapKeys.TYPE_KEY) != null) {
					this.type = IStreamSummary.Type.fromString(((String) map
							.get(MapKeys.TYPE_KEY)).toUpperCase());
				}
			}
			this.options = new Options((String) map.get(MapKeys.OPTIONS_KEY));
			this.firmerThanParent = new Boolean(
					(String) map.get("firmerThanParent"));
			this.changeFlowsToParent = new Boolean(
					(String) map.get("changeFlowsToParent"));
			this.changeFlowsFromParent = new Boolean(
					(String) map.get("changeFlowsFromParent"));
			this.baseParent = (String) map.get("baseParent");
			if (map.get("IsUnloaded") != null
					&& ((String) map.get("IsUnloaded")).equals("1")) {
				this.unloaded = true;
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getStream()
	 */
	public String getStream() {
		return stream;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setStream(com.perforce.p4java.impl.generic.core.file.FilePath)
	 */
	public void setStream(String stream) {
		this.stream = stream;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getAccessed()
	 */
	public Date getAccessed() {
		return accessed;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setAccessed(java.util.Date)
	 */
	public void setAccessed(Date accessed) {
		this.accessed = accessed;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getUpdated()
	 */
	public Date getUpdated() {
		return updated;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setUpdated(java.util.Date)
	 */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getOwnerName()
	 */
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setOwnerName(java.lang.String)
	 */
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getDescription()
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getParent()
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setParent(java.lang.String)
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getType()
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setParent(com.perforce.p4java.core.IStreamSummary.Type)
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getOptions()
	 */
	public IOptions getOptions() {
		return options;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setOptions(com.perforce.p4java.core.IStreamSummary.IOptions)
	 */
	public void setOptions(IOptions options) {
		this.options = options;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#isFirmerThanParent()
	 */
	public boolean isFirmerThanParent() {
		return firmerThanParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setFirmerThanParent(boolean)
	 */
	public void setFirmerThanParent(boolean firmerThanParent) {
		this.firmerThanParent = firmerThanParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#isChangeFlowsToParent()
	 */
	public boolean isChangeFlowsToParent() {
		return changeFlowsToParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setChangeFlowsToParent(boolean)
	 */
	public void setChangeFlowsToParent(boolean changeFlowsToParent) {
		this.changeFlowsToParent = changeFlowsToParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#isChangeFlowsFromParent()
	 */
	public boolean isChangeFlowsFromParent() {
		return changeFlowsFromParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setChangeFlowsFromParent(boolean)
	 */
	public void setChangeFlowsFromParent(boolean changeFlowsFromParent) {
		this.changeFlowsFromParent = changeFlowsFromParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#getBaseParent()
	 */
	public String getBaseParent() {
		return baseParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#setBaseParent(java.lang.String)
	 */
	public void setBaseParent(String baseParent) {
		this.baseParent = baseParent;
	}

	/**
	 * @see com.perforce.p4java.core.IStreamSummary#isUnloaded()
	 */
	public boolean isUnloaded() {
		return unloaded;
	}
}
