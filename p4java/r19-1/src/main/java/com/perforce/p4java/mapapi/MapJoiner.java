package com.perforce.p4java.mapapi;

import static com.perforce.p4java.mapapi.MapFlag.*;

public class MapJoiner extends Joiner {
    
    public MapTable m0;

    public MapItem map;
    public MapItem map2;

    protected String newLhs;
    protected String newRhs;
    
    /*
     * mapFlagGrid -- how to combine two mapFlags
     */

    protected static final MapFlag mapFlagGrid[][] = new MapFlag[][] {

        /* Map */	/* Unmap */	/* Remap */	/* Havemap */	/* Changemap */	/* Andmap */
        {/* Map */	    MfMap,		MfUnmap,	MfRemap,	MfHavemap,	MfChangemap,	MfAndmap, },
        {/* Unmap */	MfUnmap,	MfUnmap,	MfUnmap,	MfUnmap,	MfUnmap,	MfUnmap, },
        {/* Remap */	MfRemap,	MfUnmap,	MfRemap,	MfHavemap,	MfChangemap,	MfAndmap,},
        {/* Havemap */	MfHavemap,	MfUnmap,	MfHavemap,	MfHavemap,	MfHavemap,	MfAndmap,},
        {/* Changemap */ MfChangemap,	MfUnmap,	MfChangemap,	MfHavemap,	MfChangemap,	MfAndmap,},
        {/* Andmap */	MfAndmap,	MfUnmap,	MfAndmap,	MfAndmap,	MfChangemap,	MfAndmap,},

    } ;

    public MapJoiner()
    {
        badJoin = false;
        m0 = new MapTable();
    }

    public void insert()
    {
        newLhs = map.lhs().expand( this.data, params );
        newRhs = map.rhs().expand( this.data, params );
        MapFlag mapFlag = mapFlagGrid[ map.flag().code ][ map2.flag().code ];
        m0.insertNoDups( newLhs, newRhs, mapFlag );
    }
}