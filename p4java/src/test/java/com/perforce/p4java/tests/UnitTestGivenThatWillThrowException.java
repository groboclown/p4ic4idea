package com.perforce.p4java.tests;

import com.perforce.p4java.exception.P4JavaException;

/**
 * @deprecated p4ic4idea: use ExpectedException
 */
@FunctionalInterface
public interface UnitTestGivenThatWillThrowException {
  void given(Class<? extends P4JavaException> firstThrownException) throws P4JavaException;
}
