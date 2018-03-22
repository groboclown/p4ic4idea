/**
 * 
 */
package com.perforce.p4java.impl.generic.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.admin.IDbSchema;

/**
 * Simple default implementation class for the IDbSchema interface.
 */
public class DbSchema implements IDbSchema {
	
	private String name = null;
	private int version = NOVERSION;
	private List<Map<String, String>> columnMetadata = null;

	/**
	 * Default constructor.
	 */
	public DbSchema() {
	}

	/**
	 * Construct a DbSchema using explicit field values.
	 */
	public DbSchema(String name, int version,
			List<Map<String, String>> columnMetadata) {
		this.name = name;
		this.version = version;
		this.columnMetadata = columnMetadata;
	}
	
	/**
	 * Construct a DbSchema from a map returned by the Perforce server.<p>
	 * 
	 * Don't use this unless you know the correct format of the maps and you either
	 * got the map directly from the server or you cobbled together something
	 * suitable yourself. No real error- or sanity-checking is done here.
	 */
	public DbSchema(Map<String, Object> map) {
		if (map != null) {
			try {
				this.name = (String) map.get("table");
				this.version = new Integer((String) map.get("version"));
				this.columnMetadata = new ArrayList<Map<String, String>>();
				
				String columnName = null;
				for (int i = 0; (columnName = (String) map.get("name" + i)) != null; i++) {
					Map<String, String> colMap = new HashMap<String, String>();
					colMap.put("name", columnName);
					if (map.containsKey("key" + i)) {
						colMap.put("key", (String) map.get("key" + i));
					}
					if (map.containsKey("fmtkind" + i)) {
						colMap.put("fmtkind", (String) map.get("fmtkind" + i));
					}
					if (map.containsKey("dmtype" + i)) {
						colMap.put("dmtype", (String) map.get("dmtype" + i));
					}
					if (map.containsKey("type" + i)) {
						colMap.put("type", (String) map.get("type" + i));
					}
					this.columnMetadata.add(colMap);
				}
			} catch (Throwable thr) {
				Log.warn("Unexpected exception: " + thr.getLocalizedMessage());
				Log.exception(thr);
			}
		}
	}

	/**
	 * @see com.perforce.p4java.admin.IDbSchema#getColumnMetadata()
	 */
	public List<Map<String, String>> getColumnMetadata() {
		return columnMetadata;
	}

	/**
	 * @see com.perforce.p4java.admin.IDbSchema#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see com.perforce.p4java.admin.IDbSchema#getVersion()
	 */
	public int getVersion() {
		return version;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void setColumnMetadata(List<Map<String, String>> columnMetadata) {
		this.columnMetadata = columnMetadata;
	}
}
