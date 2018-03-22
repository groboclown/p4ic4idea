package com.perforce.p4java.common.function;

import com.perforce.p4java.exception.P4JavaException;

/**
 * @author Sean Shou
 * @since 21/09/2016
 */
public interface FunctionWithException<T, R> {
  R apply(T t) throws P4JavaException;
}
