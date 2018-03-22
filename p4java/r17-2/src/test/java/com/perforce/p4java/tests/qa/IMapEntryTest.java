package com.perforce.p4java.tests.qa;


import com.perforce.p4java.impl.generic.core.MapEntry;

import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;


public class IMapEntryTest {

    private static Helper h = null;

    @BeforeClass
    public static void beforeClass() {

        h = new Helper();

    }


    // GET RIGHT QUOTED
    @Test
    public void getRightQuoted() throws Throwable {
        MapEntry mapEntry = new MapEntry(0, null, "\"left side\"", "\"right side\"");
        assertEquals("\"\"right side\"\"", mapEntry.getRight(true));
    }


    // GET LEFT QUOTED
    @Test
    public void getLeftQuoted() throws Throwable {
        MapEntry mapEntry = new MapEntry(0, null, "\"left side\"", "\"right side\"");
        assertEquals("\"\"left side\"\"", mapEntry.getLeft(true));
    }


    @AfterClass
    public static void afterClass() {
        h.after(null);
    }

}