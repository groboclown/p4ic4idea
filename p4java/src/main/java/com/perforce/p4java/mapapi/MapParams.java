package com.perforce.p4java.mapapi;

public class MapParams {
    public MapParam vector[];

    public MapParams() {
        vector = new MapParam[MapHalf.PARAM_VECTOR_LENGTH];
        for(int i = 0; i < MapHalf.PARAM_VECTOR_LENGTH; i++) {
            vector[i] = new MapParam();
        }
    }
}
