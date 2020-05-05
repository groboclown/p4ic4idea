package com.perforce.p4java.common.function;

import com.perforce.p4java.exception.RequestException;

/**
 * Represents a function that accepts one argument and produces a result.
 * whose functional method is {@link #apply(Object)}.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * apiNote: It will be replace by jdk-api if p4java use jdk1.8 in future.
 */
public interface Function<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t) throws RequestException;
}
