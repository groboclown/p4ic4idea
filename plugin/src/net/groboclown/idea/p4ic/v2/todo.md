# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.

1. File update action removal has major issues.
    1. If a file isn't owned by Perforce, and it is deleted, then the
        local cache stores the file for delete.  However, if the delete
        action fails because it isn't on a server, then the file update
        state is not removed.
    1. The state is not updated after a submit or changelist pull.
        It looks like there need to be a special case for
        synchronizing the pending updates and
        actual updates (if a pending update doesn't exist, then the
        cached version needs to be flushed).


## Big Bugs

There are multiple "todos" and "fixmes" marked in the code, but these are the bugs
that should be fixed up.

1. Connection setup panel - when initial "refresh" button is pressed, the directory list
   is loaded, and the first one is displayed as selected.  However, the properties are
   not loaded.  *Fixed, needs check.*
1. Connection setup panel - For relative configs, the directory list is not refreshed.
   Ensure it's reading the right root directory path.  This also happens with
   specific config file.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.
1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. The working online/offline icon isn't being correctly updated
   when state changes. *Fixed, needs check.*


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

1. Replace "IntelliJ" and "IDEA" from the strings with the actual IDE name.
   Text display should fetch the name of the IDE, and not hard-code "IntelliJ" or
    "IDEA".

## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

