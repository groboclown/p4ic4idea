package com.perforce.p4java.mapapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/*
 * MapItemArray - array of MapItems, means for returning multiple results
 *                from Match()
 *
 *	MapPairArray::Get() - retrieve an item to the array ordered by slot.
 *
 *	MapPairArray::Put() - adds an item to the array ordered by slot.
 */
public class MapItemArray extends ArrayList<MapWrap> {

    public MapItem getItem(int i ) {
        if(i >= size()) {
            return null;
        }

        MapWrap w = super.get( i );
        return w == null ? null : w.getMap();
    }

    public String getTranslation(int i ) {
        if(i >= size()) {
            return null;
        }

        MapWrap w = super.get( i );
        return w == null ? null : w.getTo();
    }

    public MapItem put(MapItem i, String t ) {
        if( i == null ) {
            return null;
        }

        MapWrap w = new MapWrap();
        w.setMap(i);
        w.setTo(t);

        super.add( w );

        Collections.sort(this, new Comparator<MapWrap>() {
            @Override
            public int compare(MapWrap o1, MapWrap o2) {
                return o1.getMap().slot - o2.getMap().slot;
            }
        });

        return i;
    }

    public int putTree(MapItem item, MapTableT dir ) {
        if( item == null )
            return 0;

        put( item, null );
        int count = 1;
        count += putTree( item.whole( dir ).left, dir );
        count += putTree( item.whole( dir ).center, dir );
        count += putTree( item.whole( dir ).right, dir );

        return count;
    }

    public void dump(String name ) {
        for( int i = 0; i < size(); i++ ) {
            System.out.printf("%s %c%s <-> %s (slot %d)\n",
                    name,
                    " -+$@&    123456789".charAt(getItem(i).mapFlag.code),
                    getItem(i).lhs(),
                    getItem(i).rhs(),
                    getItem(i).slot);
        }
    }
}
