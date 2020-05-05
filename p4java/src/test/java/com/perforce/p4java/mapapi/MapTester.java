package com.perforce.p4java.mapapi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.perforce.p4java.mapapi.MapFlag.*;

public class MapTester {

    MapTable mapTable = new MapTable();
    int mapCount = 0;


    private void InsertMap( MapTable t, String l, String r )
    {
        String l1;
        MapFlag flag = MfMap;

        if( l.length() > 1 )
            switch( l.charAt(0) )
            {
                case '-':
                    flag = MfUnmap;
                    break;
                case '+':
                    flag = MfRemap;
                    break;
                case '&':
                    flag = MfAndmap;
                    break;
            }

        if( flag != MfMap )
            l1 = l.substring(1);
        else
            l1 = l;

        t.insert( l1, r, flag );
    }

    public String PrintMap( MapTable t, MapTableT dir, String path, boolean explode )
    {
        StringBuffer ret = new StringBuffer();
        String o;
        MapItem m;
        MapWrap w;

        if( explode )
        {
            MapItemArray v = t.explode( dir, path );
            int i = 0;
            while( ( m = v.getItem( i++ ) ) != null && ( o = v.getTranslation( i-1 ) ) != null ) {
                ret.append(path).append(" -> ").append(o);
                        if(m.flag() == MfAndmap )  {
                            ret.append(" (readonly)"); }
                            ret.append("\n");
            }

            if( i == 1 ) {
                // Print the empty line
                ret.append(path).append(" -> \n");
            }
        }
        else {
            w = t.translate(dir, path);
            ret.append(path).append(" -> ").append(w != null ? w.getTo() : "");
            if (w != null && w.getMap().flag() == MfAndmap) {
                ret.append(" (readonly)");
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    private void ReadMapFile( MapTable t, String in, boolean allowOne ) throws Exception
    {
        t.clear();

        for(String line : in.split("\n"))
        {
            if(line.isEmpty()) {
                continue;
            }

            Matcher match = Pattern.compile("^([^ ]+) ([^ ]+)$").matcher(line);
            if( !match.matches() && !allowOne ) {
                throw new Exception("Bad mapping: " + line);
            } else if (!match.matches()) {
                match = Pattern.compile("^([^ ]+)$").matcher(line);
                if (!match.matches()) {
                    throw new Exception("Bad mapping: " + line);
                }
            }

            int matches = match.groupCount();
            String m1 = match.group(1);
            String m2 = matches > 1 ? match.group(2) : m1;

            t.validate( m1, m2 );

            InsertMap( t, m1, m2 );
        }
    }


    public MapTester(int caseSensitivity) {
        // Set case
        if (caseSensitivity == 0 || caseSensitivity == 1)
            mapTable.setCaseSensitivity(caseSensitivity);
    }

    public String SetMap(String map) throws Exception {
        // Read in mapping file
        ReadMapFile(mapTable, map, false);
        StringBuffer out = new StringBuffer();
        mapTable.dump( out, "raw map", 0 );
        mapCount = 0;
        return out.toString();

        }

    public String Join(MapTableT dir1, String map, MapTableT dir2) throws Exception {

        // Multi-maps!! join them
        MapTable rightTable = new MapTable();
        ReadMapFile(rightTable, map, true);

        StringBuffer out = new StringBuffer();
        rightTable.dump( out, "raw map [" + ++mapCount + "]", 0 );

        mapTable = mapTable.join2(dir1, rightTable, dir2);

        String tmp = "join map [" + mapCount + "] (join " +
                dir1.toString() + ":" +
                dir2.toString() + ")";
        mapTable.dump( out, tmp, 0 );
        return out.toString();
    }

    public String Restrict(MapTableT dir1, String map, MapTableT dir2) throws Exception {

        // Multi-maps!! join them
        MapTable rightTable = new MapTable();
        ReadMapFile(rightTable, map, true);

        StringBuffer out = new StringBuffer();
        rightTable.dump( out, "raw map [" + ++mapCount + "]", 0 );

        mapTable = mapTable.join(dir1, rightTable, dir2);

        String tmp = "join map [" + mapCount + "] (restrict " +
                dir1.toString() + ":" +
                dir2.toString() + ")";
        mapTable.dump( out, tmp, 0 );
        return out.toString();
    }

    public String Build(MapTableT dir, boolean showTree, MapTableT showStrings, String paths, boolean explode) {

        StringBuffer out = new StringBuffer();

        mapTable.disambiguate();
        mapTable.dump(out, "mumbo map", 0);

        if (showTree)
            mapTable.dumpTree(out, dir, dir.toString());


        // Show strings

        if (showStrings != null) {
            mapTable.strings(showStrings).dump(out);
        }

        // apply mapping
        if (paths != null) {
            for (String line : paths.split("\n")) {
                if (line.isEmpty()) {
                    continue;
                }

                out.append(PrintMap(mapTable, dir, line, explode));
            }
        }

        return out.toString();
    }

}
