package com.perforce.p4java.mapapi;

/**
 * MapPair - pair of MapItem entries that are candidates for Join
 */
public class MapPair {

    public MapItem item1;
    public MapItem tree2;
    public MapHalf h1;
    public MapHalf h2;

    public MapPair( MapItem item1, MapItem tree2, MapHalf h1, MapHalf h2 )
    {
        this.item1 = item1;
        this.tree2 = tree2;
        this.h1 = h1;
        this.h2 = h2;
    }

    // For MapPairArray::Sort()

    public int compareSlot(MapPair o )
    {
        int r;

        if( ( r = item1.slot - o.item1.slot ) == 0 )
            r = tree2.slot - o.tree2.slot;

        return -r;
    }
}
