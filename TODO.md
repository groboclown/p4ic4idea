# To Do List

Some of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view
of Github.


## Bugs

### Open directories for add

The plugin incorrectly recognizes directories as files, and attempts to add them.

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

### Open for Edit doesn't move a file to a changelist.

If a file is writable, the connection is offline, and the "Automatically open for edit..." option is not selected, then
explicitly checking out the file will not cause the pending action to be reflected in the UI changelists.  It will be
shown in the cached list of actions.

### Diff Not Reporting Differences

The `diff` functionality shows no differences between versions.

### Move File Changelist View Message

Move file operations show up as "moved from ../../../..//depot/path/".



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

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or
duplicates existing pending actions.  The pending action list must be altered to reflect the new action.

Some of this work has started.  It is handled in `PendingActionCurator`.  Further implementation should use
the LocalHistory standard IDE component to read states, rather than caching file contents itself.



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
