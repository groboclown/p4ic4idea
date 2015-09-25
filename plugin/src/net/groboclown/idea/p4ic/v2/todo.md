# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.

1. Need to implement `P4ContentRevision.getContent()`; but this is mostly driven by the history
   feature (see below).



## Features that drive architecture

There are some features that drive how the inner architecture will work.

1. History, as this will dictate the necessary stored information for
   cached files and revision numbers.
   
   
   
## Parts needing heavy testing

1. `ChangeListSync` needs to be thoroughly debugged.  It seems to be working, but it
   needs heavy testing.



## Features Needing Migration

1. `P4ChangelistListener`



## Not-implemented behavior in existing features

(see `// FIXME implement` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include "ifDebug" calls if debug is used.

