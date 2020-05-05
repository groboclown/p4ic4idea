package com.perforce.p4java.mapapi;

public class MapWrap {
    private MapItem map;
    private String to;

    public void setMap(MapItem map) {
        this.map = map;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public MapItem getMap() {
        return map;
    }

    public String getTo() {
        return to;
    }

}
