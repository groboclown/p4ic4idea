package com.perforce.p4java.option.server;

import com.perforce.p4java.exception.OptionsException;
import com.perforce.p4java.option.Options;
import com.perforce.p4java.server.IServer;

import java.util.List;

/**
 * This class is used to encapsulate all the information
 * that forms the options part of the 'p4 list' command
 * <p>
 * Usage: list [-l label [-d]] [-C] [-M] files...
 * <p>
 */
public class ListOptions extends Options {

	public static final String OPTIONS_SPECS = "s:l b:d b:C b:M";

	/**
	 * The repo against which the 'p4 list' command is issued
	 */
	private String label;

	/**
	 * Delete flag
	 */
	private boolean delete;

	/**
	 * Limits any depot paths to those that can be mapped through
	 * the client workspace
	 */
	private boolean limitDepotPaths = false;

	/**
	 * Flag that can be used to specify when issuing the list command against a
	 * forwarding replica
	 */
	private boolean listFromReplica = false;

	/**
	 * Default constructor
	 */
	public ListOptions() {

	}

	/**
	 * @param server possibly-null IServer representing the Perforce server the
	 *               options are to be used against. If this parameter is null, it
	 *               is acceptable to throw an OptionsException, but it is also
	 *               possible to ignore it and do the best you can with what you've
	 *               got...
	 * @return list of options strings
	 * @throws OptionsException Options Exception
	 */
	@Override
	public List<String> processOptions(IServer server) throws OptionsException {
		this.optionList = this.processFields(OPTIONS_SPECS, this.label, this.delete,
				this.limitDepotPaths, this.listFromReplica);
		return this.optionList;
	}

	/**
	 * Returns the label attached to the list of files retrived
	 *
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets a label to the list of files to be retrieved
	 *
	 * @param label label name
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Should the content referred to by the label be deleted
	 *
	 * @return delete flag
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * Sets a flag indicating that the files referred to by the label should be deleted or not
	 *
	 * @param delete delete flag
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * Indicates whether depot path should be limited to that can be mapped via client workspace
	 *
	 * @return limitDepotPaths flag
	 */
	public boolean isLimitDepotPaths() {
		return limitDepotPaths;
	}

	/**
	 * Sets a flag indicating whether depot path shou;d be limited to that can be mapped via client workspace
	 *
	 * @param limitDepotPaths limitDepotPaths flag
	 */
	public void setLimitClient(boolean limitDepotPaths) {
		this.limitDepotPaths = limitDepotPaths;
	}

	/**
	 * Returns listFromReplica
	 *
	 * @return listFromReplica flag
	 */
	public boolean isListFromReplica() {
		return listFromReplica;
	}

	/**
	 * Sets listFromReplica
	 *
	 * @param listFromReplica listFromReplica flag
	 */
	public void setListFromReplica(boolean listFromReplica) {
		this.listFromReplica = listFromReplica;
	}
}
