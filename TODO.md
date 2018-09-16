# To Do List

Some of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view
of Github.



## Bugs

### Move Files fails horribly

Due to several other bugs and missing functionality, moving a file that's marked for edit will cause big failures.  The
source file will still be tried to add, but it doesn't exist, so Perforce reports a failure, which causes the move
operation to barf up in the event logs.

### Open File For Add

This triggers at least 2 actions to attempt to add the file.  One will succeed, but the second will fail.

Possible sources: `P4EditFileProvider` and `P4VFSListener`.

### Open directories for add

The plugin incorrectly recognizes directories as files, and attempts to add them.

### Files open for edit outside IDE do not show up in IDE changelist

If you open a file for edit outside the IDE, and refresh the changelist view in the IDE, the outside edited files do not
show up.

### Files moved to another changelist outside IDE do not show up in IDE changelist

If you move a file from one changelist to another outside the IDE, the file is removed from the old changelist, but is
not added to the new changelist, and is instead "lost" (no longer in the changelist view).

This is happening from `P4ChangeProvider`.  It looks like the provider removes the change from the original, but it
is never correctly added to the new changelist.

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or
duplicates existing pending actions.  The pending action list must be altered to reflect the new action.

This needs to be handled by the `CacheQueryHandler`.  `CacheStoreUpdateListener` seems like the better place, but,
as it notes, it really shouldn't be messing with that queue due to possible in-flight server requests.

### Remove Pending Action requires Refresh

When the pending actions in the Active Connection panel are removed, the UI does not refresh.  A forced refresh shows it
removed.   `CachePendingActionHandlerImpl` will need to send out an event on remove.

This will need to be done in order to have IdeChangelistCacheStore remove action links if the action is deleted.

### Check Connection from VCS Root Directory Configuration memory leak

If you check the connection from the VCS root directory configuration dialog, a serious memory leak happens that puts
the breaks on the IDE.

This occurs during the New Project from Version Control, and might occur under normal circumstances.

Source: `P4VcsRootConfigurable`

Note that, under New Project from Version Control, the Vcs Root in the `P4RootConfigPanel` can change, but the panel
won't be updated with the new root.  This could be the source of at least one issue.

This might be fixed now.  The API has changed to force usage that prevents serious memory leaks from spreading.

### Duplicate event log entries

Notifications are showing up x4 in the event log.  Could be a sign of excessive event generation.

### Open for Edit doesn't move a file to a changelist.

If a file is writable, the connection is offline, and the "Automatically open for edit..." option is not selected, then
explicitly checking out the file will not cause the pending action to be reflected in the UI changelists.  It will be
shown in the cached list of actions.



## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### SSO and Manual Passwords

The SSO and asking the user for passwords are not well tested.

### Manage Pending Operations

If there are pending actions that failed to go through due to errors, the user needs a way to manage these operations.
This should be done through the active connection panel.

### Swarm Integration

Swarm integration needs to be re-instated.

### Revert Unchanged

Re-add implementation.



## Near-Term Functionality

These pieces of functionality are not required for the 0.10 release, but should be implemented soon after release.

### Caching File Contents

The cache mechanism should support making a copy of a file when an operation happens, to allow for better offline
support.

### Repository View

The repository view doesn't show any commits.

### Shelved file support

Re-add support for showing shelved files and managing shelved files.

### Additional History Browsing Support

Context menus or sidebar actions for viewing additional history for existing items, such as the history of files from
within the changelist details panel. 

### Pending Changes View

Re-add the pending changes view, for job and shelved file inspection.

### Use Windows Registry Passwords

The passwords stored in the Windows Registry (through the `p4 set PASSWD` command and other password commands) is stored
in an encrypted way.  The code should include handling the encrypted values.  The publicly available p4 cli C code
contains the implementation for how it's done.  However, that code is non-trivial. 

### Relative P4CONFIG support

The choice was made to eliminate the use of relative P4CONFIG files, and instead managed through the VCS Root mechanism.
However, without this, the full environment support won't work.  This needs to be re-added, with support of the VSC Root
mapping mechanism in `P4Vcs`.  However, the user needs to be able to manage it, and that requires new UI support.  This
is a big feature, and will require some careful planning to handle correctly.
