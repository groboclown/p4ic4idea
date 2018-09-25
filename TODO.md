# To Do List

Some of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view
of Github.


## Bugs

### Open for Edit doesn't move a file to a changelist.

**Priority: Critical**

**Current observed behavior:** If a file is writable, the connection is offline, and the "Automatically open for
edit..." option is not selected, then editing the file manually and explicitly opening for edit (Ctrl-Alt-A)
will only move the file to the "Modified locally without checkout" section in the changelist view.
The only way to make it checked out is to explicitly right click on the file in the changelist view and select
"checkout".

### Diff Not Reporting Differences

**Priority: Critical**

The `diff` functionality shows no differences between versions.

### Add / Edit Files Doesn't Trigger Changelist Refresh At The Right Time

**Priority: Critical** 

### Open directories for add

**Priority: Major**

The plugin incorrectly recognizes directories as files, and attempts to add them.

Some protections have been put in place around this, but it may not be prevented.  Additional testing is
required.  It might have been a Windows Subsystem for Linux issue.

### Move File Changelist View Message

**Priority: Major**

Move file operations show up as "moved from ../../../..//depot/path/".

### Remove Pending Action requires Refresh

**Priority: Minor**

When the pending actions in the Active Connection panel are removed, the UI does not refresh.  A forced refresh shows it
removed.   `CachePendingActionHandlerImpl` will need to send out an event on remove.

This will need to be done in order to have IdeChangelistCacheStore remove action links if the action is deleted.

### Requesting Online Mode Doesn't Change Active Connection State

**Priority: Minor**

When offline, pressing the "connect" button doesn't change the connection state.  The user must perform some other
server action to have this state change.

### Check Connection from VCS Root Directory Configuration memory leak

**Priority: Minor**

If you check the connection from the VCS root directory configuration dialog, a serious memory leak happens that puts
the breaks on the IDE.

This occurs during the New Project from Version Control, and might occur under normal circumstances.

Source: `P4VcsRootConfigurable`

Note that, under New Project from Version Control, the Vcs Root in the `P4RootConfigPanel` can change, but the panel
won't be updated with the new root.  This could be the source of at least one issue.

This might be fixed now.  The API has changed to force usage that prevents serious memory leaks from spreading.

### File Change Operations Do Not Refresh Change List View

**Priority: Minor** ?

After file operations (add, edit, delete, move), the change list view does not refresh itself.  The user must manually
refresh the view before the changes show up.  Revert works correctly. 


## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### SSO and Manual Passwords

The SSO and asking the user for passwords are not well tested.

### Swarm Integration

Swarm integration needs to be re-instated.

### Revert Unchanged

Re-add implementation.

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or
duplicates existing pending actions.  The pending action list must be altered to reflect the new action.

Some of this work has started.  It is handled in `PendingActionCurator`.



## Near-Term Functionality

These pieces of functionality are not required for the 0.10 release, but should be implemented soon after release.

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

### Locally Cache Pending Changes

When a user makes a change to file (add, delete, move, edit), a cached version of the before-change should be kept.
See `com.intellij.history.integration.IdeaGateway#acquireAndUpdateActualContent()` for how the local data is preserved.
Use `VirtualFile#putUserData(custom key)` and `VirtualFile#getUserData(custom key)` to save off the data.  Note that
extreme care must be taken to properly clean up the cached data.  This means tight object lifecycle management.
