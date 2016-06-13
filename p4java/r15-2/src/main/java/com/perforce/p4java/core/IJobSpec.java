/*
 * Copyright 2008 Perforce Software Inc., All Rights Reserved.
 */

package com.perforce.p4java.core;

import java.util.List;
import java.util.Map;

/**
 * Metadata definitions for jobs associated with a specific server. See the main
 * Perforce documentation for the (voluminous) commentary on the various semantics
 * and usage models for this information. In summary, every job is associated
 * with a server-wide jobspec which details the various fields, values, etc.
 * associated with the job; this information can be used to dynamically produce
 * forms, display widgets, etc.<p>
 * 
 * Job specs are complete and not refreshable, updateable, or completable. Setter
 * methods below have only local effect.
 */

public interface IJobSpec extends IServerResource {
	
	/**
	 * Interface onto the main jobspec field specifier. Semantics and usage
	 * are described in the main Perforce jobspec documentation and will not
	 * be given here.
	 */
	public interface IJobSpecField {
		int getCode();
		void setCode(int code);
		
		String getName();
		void setName(String name);
		
		String getDataType();
		void setDataType(String dataType);
		
		int getLength();
		void setLength(int length);
		
		String getFieldType();
		void setFieldType(String fieldType);
	};
	
	/**
	 * Return a list of all known Perforce job fields on this jobspec.
	 * @return non-null but possibly-empty list of IJobSpecField objects for all known
	 * 			job fields in this jobspec.
	 */
	List<IJobSpecField> getFields();
	
	/**
	 * Set the list of all known Perforce job fields on this jobspec.
	 * 
	 * @param fields list of job fields
	 */
	void setFields(List<IJobSpecField> fields);
	
	/**
	 * Return a map of the values 'select' fields can have for each relevant jobspec
	 * field.
	 * 
	 * @return non-null but possibly-empty map of select field value lists.
	 */
	Map<String, List<String>> getValues();
	
	/**
	 * Set the map of the values 'select' fields can have for each relevant jobspec
	 * field.
	 * 
	 * @param values value map
	 */
	void setValues(Map<String, List<String>> values);
	
	/**
	 * Return a map of the possible presets for each known jobspec field.
	 * 
	 * @return non-null but possibly-empty map of presets
	 */
	Map<String, String> getPresets();
	
	/**
	 * Set the map of the possible presets for each known jobspec field.
	 * 
	 * @param presets map of presets
	 */
	void setPresets(Map<String, String> presets);
	
	/**
	 * Get the comments associated with this jobspec.
	 * 
	 * @return possibly-null comments string. This may be rather large...
	 */
	String getComments();
	
	/**
	 * Set the comments associated with this jobspec.
	 * 
	 * @param comments jobspec comment string
	 */
	void setComments(String comments);
	
	/**
	 * Get a list of field values for a given field.
	 * 
	 * @param fieldName non-null field name
	 * @return possibly-null list of field values for the field.
	 */
	List<String> getFieldValues(String fieldName);
	
	/**
	 * Get the preset for a given field.
	 * 
	 * @param fieldName non-null field name
	 * @return possibly-null preset for the named field.
	 */
	String getFieldPreset(String fieldName);
}
