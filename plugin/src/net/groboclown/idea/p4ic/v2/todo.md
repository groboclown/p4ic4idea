# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.


## Big Bugs


1. Modified without checkout on files that aren't different
   than the P4 version.  *Fixed needs check.*
1. working offline at startup has major issues (server off).
    1. The "I'm offline now" message seems to not propagate correctly.
       Needs better tracing of the server pool, and its currently
       known online/offline state.
1. Reinstate the old pwd state object so that it can ensure that the
   state values are wiped out, for those migrating to the new storage
   mechanism.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.
1. When the connection config is changed, it requires a restart to pick up the changes.
1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. Job list in submit needs wrapping scroll pane.
1. Failure in job submit does not show error to user.
1. ssh connection failure due to can't reach host port is indistinguishable from
   when the server is down.
1. The AllClientsState and another class (find it) need to know the association of
   each project's client-server objects.  This is a new persistent data class that
   needs to be created.
1. The ServerConnection.runImmediately process is hokey and susceptible to abuse
   or incorrect usage.  It needs to be reworked.


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


## Smaller Bugs

1. Connection setup panel - when initial "refresh" button is pressed, the directory list
   is loaded, and the first one is displayed as selected.  However, the properties are
   not loaded.  Looks like a timing issue.
   
   It looks bigger than a timing issue.  When the configs aren't valid, then are changed
   to be valid, the refresh and client drop-down aren't working.
1. Connection setup panel - For relative configs, the directory list is not refreshed.
   Ensure it's reading the right root directory path.
   Ensure it's reading the right root directory path.
1. Going offline does not show the offline icon.
1. Remove the old, dead code.
1. Many "to string" parts in `P4Bundle.message` calls are done on FilePath, which
   do not display well to the user.  Make these nicer.
1. Replace "IntelliJ" and "IDEA" from the strings with the actual IDE name.
1. Selecting "no" to the add new file dialog to p4 still adds the file for edit.
1. Text display should fetch the name of the IDE, and not hard-code "IntelliJ" or
    "IDEA".

## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

