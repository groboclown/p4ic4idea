# Design Documents for the v3 Implementation

## Big Issues

These are the big issues discovered with the old plugin
that have influenced the new design.

* [Overall Architecture](architecture.md)
* [Configuration and Connection Management](connection.md)
* [Synchronous vs Asynchronous Communication](threading.md)
* [Error reporting](errors.md)
* [Caching and Data Storage](cache.md)
* [State Management](state.md)


## Unit Tests

Unit tests are now basically required.  Lots of support code for
unit tests is present as their own projects.  Likewise, code should
be written so that it's easily testable.  Some actions, like reading
from the system properties, should be delegated to helper classes
to make unit testing easy.

Behavior that directly depends upon the core IDE behavior is still not
well tested, due to reluctance to use heavyweight Idea test cases.
Currently, where this is used in test cases, it is simulated.  This is
a half measure, as it doesn't guarantee that future versions of the IDE
will continue to behave that way. 
