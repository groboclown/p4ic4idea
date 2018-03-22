/**
 * 
 */
package com.perforce.p4java.core.file;

/**
 * Integration resolve action records as returned by getExtendedFiles
 * with the equivalent of the fstat -Or option. See the main Perforce
 * documentation for an explanation of the semantics and usage of
 * these fields.
 */

public interface IResolveRecord {
	FileAction getResolveAction();
	void setResolveAction(FileAction action);
	
	String getResolveBaseFile();
	void setResolveBaseFile(String baseFile);
	
	int getResolveBaseRevision();
	void setResolveBaseRevision(int rev);
	
	String getResolveFromFile();
	void setResolveFromFile(String filePath);
	
	int getResolveStartFromRevision();
	void setResolveStartFromRevision(int rev);
	
	int getResolveEndFromRevision();	
	void setResolveEndFromRevision(int rev);

	String getResolveType();
	void setResolveType(String resolveType);
}
