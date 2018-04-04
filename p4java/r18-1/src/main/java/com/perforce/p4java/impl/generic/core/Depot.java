package com.perforce.p4java.impl.generic.core;

import static com.perforce.p4java.common.base.ObjectUtils.nonNull;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseLong;
import static com.perforce.p4java.common.base.P4ResultMapUtils.parseString;
import static com.perforce.p4java.common.base.StringHelper.firstNonBlank;
import static com.perforce.p4java.impl.mapbased.MapKeys.ADDRESS_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.DATE_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.DEPOT_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.DEPTH_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.DESCRIPTION_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.DESC_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.EXTRA_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.MAP_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.MAP_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.NAME_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.OWNER_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.OWNER_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.SPEC_MAP_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.STREAM_DEPTH;
import static com.perforce.p4java.impl.mapbased.MapKeys.SUFFIX_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.TIME_LC_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.TYPE_KEY;
import static com.perforce.p4java.impl.mapbased.MapKeys.TYPE_LC_KEY;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.upperCase;

import java.util.Date;
import java.util.Map;

import com.perforce.p4java.Log;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IMapEntry;
import com.perforce.p4java.core.ViewMap;
import org.apache.commons.lang3.time.FastDateFormat;
/**
 * Simple default implementation class for the IDepot interface.
 *
 * @version $Id$
 */

public class Depot extends ServerResource implements IDepot {
    private String name = null;
    private String ownerName = null;
    private Date modDate = null;
    private String description = null;
    private DepotType depotType = null;
    private String address = null;
    private String suffix = null;
    /*
     * #1052310, 1009333 (Bug #50432) **
     * Add the ability to root streams at a deeper level in the depot
     * directory hierarchy.  New stream depots contain the 'StreamDepth'
     * field for defining the root level of a stream's path (the number
     * of slashes found within the stream's Stream field) below the
     * depot name. This field accepts a value between one and 10, while
     * it defaults to one.
     */
    private String streamDepth = null;
    private String map = null;
    private ViewMap<IMapEntry> specMap = new ViewMap<>();

    public Depot() {
        super(false, false);
    }

