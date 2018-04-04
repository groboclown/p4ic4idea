package com.perforce.p4java;

import java.lang.reflect.InvocationTargetException;

import com.perforce.p4java.exception.P4JavaException;

/**
 * @author Sean Shou
 * @since 20/09/2016
 */
@FunctionalInterface
public interface UnitTestWhen<T> {
  T when() throws P4JavaException, InvocationTargetException, IllegalAccessException;
}
