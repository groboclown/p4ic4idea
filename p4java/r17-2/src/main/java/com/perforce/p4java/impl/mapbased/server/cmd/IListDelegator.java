package com.perforce.p4java.impl.mapbased.server.cmd;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.ListData;
import com.perforce.p4java.option.server.ListOptions;

import java.util.List;

public interface IListDelegator {

	ListData getListData(List<IFileSpec> fileSpecs, ListOptions options) throws P4JavaException;

	ListData getListData(List<IFileSpec> fileSpecs, ListOptions options, String clientName) throws P4JavaException;
}
