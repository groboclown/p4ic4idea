# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.


## Big Bugs

There are multiple "todos" marked in the code, but these are the bugs
that should be fixed up.

1. When the connection config is changed, it requires a restart to pick up the changes.
   *Fixed, needs check.*
1. Connection setup panel - when initial "refresh" button is pressed, the directory list
   is loaded, and the first one is displayed as selected.  However, the properties are
   not loaded.  *Fixed, needs check.*
1. Connection setup panel - For relative configs, the directory list is not refreshed.
   Ensure it's reading the right root directory path.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.
1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. Job list in submit needs wrapping scroll pane. *Fixed, needs check.*
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

1. Remove the old, dead code.
1. Replace "IntelliJ" and "IDEA" from the strings with the actual IDE name.
   Text display should fetch the name of the IDE, and not hard-code "IntelliJ" or
    "IDEA".

## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

