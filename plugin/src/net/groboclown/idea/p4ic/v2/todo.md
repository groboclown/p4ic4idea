# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before release.

1. Add a file while offline, then move it.  File stays added in old position.
   Need to clear it out, both the file and the pending update.  This means a
   minor offline mode revert (just for adds now, which is very doable).
1. Files open for add seem to stick around in the local cache if the push to
   the server failed.
1. Warnings then warnings means the old ones are removed.
1. Change view state doesn't correctly match what's in the actual changes.
   This comes from a big problem with the design.  We need to properly map
   state objects to the pending update objects, so that local caches can
   correctly be cleaned up.
1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.  *TODO double check this*


## Big Bugs

There are multiple "todos" and "fixmes" marked in the code, but these are the bugs
that should be fixed up.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.
1. The working online/offline icon isn't being correctly updated
   when state changes. *Fixed, needs check.*


## Not-implemented behavior in existing features

(see `// FIXME` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include `LOG.isDebugEnabled()` calls if debug is used.
1. `ClientCacheManager`


## Smaller Bugs

1. Replace "IntelliJ" and "IDEA" from the strings with the actual IDE name.
   Text display should fetch the name of the IDE, and not hard-code "IntelliJ" or
    "IDEA".

## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

