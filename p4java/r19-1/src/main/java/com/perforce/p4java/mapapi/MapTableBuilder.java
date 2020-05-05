package com.perforce.p4java.mapapi;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientViewMapping;
import com.perforce.p4java.core.IMapEntry;

import java.util.List;

public class MapTableBuilder {

    /**
     * @param client - desired client to return the map table of.
     * @return a MapTable with a particular clients view mappings.
     */
    public static MapTable buildMapTable(IClient client) {
        List<IClientViewMapping> clientMappings = client.getClientView().getEntryList();
        MapTable mt = new MapTable();
        for (IClientViewMapping clientmapping : clientMappings) {
            IMapEntry.EntryType type = clientmapping.getType();
            MapFlag mapFlag = convertType(type);
            mt.insert(clientmapping.getLeft(), clientmapping.getRight(), mapFlag);
        }
        return mt;
    }

    private static MapFlag convertType(IMapEntry.EntryType type) {
        switch (type) {
            case INCLUDE:
                return MapFlag.MfMap;
            case OVERLAY:
                return MapFlag.MfRemap;
            case EXCLUDE:
                return MapFlag.MfUnmap;
            case DITTO:
                return MapFlag.MfAndmap;
            default:
                return MapFlag.MfMap;
        }
    }

}
