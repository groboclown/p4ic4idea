package com.perforce.p4java.tests;

import com.perforce.p4java.exception.P4JavaException;

public interface UnitTestGivenThatWillThrowException {
  void given(Class<? extends P4JavaException> firstThrownException) throws P4JavaException;
}
