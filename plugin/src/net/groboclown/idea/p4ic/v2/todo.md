# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.



## Features that drive architecture

There are some features that drive how the inner architecture will work.


## Big Bugs


1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. Config GUI doesn't show properties correctly.
1. Changelist mappings don't seem to be persisted.
1. Changelist refresh: some files alternate between showing up and disappearing
   with each refresh.
1. Job list in submit needs wrapping scroll pane.
1. Failure in job submit does not show error to user.


## Parts needing heavy testing

1. `ChangeListSync` needs to be thoroughly debugged.  It seems to be working, but it
   needs heavy testing.
1. All of the history related items.
1. `P4StatusUpdateEnvironment` group mapping


## Not-implemented behavior in existing features

(see `// FIXME` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include `LOG.isDebugEnabled()` calls if debug is used.


## Smaller Bugs

1. Connection setup panel - when initial "refresh" button is pressed, the directory list
   is loaded, and the first one is displayed as selected.  However, the properties are
   not loaded.  Looks like a timing issue.
1. Remove the old, dead code.


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

