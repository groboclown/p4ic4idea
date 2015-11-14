# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before release.

1. Files open for add seem to stick around in the local cache if the push to
   the server failed.
   Change view state doesn't correctly match what's in the actual changes.
   *TODO partial fix; it requires a full "mark everything as dirty" call.
   Double check what remains to fix with this.*
   These errors can happen with submits and reverts.



## Big Bugs

There are multiple "todos" and "fixmes" marked in the code, but these are the bugs
that should be fixed up.

1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.



## Smaller Bugs


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?


