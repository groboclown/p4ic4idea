package com.perforce.p4java.mapapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * MapPairArray - array of MapPairs, candidates for MapHalf::Join
 *
 *	MapPairArray::Match() - match a MapItem against the tree,
 *		adding any potentially matching entries to the
 *		array.
 *
 *	MapPairArray::Sort() - resort entries according to their
 *		map precedence rather than tree order.
 *
 *	MapPairArray::Get() - retrieve an entry suitable for
 *		calling MapHalf::Join().
 */
public class MapPairArray extends ArrayList<MapPair> {

    private MapTableT dir1;
    private MapTableT dir2;

    public MapPairArray( MapTableT dir1, MapTableT dir2 )
    {
        this.dir1 = dir1;
        this.dir2 = dir2;
    }

    public void match(MapItem item1, MapItem tree2 ) {
        // Do non-wildcard initial substrings match?
        MapItem i2 = tree2;
        MapItem.MapWhole t1 = item1.whole( dir1 );

        do {
            MapItem.MapWhole t2 = i2.whole( dir2 );

            int r = t2.half.matchHead( t1.half );

            //if( DEBUG_JOIN )
            //    p4debug.printf("cmp %d %s %s\n", r, t1->half.Text(), t2->half.Text() );

            // On match, add this to list of pairs for MapHalf::Join()

            if( r == 0 && !t2.half.matchTail( t1.half ) )
                add( new MapPair( item1, i2, t1.half, t2.half ) );

            // Recursively explore other matching possibilities.

            if( r <= 0 && t2.left != null )   match( item1, t2.left );
            if( r >= 0 && t2.right != null )  match( item1, t2.right );

            if( r != 0 )
                return;

            // tail iteration down the center

            i2 = t2.center;
        } while( i2 != null );
    }

    public void sort() {
        Collections.sort(this, new Comparator<MapPair>() {
            @Override
            public int compare(MapPair o1, MapPair o2) {
                return o1.compareSlot(o2);
            }
        });
    }

}