    /**
     * Construct a Perforce depot object from a suitable depotMap passed back
     * from the Perforce server as the result of a depot list command.
     *
     * @param depotMap depotMap passed back from the Perforce server as a result of the depot list or depot -o
     *            commands; if null, fields will have default values.
     */
    public Depot(final Map<String, Object> depotMap) {
        super(false, false);
        if (nonNull(depotMap)) {
            // Note that the interpretation of depotMap values depends to some extent on
            // the type of the depot. Note also the wildly-annoying way the server sends
            // back depotMap keys with different case depending on whether it's the result of
            // an individual depot listing or all depots... hence all the second guessing
            // going on below.

            // The "p4 -ztag depot -o" command returns upper case field names for a single depot
            // The "p4 -ztag depots" command returns lower case fields names for multiple depots (summaries)
            try {
                name = firstNonBlank(parseString(depotMap, NAME_LC_KEY), parseString(depotMap, DEPOT_KEY));
                ownerName = firstNonBlank(parseString(depotMap, OWNER_LC_KEY), parseString(depotMap, OWNER_KEY));

                try {
                    if (nonNull(depotMap.get(TIME_LC_KEY))) {
                        modDate = new Date(parseLong(depotMap, TIME_LC_KEY));
                    } else if (nonNull(depotMap.get(DATE_KEY))) {
                        FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss");
                        modDate =  dateFormat.parse(parseString(depotMap, DATE_KEY));
                    }
                } catch (Throwable thr) {
                    Log.error("Unexpected exception in Depot constructor: %s", thr.getLocalizedMessage());
                    Log.exception(thr);
                }
                description = firstNonBlank(parseString(depotMap, DESC_LC_KEY), parseString(depotMap, DESCRIPTION_KEY));
                if (isNotBlank(description) && (description.length() > 1) && endsWith(description, "\n")) {
                    description = substring(description, 0, description.length() - 1);
                }
                if (nonNull(depotMap.get(TYPE_LC_KEY))) {
                    depotType = DepotType.fromString(upperCase(parseString(depotMap, TYPE_LC_KEY)));
                } else if (nonNull(depotMap.get(TYPE_KEY))) {
                    depotType = DepotType.fromString(upperCase(parseString(depotMap, TYPE_KEY)));
                }
                switch (depotType) {
                    case REMOTE:
                        address = firstNonBlank(parseString(depotMap, EXTRA_LC_KEY), parseString(depotMap, ADDRESS_KEY));
                        break;
                    case SPEC:
                        suffix = firstNonBlank(parseString(depotMap, EXTRA_LC_KEY), parseString(depotMap, SUFFIX_KEY));
                        // Get the spec maps
                        for (int i = 0; ; i++) {
                            if (!depotMap.containsKey(SPEC_MAP_KEY + i)) {
                                break;
                            } else if (nonNull(depotMap.get(SPEC_MAP_KEY + i))) {
                                try {
                                    String path = parseString(depotMap, SPEC_MAP_KEY + i);
                                    specMap.getEntryList().add(new MapEntry(i, path));
                                } catch (Throwable thr) {
                                    Log.error("Unexpected exception in depot spec depotMap-based constructor: %s", thr.getLocalizedMessage());
                                    Log.exception(thr);
                                }
                            }
                        }
                        break;
                    case STREAM:
                        streamDepth = firstNonBlank(parseString(depotMap, DEPTH_LC_KEY), parseString(depotMap, STREAM_DEPTH));
                        break;
                    default:
                        break;
                }
                map = firstNonBlank(parseString(depotMap, MAP_LC_KEY), parseString(depotMap, MAP_KEY));
            } catch (Throwable thr) {
                Log.error("Unexpected exception in Depot constructor: %s", thr.getLocalizedMessage());
                Log.exception(thr);
            }
        }
    }

    public Depot(final String name,
                 final String ownerName,
                 final Date modDate,
                 final String description,
                 final DepotType depotType,
                 final String address,
                 final String suffix,
                 final String map) {

        this(name, ownerName, modDate, description, depotType, address, suffix, null, map);
    }

    public Depot(final String name,
                 final String ownerName,
                 final Date modDate,
                 final String description,
                 final DepotType depotType,
                 final String address,
                 final String suffix,
                 final String streamDepth,
                 final String map) {

        this(name, ownerName, modDate, description, depotType, address, suffix, streamDepth, map, null);
    }

    public Depot(final String name,
                 final String ownerName,
                 final Date modDate,
                 final String description,
                 final DepotType depotType,
                 final String address,
                 final String suffix,
                 final String streamDepth,
                 final String map,
                 final ViewMap<IMapEntry> specMap) {

        this.name = name;
        this.ownerName = ownerName;
        this.modDate = modDate;
        this.description = description;
        this.depotType = depotType;
        this.address = address;
        this.suffix = suffix;
        this.map = map;
        this.specMap = specMap;
        
        // Server versions older than 16.1  do not accept additional fields on depot specs.
        // We should not add stream depth to anything other than a stream depot spec.
        if (depotType == DepotType.STREAM) {
            this.streamDepth = firstNonBlank(streamDepth, "1");
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public DepotType getDepotType() {
        return depotType;
    }

    public void setDepotType(DepotType depotType) {
        this.depotType = depotType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public ViewMap<IMapEntry> getSpecMap() {
        return specMap;
    }

    public void setSpecMap(ViewMap<IMapEntry> specMap) {
        this.specMap = specMap;
    }

    public String getStreamDepth() {
        return streamDepth;
    }

    public void setStreamDepth(String streamDepth) {
        this.streamDepth = streamDepth;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
