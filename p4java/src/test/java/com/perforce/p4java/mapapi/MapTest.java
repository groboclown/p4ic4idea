package com.perforce.p4java.mapapi;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static com.perforce.p4java.mapapi.MapTableT.LHS;
import static com.perforce.p4java.mapapi.MapTableT.RHS;

public class MapTest {

    @Test
    public void RestrictTest() throws Exception {
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  //depot/... -> //client/...\n",
                mt.SetMap("//depot/... //client/..."));
        Assert.assertEquals(
                "map raw map [1]: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //depot/bar/...\n" +
                        "\t  //depot/foo/... -> //depot/foo/...\n" +
                        "map join map [1] (restrict LHS:LHS): 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //client/bar/...\n" +
                        "\t  //depot/foo/... -> //client/foo/...\n",
                mt.Restrict(LHS,"//depot/foo/...\n//depot/bar/...", LHS));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //client/bar/...\n" +
                        "\t  //depot/foo/... -> //client/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //depot/bar/... <-> //client/bar/... (maxslot 1 (1))\n" +
                        "LHS  //depot/foo/... <-> //client/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/bar/ (true)\n" +
                        "\t-> 1: //depot/foo/ (true)\n" +
                        "//depot/foo/a -> //client/foo/a\n" +
                        "//depot/bar/b -> //client/bar/b\n" +
                        "//depot/baz/c -> \n",
                mt.Build(LHS, true,LHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", false));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //client/bar/...\n" +
                        "\t  //depot/foo/... -> //client/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //depot/bar/... <-> //client/bar/... (maxslot 1 (1))\n" +
                        "LHS  //depot/foo/... <-> //client/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/bar/ (true)\n" +
                        "\t-> 1: //depot/foo/ (true)\n" +
                        "//depot/foo/a -> //client/foo/a\n" +
                        "//depot/bar/b -> //client/bar/b\n" +
                        "//depot/baz/c -> \n",
                mt.Build(LHS, true,LHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", true));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //client/bar/...\n" +
                        "\t  //depot/foo/... -> //client/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //client/bar/... <-> //depot/bar/... (maxslot 1 (1))\n" +
                        "RHS  //client/foo/... <-> //depot/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //client/bar/ (true)\n" +
                        "\t-> 1: //client/foo/ (true)\n" +
                        "//depot/foo/a -> \n" +
                        "//depot/bar/b -> \n" +
                        "//depot/baz/c -> \n",
                mt.Build(RHS, true,RHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", false));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //client/bar/...\n" +
                        "\t  //depot/foo/... -> //client/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //client/bar/... <-> //depot/bar/... (maxslot 1 (1))\n" +
                        "RHS  //client/foo/... <-> //depot/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //client/bar/ (true)\n" +
                        "\t-> 1: //client/foo/ (true)\n" +
                        "//depot/foo/a -> \n" +
                        "//depot/bar/b -> \n" +
                        "//depot/baz/c -> \n",
                mt.Build(RHS, true,RHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", true));
    }

    @Test
    public void JoinTest() throws Exception {
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  //depot/... -> //client/...\n",
                mt.SetMap("//depot/... //client/..."));
        Assert.assertEquals(
                "map raw map [1]: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/bar/... -> //depot/bar/...\n" +
                        "\t  //depot/foo/... -> //depot/foo/...\n" +
                        "map join map [1] (join LHS:LHS): 2 items, joinError false, emptyReason \n" +
                        "\t  //client/bar/... -> //depot/bar/...\n" +
                        "\t  //client/foo/... -> //depot/foo/...\n",
                mt.Join(LHS,"//depot/foo/...\n//depot/bar/...", LHS));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //client/bar/... -> //depot/bar/...\n" +
                        "\t  //client/foo/... -> //depot/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //client/bar/... <-> //depot/bar/... (maxslot 1 (1))\n" +
                        "LHS  //client/foo/... <-> //depot/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //client/bar/ (true)\n" +
                        "\t-> 1: //client/foo/ (true)\n" +
                        "//depot/foo/a -> \n" +
                        "//depot/bar/b -> \n" +
                        "//depot/baz/c -> \n",
                mt.Build(LHS, true,LHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", false));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //client/bar/... -> //depot/bar/...\n" +
                        "\t  //client/foo/... -> //depot/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //client/bar/... <-> //depot/bar/... (maxslot 1 (1))\n" +
                        "LHS  //client/foo/... <-> //depot/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //client/bar/ (true)\n" +
                        "\t-> 1: //client/foo/ (true)\n" +
                        "//depot/foo/a -> \n" +
                        "//depot/bar/b -> \n" +
                        "//depot/baz/c -> \n",
                mt.Build(LHS, true,LHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", true));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //client/bar/... -> //depot/bar/...\n" +
                        "\t  //client/foo/... -> //depot/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //depot/bar/... <-> //client/bar/... (maxslot 1 (1))\n" +
                        "RHS  //depot/foo/... <-> //client/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/bar/ (true)\n" +
                        "\t-> 1: //depot/foo/ (true)\n" +
                        "//depot/foo/a -> //client/foo/a\n" +
                        "//depot/bar/b -> //client/bar/b\n" +
                        "//depot/baz/c -> \n",
                mt.Build(RHS, true,RHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", false));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  //client/bar/... -> //depot/bar/...\n" +
                        "\t  //client/foo/... -> //depot/foo/...\n" +
                        "MapTree\n" +
                        "\t<<<  //depot/bar/... <-> //client/bar/... (maxslot 1 (1))\n" +
                        "RHS  //depot/foo/... <-> //client/foo/... (maxslot 1 (1))\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/bar/ (true)\n" +
                        "\t-> 1: //depot/foo/ (true)\n" +
                        "//depot/foo/a -> //client/foo/a\n" +
                        "//depot/bar/b -> //client/bar/b\n" +
                        "//depot/baz/c -> \n",
                mt.Build(RHS, true,RHS,"//depot/foo/a\n//depot/bar/b\n//depot/baz/c", true));
    }

    static String maptest_mf = "//depot/a/... //client/a/...\n" +
            "//depot/%%1/%%2 //client/sub/%%2/%%1\n" +
            "//depot/deep/.../* //client/.../deep/*";
    static String maptest_rf = "//depot/a/test\n" +
            "//.../a/*";
    static String maptest_data = "//depot/a/b\n" +
            "//depot/a/b/c/d\n" +
            "//depot/f/j\n" +
            "//depot/numberall\n" +
            "//depot/deep/here/there/everywhere\n" +
            "//depot/choosy/little/beggars";
    static String maptest_3_data = "//client/a/b\n" +
            "//client/a/b/c/d\n" +
            "//client/sub/j/f\n" +
            "//client/numberall\n" +
            "//client/here/there/deep/everywhere\n" +
            "//client/deep/here/there/everywhere\n" +
            "//client/choosy/little/beggars\n" +
            "//client/a/test\n" +
            "//client/numberall\n" +
            "//client/a/numberall\n" +
            "//client/b/numberall\n" +
            "//client/c/numberall\n" +
            "//client/deep/numberall";

    static String maptest_0_mf = "//... //...";
    static String maptest_2_0_mf = "//depot/a/... //client/...\n" +
            "//depot/deep/... //client/...";
    static String maptest_2_1_mf = "//depot/a/... //client/...\n" +
            "+//depot/deep/... //client/...";
    static String maptest_3_mf = "//depot/a/... //client/a/...\n" +
            "//depot/deep/... //client/deep/...\n" +
            "//depot/numberall //client/deep/numberall\n" +
            "&//depot/numberall //client/a/numberall\n" +
            "&//depot/numberall //client/b/numberall";
    static String maptest_r_data = "//client/sub/b/a\n" +
            "//client/a/b/c/d\n" +
            "//client/sub/j/f\n" +
            "//client/f/j\n" +
            "//client/here/there/deep/everywhere\n" +
            "//client/there/here/deep/everywhere";

    static String maptest_4_0_mf = "//SentrySuite/...";
    static String maptest_4_1_mf = "//sentrysuite/... //sentrysuite/...";
    @Test
    public void Basic1Test() throws Exception {
        //tm `maptest -M maptest.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t- //depot/%%1/deep -> //client/sub/deep/%%1\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t- //depot/a/deep/%%1 -> //client/a/deep/%%1\n" +
                        "\t- //depot/a/.../deep/%%1 -> //client/a/.../deep/%%1\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/ (true)\n" +
                        "//depot/a/b -> //client/sub/b/a\n" +
                        "//depot/a/b/c/d -> //client/a/b/c/d\n" +
                        "//depot/f/j -> //client/sub/j/f\n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> //client/here/there/deep/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void Basic2Test() throws Exception {
        //tm `maptest -M maptest.mf -s -b maptest.3.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t- //depot/%%1/deep -> //client/sub/deep/%%1\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t- //depot/a/deep/%%1 -> //client/a/deep/%%1\n" +
                        "\t- //depot/a/.../deep/%%1 -> //client/a/.../deep/%%1\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/ (true)\n" +
                        "//client/a/b -> \n" +
                        "//client/a/b/c/d -> //depot/a/b/c/d\n" +
                        "//client/sub/j/f -> //depot/f/j\n" +
                        "//client/numberall -> \n" +
                        "//client/here/there/deep/everywhere -> //depot/deep/here/there/everywhere\n" +
                        "//client/deep/here/there/everywhere -> \n" +
                        "//client/choosy/little/beggars -> \n" +
                        "//client/a/test -> \n" +
                        "//client/numberall -> \n" +
                        "//client/a/numberall -> \n" +
                        "//client/b/numberall -> \n" +
                        "//client/c/numberall -> \n" +
                        "//client/deep/numberall -> \n",
                mt.Build(RHS, false, LHS, maptest_3_data, false));
    }

    @Test
    public void Basic3Test() throws Exception {
        //tm `maptest -M maptest.mf -R maptest.rf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 2 items, joinError false, emptyReason \n" +
                        "\t  //.../a/* -> //.../a/*\n" +
                        "\t  //depot/a/test -> //depot/a/test\n" +
                        "map join map [1] (restrict LHS:LHS): 6 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/a/%%1 -> //client/a/deep/%%1\n" +
                        "\t  //depot/deep/.../a/%%1 -> //client/.../a/deep/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/sub/%%1/a\n" +
                        "\t  //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/a/%%1 -> //client/a/a/%%1\n" +
                        "\t  //depot/a/.../a/%%1 -> //client/a/.../a/%%1\n",
                mt.Restrict(LHS, maptest_rf, LHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/a/%%1 -> //client/a/deep/%%1\n" +
                        "\t  //depot/deep/.../a/%%1 -> //client/.../a/deep/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/sub/%%1/a\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/a/%%1 -> //client/a/a/%%1\n" +
                        "\t  //depot/a/.../a/%%1 -> //client/a/.../a/%%1\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "//depot/a/b -> //client/sub/b/a\n" +
                        "//depot/a/b/c/d -> \n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> \n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void Basic4Test() throws Exception {
        //tm `maptest -M maptest.mf -r ...`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  ... -> ...\n" +
                        "map join map [1] (restrict LHS:LHS): 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../%%1 -> //client/.../deep/%%1\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.Restrict(LHS,"...",LHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../%%1 -> //client/.../deep/%%1\n" +
                        "\t- //depot/%%1/deep -> //client/sub/deep/%%1\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t- //depot/a/deep/%%1 -> //client/a/deep/%%1\n" +
                        "\t- //depot/a/.../deep/%%1 -> //client/a/.../deep/%%1\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.Build(LHS, false, null, null, false));
    }

    @Test
    public void Basic5Test() throws Exception {
        //tm `maptest -m "//depot/**" -r ...`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  //depot/** -> //depot/**\n",
                mt.SetMap("//depot/** //depot/**"));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  ... -> ...\n" +
                        "map join map [1] (restrict LHS:LHS): 1 items, joinError false, emptyReason \n" +
                        "\t  //depot/%%1 -> //depot/%%1\n",
                mt.Restrict(LHS,"...",LHS));
        Assert.assertEquals(
                "map mumbo map: 1 items, joinError false, emptyReason \n" +
                        "\t  //depot/%%1 -> //depot/%%1\n",
                mt.Build(LHS, false, null, null, false));
    }

    @Test
    public void Basic6Test() throws Exception {
        //tm `maptest -m "a-*" -r "*-b"`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  a-* -> a-*\n",
                mt.SetMap("a-* a-*"));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  *-b -> *-b\n" +
                        "map join map [1] (restrict LHS:LHS): 2 items, joinError false, emptyReason \n" +
                        "\t  a-b -> a-b\n" +
                        "\t  a-%%1-b -> a-%%1-b\n",
                mt.Restrict(LHS,"*-b",LHS));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  a-b -> a-b\n" +
                        "\t  a-%%1-b -> a-%%1-b\n",
                mt.Build(LHS, false, null, null, false));
    }

    @Test
    public void Basic7Test() throws Exception {
        //tm `maptest -m "a-*" -r "**-b"`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  a-* -> a-*\n",
                mt.SetMap("a-* a-*"));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  **-b -> **-b\n" +
                        "map join map [1] (restrict LHS:LHS): 2 items, joinError false, emptyReason \n" +
                        "\t  a-b -> a-b\n" +
                        "\t  a-%%1-b -> a-%%1-b\n",
                mt.Restrict(LHS,"**-b",LHS));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t  a-b -> a-b\n" +
                        "\t  a-%%1-b -> a-%%1-b\n",
                mt.Build(LHS, false, null, null, false));
    }

    @Test
    public void Basic8Test() throws Exception {
        //tm `maptest -m "foo*" -r "foo"`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  foo* -> foo*\n",
                mt.SetMap("foo* foo*"));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  foo -> foo\n" +
                        "map join map [1] (restrict LHS:LHS): 1 items, joinError false, emptyReason \n" +
                        "\t  foo -> foo\n",
                mt.Restrict(LHS,"foo",LHS));
        Assert.assertEquals(
                "map mumbo map: 1 items, joinError false, emptyReason \n" +
                        "\t  foo -> foo\n",
                mt.Build(LHS, false, null, null, false));
    }

    @Test
    public void Remap1Test() throws Exception {
        //Test the MfRemap(overlay mappings)
        //Without overlay
        //tm `maptest -M maptest.2-0.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 2 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/... -> //client/...\n" +
                        "\t  //depot/a/... -> //client/...\n",
                mt.SetMap(maptest_2_0_mf));
        Assert.assertEquals(
                "map mumbo map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/... -> //client/...\n" +
                        "\t- //depot/a/... -> //client/...\n" +
                        "\t  //depot/a/... -> //client/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "//depot/a/b -> \n" +
                        "//depot/a/b/c/d -> \n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> //client/here/there/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void Remap2Test() throws Exception {
        //With overlay
        //tm `maptest -M maptest.2-1.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 2 items, joinError false, emptyReason \n" +
                        "\t+ //depot/deep/... -> //client/...\n" +
                        "\t  //depot/a/... -> //client/...\n",
                mt.SetMap(maptest_2_1_mf));
        Assert.assertEquals(
                "map mumbo map: 2 items, joinError false, emptyReason \n" +
                        "\t+ //depot/deep/... -> //client/...\n" +
                        "\t  //depot/a/... -> //client/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "//depot/a/b -> //client/b\n" +
                        "//depot/a/b/c/d -> //client/b/c/d\n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> //client/here/there/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void MoreJoins1Test() throws Exception {
        //Lets try some joins
        //tm `maptest -M maptest.mf -M JRR#maptest.0.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  //... -> //...\n" +
                        "map join map [1] (join RHS:RHS): 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../%%1 -> //client/.../deep/%%1\n" +
                        "\t  //depot/%%2/%%1 -> //client/sub/%%1/%%2\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.Join(RHS, maptest_0_mf, RHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../%%1 -> //client/.../deep/%%1\n" +
                        "\t- //depot/%%1/deep -> //client/sub/deep/%%1\n" +
                        "\t  //depot/%%2/%%1 -> //client/sub/%%1/%%2\n" +
                        "\t- //depot/a/deep/%%1 -> //client/a/deep/%%1\n" +
                        "\t- //depot/a/.../deep/%%1 -> //client/a/.../deep/%%1\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/ (true)\n" +
                        "//depot/a/b -> //client/sub/b/a\n" +
                        "//depot/a/b/c/d -> //client/a/b/c/d\n" +
                        "//depot/f/j -> //client/sub/j/f\n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> //client/here/there/deep/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void MoreJoins2Test() throws Exception {
        //Use a simple //... //... map to flip everything
        //tm `maptest -M maptest.mf -M JLR#maptest.0.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  //... -> //...\n" +
                        "map join map [1] (join LHS:RHS): 3 items, joinError false, emptyReason \n" +
                        "\t  //client/.../deep/%%1 -> //depot/deep/.../%%1\n" +
                        "\t  //client/sub/%%2/%%1 -> //depot/%%1/%%2\n" +
                        "\t  //client/a/... -> //depot/a/...\n",
                mt.Join(LHS, maptest_0_mf, RHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //client/.../deep/%%1 -> //depot/deep/.../%%1\n" +
                        "\t- //client/sub/deep/%%1 -> //depot/%%1/deep\n" +
                        "\t  //client/sub/%%2/%%1 -> //depot/%%1/%%2\n" +
                        "\t- //client/a/deep/%%1 -> //depot/a/deep/%%1\n" +
                        "\t- //client/a/.../deep/%%1 -> //depot/a/.../deep/%%1\n" +
                        "\t- //client/a/%%1 -> //depot/a/%%1\n" +
                        "\t  //client/a/... -> //depot/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //client/ (true)\n" +
                        "//depot/a/b -> \n" +
                        "//depot/a/b/c/d -> \n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> \n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));

    }

    @Test
    public void MoreJoins3Test() throws Exception {
        //tm `maptest -M maptest.mf -M JLR#maptest.0.mf -s maptest.r.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  //... -> //...\n" +
                        "map join map [1] (join LHS:RHS): 3 items, joinError false, emptyReason \n" +
                        "\t  //client/.../deep/%%1 -> //depot/deep/.../%%1\n" +
                        "\t  //client/sub/%%2/%%1 -> //depot/%%1/%%2\n" +
                        "\t  //client/a/... -> //depot/a/...\n",
                mt.Join(LHS, maptest_0_mf, RHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //client/.../deep/%%1 -> //depot/deep/.../%%1\n" +
                        "\t- //client/sub/deep/%%1 -> //depot/%%1/deep\n" +
                        "\t  //client/sub/%%2/%%1 -> //depot/%%1/%%2\n" +
                        "\t- //client/a/deep/%%1 -> //depot/a/deep/%%1\n" +
                        "\t- //client/a/.../deep/%%1 -> //depot/a/.../deep/%%1\n" +
                        "\t- //client/a/%%1 -> //depot/a/%%1\n" +
                        "\t  //client/a/... -> //depot/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //client/ (true)\n" +
                        "//client/sub/b/a -> //depot/a/b\n" +
                        "//client/a/b/c/d -> //depot/a/b/c/d\n" +
                        "//client/sub/j/f -> //depot/f/j\n" +
                        "//client/f/j -> \n" +
                        "//client/here/there/deep/everywhere -> //depot/deep/here/there/everywhere\n" +
                        "//client/there/here/deep/everywhere -> //depot/deep/there/here/everywhere\n",
                mt.Build(LHS, false, LHS, maptest_r_data, false));

    }

    @Test
    public void MoreJoins4Test() throws Exception {
        //Add another level with some restricts
        //tm `maptest -M maptest.mf -M JRR#maptest.0.mf -M RLL#maptest.rf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  //... -> //...\n" +
                        "map join map [1] (join RHS:RHS): 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../%%1 -> //client/.../deep/%%1\n" +
                        "\t  //depot/%%2/%%1 -> //client/sub/%%1/%%2\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.Join(RHS, maptest_0_mf, RHS));
        Assert.assertEquals(
                "map raw map [2]: 2 items, joinError false, emptyReason \n" +
                        "\t  //.../a/* -> //.../a/*\n" +
                        "\t  //depot/a/test -> //depot/a/test\n" +
                        "map join map [2] (restrict LHS:LHS): 6 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/a/%%1 -> //client/a/deep/%%1\n" +
                        "\t  //depot/deep/.../a/%%1 -> //client/.../a/deep/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/sub/%%1/a\n" +
                        "\t  //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/a/%%1 -> //client/a/a/%%1\n" +
                        "\t  //depot/a/.../a/%%1 -> //client/a/.../a/%%1\n",
                mt.Restrict(LHS, maptest_rf, LHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/a/%%1 -> //client/a/deep/%%1\n" +
                        "\t  //depot/deep/.../a/%%1 -> //client/.../a/deep/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/sub/%%1/a\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/a/%%1 -> //client/a/a/%%1\n" +
                        "\t  //depot/a/.../a/%%1 -> //client/a/.../a/%%1\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "//depot/a/b -> //client/sub/b/a\n" +
                        "//depot/a/b/c/d -> \n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> \n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void MoreJoins5Test() throws Exception {
        //tm `maptest -M maptest.mf -M RLL#maptest.rf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_mf));
        Assert.assertEquals(
                "map raw map [1]: 2 items, joinError false, emptyReason \n" +
                        "\t  //.../a/* -> //.../a/*\n" +
                        "\t  //depot/a/test -> //depot/a/test\n" +
                        "map join map [1] (restrict LHS:LHS): 6 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/a/%%1 -> //client/a/deep/%%1\n" +
                        "\t  //depot/deep/.../a/%%1 -> //client/.../a/deep/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/sub/%%1/a\n" +
                        "\t  //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/a/%%1 -> //client/a/a/%%1\n" +
                        "\t  //depot/a/.../a/%%1 -> //client/a/.../a/%%1\n",
                mt.Restrict(LHS, maptest_rf, LHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/a/%%1 -> //client/a/deep/%%1\n" +
                        "\t  //depot/deep/.../a/%%1 -> //client/.../a/deep/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/sub/%%1/a\n" +
                        "\t- //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/%%1 -> //client/a/%%1\n" +
                        "\t  //depot/a/a/%%1 -> //client/a/a/%%1\n" +
                        "\t  //depot/a/.../a/%%1 -> //client/a/.../a/%%1\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "//depot/a/b -> //client/sub/b/a\n" +
                        "//depot/a/b/c/d -> \n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> \n" +
                        "//depot/deep/here/there/everywhere -> \n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Test
    public void Ditto1Test() throws Exception {
        //Test the MfAndmap(one - to - many mappings) ->Here be dragons !
        //Basic mapping
        //tm `maptest -M maptest.3.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t- //depot/deep/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t- //depot/a/numberall -> //client/a/numberall\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//depot/a/b -> //client/a/b\n" +
                        "//depot/a/b/c/d -> //client/a/b/c/d\n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> //client/deep/numberall\n" +
                        "//depot/deep/here/there/everywhere -> //client/deep/here/there/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, false));
    }

    @Ignore()
    @Test
    public void Ditto2Test() throws Exception {
        //Exploded mapping
        //tm `maptest -e -M maptest.3.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t- //depot/deep/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t- //depot/a/numberall -> //client/a/numberall\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//depot/a/b -> //client/a/b\n" +
                        "//depot/a/b/c/d -> //client/a/b/c/d\n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> //client/b/numberall (readonly)\n" +
                        "//depot/numberall -> //client/a/numberall (readonly)\n" +
                        "//depot/numberall -> //client/deep/numberall\n" +
                        "//depot/deep/here/there/everywhere -> //client/deep/here/there/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, true));
    }

    @Ignore()
    @Test
    public void Ditto3Test() throws Exception {
        //The other way
        //tm `maptest -M maptest.3.mf -s -b maptest.3.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t- //depot/deep/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t- //depot/a/numberall -> //client/a/numberall\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//client/a/b -> //depot/a/b\n" +
                        "//client/a/b/c/d -> //depot/a/b/c/d\n" +
                        "//client/sub/j/f -> \n" +
                        "//client/numberall -> \n" +
                        "//client/here/there/deep/everywhere -> \n" +
                        "//client/deep/here/there/everywhere -> //depot/deep/here/there/everywhere\n" +
                        "//client/choosy/little/beggars -> \n" +
                        "//client/a/test -> //depot/a/test\n" +
                        "//client/numberall -> \n" +
                        "//client/a/numberall -> //depot/numberall (readonly)\n" +
                        "//client/b/numberall -> //depot/numberall (readonly)\n" +
                        "//client/c/numberall -> \n" +
                        "//client/deep/numberall -> //depot/numberall\n",
                mt.Build(RHS, false, LHS, maptest_3_data, false));
    }

    @Ignore()
    @Test
    public void Ditto4Test() throws Exception {
        //The other way(exploded)
        //tm `maptest -e -M maptest.3.mf -s -b maptest.3.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t- //depot/deep/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t- //depot/a/numberall -> //client/a/numberall\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//client/a/b -> //depot/a/b\n" +
                        "//client/a/b/c/d -> //depot/a/b/c/d\n" +
                        "//client/sub/j/f -> \n" +
                        "//client/numberall -> \n" +
                        "//client/here/there/deep/everywhere -> \n" +
                        "//client/deep/here/there/everywhere -> //depot/deep/here/there/everywhere\n" +
                        "//client/choosy/little/beggars -> \n" +
                        "//client/a/test -> //depot/a/test\n" +
                        "//client/numberall -> \n" +
                        "//client/a/numberall -> //depot/numberall (readonly)\n" +
                        "//client/b/numberall -> //depot/numberall (readonly)\n" +
                        "//client/c/numberall -> \n" +
                        "//client/deep/numberall -> //depot/numberall\n",
                mt.Build(RHS, false, LHS, maptest_3_data, true));
    }

    @Ignore()
    @Test
    public void Ditto5Test() throws Exception {
        //And some joins ?
        // tm `maptest -e -M maptest.3.mf -M JRR#maptest.0.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map raw map [1]: 1 items, joinError false, emptyReason \n" +
                        "\t  //... -> //...\n" +
                        "map join map [1] (join RHS:RHS): 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.Join(RHS,maptest_0_mf,RHS));
        Assert.assertEquals(
                "map mumbo map: 7 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t- //depot/deep/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t- //depot/a/numberall -> //client/a/numberall\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//depot/a/b -> //client/a/b\n" +
                        "//depot/a/b/c/d -> //client/a/b/c/d\n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> //client/b/numberall (readonly)\n" +
                        "//depot/numberall -> //client/a/numberall (readonly)\n" +
                        "//depot/numberall -> //client/deep/numberall\n" +
                        "//depot/deep/here/there/everywhere -> //client/deep/here/there/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, true));
    }

    @Test
    public void Ditto6Test() throws Exception {
        //tm `maptest -e -M maptest.3.mf -M JRR#maptest.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map raw map [1]: 3 items, joinError false, emptyReason \n" +
                        "\t  //depot/deep/.../* -> //client/.../deep/*\n" +
                        "\t  //depot/%%1/%%2 -> //client/sub/%%2/%%1\n" +
                        "\t  //depot/a/... -> //client/a/...\n" +
                        "map join map [1] (join RHS:RHS): 6 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //depot/a/numberall\n" +
                        "\t  //depot/deep/deep/%%1 -> //depot/deep/deep/%%1\n" +
                        "\t  //depot/deep/.../deep/%%1 -> //depot/deep/deep/.../%%1\n" +
                        "\t  //depot/a/deep/%%1 -> //depot/deep/a/%%1\n" +
                        "\t  //depot/a/.../deep/%%1 -> //depot/deep/a/.../%%1\n" +
                        "\t  //depot/a/... -> //depot/a/...\n",
                mt.Join(RHS,maptest_mf,RHS));
        Assert.assertEquals(
                "map mumbo map: 9 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //depot/a/numberall\n" +
                        "\t  //depot/deep/deep/%%1 -> //depot/deep/deep/%%1\n" +
                        "\t  //depot/deep/.../deep/%%1 -> //depot/deep/deep/.../%%1\n" +
                        "\t  //depot/a/deep/%%1 -> //depot/deep/a/%%1\n" +
                        "\t  //depot/a/.../deep/%%1 -> //depot/deep/a/.../%%1\n" +
                        "\t- //depot/a/numberall -> //depot/a/numberall\n" +
                        "\t- //depot/a/deep/%%1 -> //depot/a/deep/%%1\n" +
                        "\t- //depot/a/.../deep/%%1 -> //depot/a/.../deep/%%1\n" +
                        "\t  //depot/a/... -> //depot/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//depot/a/b -> //depot/a/b\n" +
                        "//depot/a/b/c/d -> //depot/a/b/c/d\n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> //depot/a/numberall (readonly)\n" +
                        "//depot/deep/here/there/everywhere -> \n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, true));
    }

    @Ignore()
    @Test
    public void Ditto7Test() throws Exception {
        //tm `maptest -e -M maptest.3.mf -M JRR#maptest.2-1.mf -s maptest.data`;
        MapTester mt = new MapTester(-1);
        Assert.assertEquals(
                "map raw map: 5 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //client/b/numberall\n" +
                        "\t& //depot/numberall -> //client/a/numberall\n" +
                        "\t  //depot/numberall -> //client/deep/numberall\n" +
                        "\t  //depot/deep/... -> //client/deep/...\n" +
                        "\t  //depot/a/... -> //client/a/...\n",
                mt.SetMap(maptest_3_mf));
        Assert.assertEquals(
                "map raw map [1]: 2 items, joinError false, emptyReason \n" +
                        "\t+ //depot/deep/... -> //client/...\n" +
                        "\t  //depot/a/... -> //client/...\n" +
                        "map join map [1] (join RHS:RHS): 10 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //depot/deep/b/numberall\n" +
                        "\t& //depot/numberall -> //depot/a/b/numberall\n" +
                        "\t& //depot/numberall -> //depot/deep/a/numberall\n" +
                        "\t& //depot/numberall -> //depot/a/a/numberall\n" +
                        "\t+ //depot/numberall -> //depot/deep/deep/numberall\n" +
                        "\t  //depot/numberall -> //depot/a/deep/numberall\n" +
                        "\t+ //depot/deep/... -> //depot/deep/deep/...\n" +
                        "\t  //depot/deep/... -> //depot/a/deep/...\n" +
                        "\t+ //depot/a/... -> //depot/deep/a/...\n" +
                        "\t  //depot/a/... -> //depot/a/a/...\n",
                mt.Join(RHS,maptest_2_1_mf,RHS));
        Assert.assertEquals(
                "map mumbo map: 13 items, joinError false, emptyReason \n" +
                        "\t& //depot/numberall -> //depot/deep/b/numberall\n" +
                        "\t& //depot/numberall -> //depot/a/b/numberall\n" +
                        "\t& //depot/numberall -> //depot/deep/a/numberall\n" +
                        "\t& //depot/numberall -> //depot/a/a/numberall\n" +
                        "\t+ //depot/numberall -> //depot/deep/deep/numberall\n" +
                        "\t  //depot/numberall -> //depot/a/deep/numberall\n" +
                        "\t+ //depot/deep/... -> //depot/deep/deep/...\n" +
                        "\t- //depot/deep/numberall -> //depot/a/deep/numberall\n" +
                        "\t  //depot/deep/... -> //depot/a/deep/...\n" +
                        "\t- //depot/a/numberall -> //depot/deep/a/numberall\n" +
                        "\t+ //depot/a/... -> //depot/deep/a/...\n" +
                        "\t- //depot/a/numberall -> //depot/a/a/numberall\n" +
                        "\t  //depot/a/... -> //depot/a/a/...\n" +
                        "strings for map:\n" +
                        "\t-> 0: //depot/a/ (true)\n" +
                        "\t-> 1: //depot/deep/ (true)\n" +
                        "\t-> 2: //depot/numberall (false)\n" +
                        "//depot/a/b -> //depot/deep/a/b\n" +
                        "//depot/a/b/c/d -> //depot/deep/a/b/c/d\n" +
                        "//depot/f/j -> \n" +
                        "//depot/numberall -> //depot/deep/b/numberall (readonly)\n" +
                        "//depot/numberall -> //depot/a/b/numberall (readonly)\n" +
                        "//depot/numberall -> //depot/deep/a/numberall (readonly)\n" +
                        "//depot/numberall -> //depot/a/a/numberall (readonly)\n" +
                        "//depot/numberall -> //depot/deep/deep/numberall\n" +
                        "//depot/deep/here/there/everywhere -> //depot/deep/deep/here/there/everywhere\n" +
                        "//depot/choosy/little/beggars -> \n",
                mt.Build(LHS, false, LHS, maptest_data, true));
    }

    @Test
    public void CaseSensitivity1Test() throws Exception {
        //Case Sensitivity
        //tm `maptest -C0 -M maptest.4-1.mf maptest.4-0.mf`;
        MapTester mt = new MapTester(0);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  //sentrysuite/... -> //sentrysuite/...\n",
                mt.SetMap(maptest_4_1_mf));
        Assert.assertEquals(
                "map mumbo map: 1 items, joinError false, emptyReason \n" +
                        "\t  //sentrysuite/... -> //sentrysuite/...\n" +
                        "//SentrySuite/... -> \n",
                mt.Build(LHS, false, null, maptest_4_0_mf, false));
    }

    @Ignore()
    @Test
    public void CaseSensitivity2Test() throws Exception {
        //tm `maptest -C1 -M maptest.4-1.mf maptest.4-0.mf`;
        MapTester mt = new MapTester(1);
        Assert.assertEquals(
                "map raw map: 1 items, joinError false, emptyReason \n" +
                        "\t  //sentrysuite/... -> //sentrysuite/...\n",
                mt.SetMap(maptest_4_1_mf));
        Assert.assertEquals(
                "map mumbo map: 1 items, joinError false, emptyReason \n" +
                        "\t  //sentrysuite/... -> //sentrysuite/...\n" +
                        "//SentrySuite/... -> //sentrysuite/...\n",
                mt.Build(LHS, false, null, maptest_4_0_mf, false));
    }
}
