package com.perforce.p4java.mapapi;

import java.util.ArrayList;

public class MapTree {

    public void clear() {
        sort = null;
        tree = null;
        depth = 0;
    }

    public ArrayList<MapItem> sort = null;
    public MapItem tree = null;
    public int depth = 0;
}
