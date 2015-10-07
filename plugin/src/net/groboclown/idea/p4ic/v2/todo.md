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
1. If a file is not marked for add (unversioned), then it is moved into a changelist,
   it shows itself in that changelist just fine.  However, if the IDE is restarted,
   then the file is back to being unversioned.  Happens with a file named
   `a@b.bat`, so there may be a file escape issue on add, or moving to changelist
   might be not correctly adding the file.
1. Files open for edit/add/etc may not be shown in changelist.  This can happen
   if IDEA is not aware of the change.  There needs to be a "right place" to
   mark these files as dirty; if it's done in the change refresh, then
   that will cause an infinite refresh.
1. Ignored files move between "unversioned" list and default list when
   `ChangeListSync` runs.


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
