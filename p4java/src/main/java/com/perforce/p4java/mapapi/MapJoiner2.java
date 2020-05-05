package com.perforce.p4java.mapapi;

public class MapJoiner2 extends MapJoiner {

    MapTableT dir1;
    MapTableT dir2;

    public MapJoiner2( MapTableT dir1, MapTableT dir2 )
    {
        this.dir1 = dir1;
        this.dir2 = dir2;
    }

    public void insert()
    {
        newLhs = map.ohs( dir1 ).expand( this.data, params );
        newRhs = map2.ohs( dir2 ).expand( this.data, params2 );
        MapFlag mapFlag = mapFlagGrid[ map.flag().code ][ map2.flag().code ];
        m0.insertNoDups( newLhs, newRhs, mapFlag );
    }
}
