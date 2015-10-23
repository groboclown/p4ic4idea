# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.

1. working offline at startup has major issues (server off).
    1. cannot reconnect once server comes back online (ssl mode only, it seems)
    1. the cached values for changelist assignment is not
       loaded correctly at restart.  Only one is restored?
       They are all deserialized.
    1. initial display does not load the cached state?  This is
       because of the initial assumption that it's working online,
       but that causes a working offline exception.  Instead, the
       working offline exception should be displayed (as it is
       currently happening), but the exception should not cause
       the refresh to stop.
    1. The "I'm offline now" message seems to not propagate correctly.
       Needs better tracing of the server pool, and its currently
       known online/offline state.
1. Reinstate the old pwd state object so that it can ensure that the
   state values are wiped out, for those migrating to the new storage
   mechanism.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.


## Big Bugs


1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. Changelist refresh: some files alternate between showing up and disappearing
   with each refresh.
1. Job list in submit needs wrapping scroll pane.
1. Failure in job submit does not show error to user.
1. ssh connection failure due to can't reach host port is indistinguishable from
   when the server is down.


## Parts needing heavy testing

1. `ChangeListSync` needs to be thoroughly debugged.  It seems to be working, but it
   needs heavy testing.
1. All of the history related items.
1. `P4StatusUpdateEnvironment` group mapping


## Not-implemented behavior in existing features

(see `// FIXME` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include `LOG.isDebugEnabled()` calls if debug is used.
1. `ClientCacheManager`
1. `UpdateGroup`


## Smaller Bugs

1. Connection setup panel - when initial "refresh" button is pressed, the directory list
   is loaded, and the first one is displayed as selected.  However, the properties are
   not loaded.  Looks like a timing issue.
1. Connection setup panel - For relative configs, the directory list is not refreshed.
   Ensure it's reading the right root directory path.
   Ensure it's reading the right root directory path.
1. Going offline does not show the offline icon.
1. Remove the old, dead code.
1. Many "to string" parts in `P4Bundle.message` calls are done on FilePath, which
   do not display well to the user.  Make these nicer.


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

