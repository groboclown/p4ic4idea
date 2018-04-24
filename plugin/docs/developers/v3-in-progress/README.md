# Design Documents for the v3 Implementation

## Big Issues

These are the big issues discovered with the old plugin
that have influenced the new design.

* [Configuration and Connection Management](connection.md)
* [Synchronous vs Asynchronous Communication](threading.md)
* [Error reporting](errors.md)


## Unit Tests

Unit tests are now basically required.  Lots of support code for
unit tests is present as their own projects.  Likewise, code should
be written so that it's easily testable.  Some actions, like reading
from the system properties, should be delegated to helper classes
to make unit testing easy.
