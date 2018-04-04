package com.perforce.p4java.common.function;

/**
 * @author Sean Shou
 * @since 29/11/2016
 */
public interface BiPredicate<T, U> {
    /**
     * Evaluates this predicate on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @return {@code true} if the input arguments match the predicate,
     * otherwise {@code false}
     */
    boolean test(T t, U u);
}
