/**
 * Copyright (c) 2008 Perforce Software.  All rights reserved.
 */
package com.perforce.p4java.impl.generic.core;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IJobSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaError;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.server.IServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple generic default implementation class for the IJobSpec interface.
 *
 *
 */

public class JobSpec extends ServerResource implements IJobSpec {

	private String comments = null;
	private List<IJobSpecField> fields = new ArrayList<IJobSpecField>();
	private Map<String, String> presets = new HashMap<String, String>();
	private Map<String, List<String>> values = new HashMap<String, List<String>>();
	
	protected class JobSpecField implements IJobSpecField {

		private int code = 0;
		private String dataType = null;
		private String fieldType = null;
		private int length = 0;
		private String name = null;
		
		protected JobSpecField() {
		}
		
		protected JobSpecField(String fieldStr) {			
			if (fieldStr != null) {
				try {
					String[] fields = fieldStr.split(" ");
					if ((fields == null) || (fields.length != 5)) {
						throw new P4JavaError(
								"Field conversion or format error in JobSpecField constructor");
					}
					
					if (fields[0] != null) {
						this.code = new Integer(fields[0].trim());
					}
					if (fields[1] != null) {
						this.name = fields[1].trim();
					}
					if (fields[2] != null) {
						this.dataType = fields[2].trim();
					}
					if (fields[3] != null) {
						this.length = new Integer(fields[3].trim());
					}
					if (fields[4] != null) {
						this.fieldType = fields[4].trim();
					}
				} catch (Exception exc) {
					Log.exception(exc);
					throw new P4JavaError(
								"Field conversion or format error in JobSpecField constructor: "
								+ exc.getLocalizedMessage());
				}
			}
		}
		
		public int getCode() {
			return this.code;
		}
		public void setCode(int code) {
			this.code = code;
		}
		public String getDataType() {
			return this.dataType;
		}
		public void setDataType(String dataType) {
			this.dataType = dataType;
		}
		public String getFieldType() {
			return this.fieldType;
		}
		public void setFieldType(String fieldType) {
			this.fieldType = fieldType;
		}
		public int getLength() {
			return this.length;
		}
		public void setLength(int length) {
			this.length = length;
		}
		public String getName() {
			return this.name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public JobSpec() {
	}
	
	public JobSpec(Map<String, Object> map, IServer server) {
		super(true, false);
		this.server = server;
		if (map != null) {
			try {
				this.comments = (String) map.get("Comments");
				
				String fieldStr = null;
				for (int i = 0;  (fieldStr = (String) map.get("Fields" + i)) != null ; i++) {
					this.fields.add(new JobSpecField(fieldStr));
				}
				
				String valueStr = null;
				for (int i = 0; (valueStr = (String) map.get("Values" + i)) != null ; i++) {
					String[] values = valueStr.trim().split(" ");
					if ((values != null) && (values.length == 2) && (values[0] != null)) {
						List<String> valList = new ArrayList<String>();
						if (values[1] != null) {
							String[] vStrs = values[1].trim().split("/");
							
							if (vStrs != null) {
								for (String str : vStrs) {
									if (str != null) {
										valList.add(str);
									}
								}
							}
						}
						
						this.values.put(values[0], valList);
					}
				}
				
				String presetStr = null;
				
				for (int i = 0; (presetStr = (String) map.get("Presets" + i)) != null ; i++) {
					int firstSpace = presetStr.indexOf(" ");
					
					if ((firstSpace > 0) && (firstSpace < (presetStr.length() + 1))) {
						String key = presetStr.substring(0, firstSpace);
						String value = presetStr.substring(firstSpace + 1);
						
						if ((value.startsWith("\"")) && (value.endsWith("\""))
								&& (value.length() > 2)) {
							value = value.substring(1, value.length() - 2);
						}
						
						this.presets.put(key, value);
					}
				}
			} catch (Exception exc) {
				Log.error("Unexpected exception: " + exc.getLocalizedMessage());
				Log.exception(exc);
				throw new P4JavaError(
						"Unexpected conversion exception in JobSpec constructor: "
						+ exc.getLocalizedMessage());
			}
		}
	}

	/**
	 * Completing a job spec calls {@link #refresh()}
	 *
	 * @see #refresh()
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#complete()
	 */
	public void complete() throws ConnectionException, RequestException,
			AccessException {
		refresh();
	}

	/**
	 * This method will refresh by getting the complete job spec model. If this
	 * refresh is successful then this job spec will be marked as complete.
	 * 
	 * @see com.perforce.p4java.impl.generic.core.ServerResource#refresh()
	 */
	public void refresh() throws ConnectionException, RequestException,
			AccessException {
		IServer refreshServer = this.server;
		if (refreshServer != null) {
			IJobSpec refreshedJobSpec = refreshServer.getJobSpec();
			if (refreshedJobSpec != null) {
				this.comments = refreshedJobSpec.getComments();
				this.fields = refreshedJobSpec.getFields();
				this.presets = refreshedJobSpec.getPresets();
				this.values = refreshedJobSpec.getValues();
			}
		}
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#getComments()
	 */
	public String getComments() {
		return this.comments;
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#getFields()
	 */
	public List<IJobSpecField> getFields() {
		return this.fields;
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#getPresets()
	 */
	public Map<String, String> getPresets() {
		return this.presets;
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#getValues()
	 */
	public Map<String, List<String>> getValues() {
		return this.values;
	}
	
	/**
	 * @see com.perforce.p4java.core.IJobSpec#getFieldValues(java.lang.String)
	 */
	public List<String> getFieldValues(String fieldName) {
		return this.values.get(fieldName);
	}
	
	/**
	 * @see com.perforce.p4java.core.IJobSpec#getFieldPreset(java.lang.String)
	 */
	public String getFieldPreset(String fieldName) {
		return this.presets.get(fieldName);
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#setComments(java.lang.String)
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#setFields(java.util.List)
	 */
	public void setFields(List<IJobSpecField> fields) {
		this.fields = fields;
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#setPresets(java.util.Map)
	 */
	public void setPresets(Map<String, String> presets) {
		this.presets = presets;
	}

	/**
	 * @see com.perforce.p4java.core.IJobSpec#setValues(java.util.Map)
	 */
	public void setValues(Map<String, List<String>> values) {
		this.values = values;
	}
}
