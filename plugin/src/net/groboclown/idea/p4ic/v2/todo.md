# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before release.

1. Saving while in online mode causes a massive slowdown during the
   changelist sync check.  Need to double check the sync check to
   see:
    1. Why is the sync check slowing down the editing?  Is it just
       my virus checker?
    1. Sync seems to take a long time to run anyway.  See where the
       performance problems are.
    1. The editor slowdown happens when the sync is running (changelists
       have the "Updating..." text), and the changelist view does not
       have the waiting spinner.  The waiting spinner only seems to
       show up when the explicit refresh is pressed.


## Big Bugs

There are multiple "todos" and "fixmes" marked in the code, but these are the bugs
that should be fixed up.

1. There might be more "files stuck in cached state", but I haven't found more.
1. Moved files should be grouped together:
    1. move between changelist: one moves, then the other should also move.
    1. revert: one is reverted, the other is also reverted.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.
1. Per-file history shows the version as just the #revision number.  It
   no longer describes the actual depot path (specifically, for integration
   tracking).



## Smaller Bugs


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?  It allows for tagging
   revisions, so maybe that could be done.
