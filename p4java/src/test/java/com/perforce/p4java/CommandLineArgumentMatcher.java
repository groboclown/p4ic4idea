/*
 * Copyright (c) 2016, Perforce Software, Inc.  All rights reserved.
 */
package com.perforce.p4java;

import java.util.Arrays;

import org.mockito.ArgumentMatcher;

/**
 * General class for matching perforce command line arguments passed as cmdargs.
 */
public class CommandLineArgumentMatcher implements ArgumentMatcher<String[]> {

    /** The expected result. */
    private String[] expected;

    /**
     * Instantiates a new parameters matcher.
     * 
     * @param expected
     *            the expected result
     */
    public CommandLineArgumentMatcher(final String[] expected) {
        this.expected = expected;
    }

    /**
     * Match an argument list passed to a mocked method invocation against an
     * expectation.
     */
    @Override
    public boolean matches(final String[] argument) {
        return Arrays.equals(expected, argument);
    }
    
    /**
     * Return the commmand line options being matched.
     */
    @Override
    public String toString(){
        return "Matching " + Arrays.toString(expected);
    }
}