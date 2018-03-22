package com.perforce.p4java.impl.mapbased.server.cmd;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.server.CmdSpec.DBSCHEMA;

import java.util.List;
import java.util.Map;

import com.perforce.p4java.admin.IDbSchema;
import com.perforce.p4java.common.function.Function;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.admin.DbSchema;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.delegator.IDBSchemaDelegator;

/**
 * Handles DB Schema commands.
 */
public class DBSchemaDelegator extends BaseDelegator implements IDBSchemaDelegator {

    /**
     * Instantiates a new DB schema delegator.
     *
     * @param server
     *            the server
     */
    public DBSchemaDelegator(final IOptionsServer server) {
        super(server);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.perforce.p4java.server.delegator.IDBSchemaDelegator#getDbSchema(java.
     * util.List)
     */
    @Override
    public List<IDbSchema> getDbSchema(final List<String> tableSpecs) throws P4JavaException {

        String[] args = new String[0];
        if (nonNull(tableSpecs)) {
            args = new String[tableSpecs.size()];
            args = tableSpecs.toArray(args);
        }
        List<Map<String, Object>> resultMaps = execMapCmdList(DBSCHEMA, args, null);
        return ResultListBuilder.buildNonNullObjectListFromCommandResultMaps(
                resultMaps,
                new Function<Map, IDbSchema>() {
                    @Override
                    public IDbSchema apply(Map map) {
                        return new DbSchema(map);
                    }
                });
    }
}
