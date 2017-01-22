# Refactoring of Connection To Dos:

1. Replace config setup w/ new UI.
2. Re-investigate all the places where the connection can be marked invalid.
    Why does this happen?  Does it need to be there?
3. Add to the status bar widget a "reload" button.  Maybe also to the VCS pop-up menu and app menu.
4. Debugging, debugging, debugging.



# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before release.

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



## Smaller Bugs


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?  It allows for tagging
   revisions, so maybe that could be done.
