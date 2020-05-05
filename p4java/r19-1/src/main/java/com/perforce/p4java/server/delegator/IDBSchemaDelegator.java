package com.perforce.p4java.server.delegator;

import java.util.List;

import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.exception.P4JavaException;

/**
 * Interface for DBSchemaDelegator implementations.
 */
public interface IDBSchemaDelegator {

    /**
     * Gets the db schema.
     *
     * @param tableSpecs
     *            the table specs
     * @return the db schema
     * @throws P4JavaException
     *             the p4 java exception
     */
    List<IDbSchema> getDbSchema(List<String> tableSpecs) throws P4JavaException;
}
