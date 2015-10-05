# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.




## Features that drive architecture

There are some features that drive how the inner architecture will work.

   
## Big Bugs


1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. Config GUI doesn't show properties correctly.
1. Doesn't seem to go offline - widget always shows green.
1. Changelist mappings don't seem to be persisted.


## Parts needing heavy testing

1. `ChangeListSync` needs to be thoroughly debugged.  It seems to be working, but it
   needs heavy testing.
1. All of the history related items.


## Features Needing Migration

1. `P4ChangelistListener`
1. `P4WorkOnlineAction` and the offline one.  These are the "turn them all on/off at once" actions.
1. `P4CheckinEnvironment` has remaining code in the submit method that needs to be implemented.


## Not-implemented behavior in existing features

(see `// FIXME` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include `LOG.isDebugEnabled()` calls if debug is used.
1. `AlertManager` - UI display for messages.


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.
