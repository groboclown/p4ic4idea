# To Do List

Some of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view of Github.



## Bugs

### Creating a changelist can cause an error

The plugin will attempt to create a new changelist in some cases when it already exists in Perforce (say, after a failed submit).

Additionally, if Perforce contains multiple changelists with the same name, the creation of these will fail due to a colliding name. ("Attempt to create duplicate changelist")

### Files move to default changelist on refresh

When the changelist view is refreshed, the opened files in all the changelists can be moved to the default changelist.

Looks like a potential caching issue.  The cache is queried to discover which changelist the file belongs.  If the cache isn't updated with the new changelist (or is ordered incorrectly), then the plugin will think the file is in the wrong changelist, and move it.

### Files open for edit outside IDE do not show up in IDE changelist

If you open a file for edit outside the IDE, and refresh the changelist view in the IDE, the outside edited files do not show up.

This might be another cache issue.  The cached list of files may not be loaded correctly.  Alternatively, the files may not be marked as dirty in `P4ChangeProvider`.

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or duplicates existing pending actions.  The pending action list must be altered to reflect the new action. 

### Remove Pending Action Refresh

When the pending actions in the Active Connection panel are removed, the UI does not refresh.  A forced refresh shows it removed. 

### Check Connection from VCS Root Directory Configuration memory leak

If you check the connection from the VCS root directory configuration dialog, a serious memory leak happens that puts the breaks on the IDE.

This occurs during the New Project from Version Control, and might occur under normal circumstances.

Source: `P4VcsRootConfigurable`

Note that, under New Project from Version Control, the Vcs Root in the `P4RootConfigPanel` can change, but the panel won't be updated with the new root.  This could be the source of at least one issue.

### Duplicate event log entries

Notifications are showing up x4 in the event log.  Could be a sign of excessive event generation.

### Open for Edit doesn't move a file to a changelist.

If a file is writable, the connection is offline, and the "Automatically open for edit..." option is not selected, then expliticly checking out the file will not cause the pending action to be reflected in the UI changelists.  It will be shown in the cached list of actions.



## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### SSO and Manual Passwords

The SSO and asking the user for passwords are not well tested.

### Manage Pending Operations

If there are pending actions that failed to go through due to errors, the user needs a way to manage these operations.  This should be done through the active connection panel.

### Swarm Integration

Swarm integration needs to be re-instated.

### Revert Unchanged

Re-add implementation.



## Near-Term Functionality

These pieces of functionality are not required for the 0.10 release, but should be implemented soon after release.

### Caching File Contents

The cache mechanism should support making a copy of a file when an operation happens, to allow for better offline support.

### Repository View

The repository view doesn't show any commits.

### Shelved file support

Re-add support for showing shelved files and managing shelved files.

### Additional History Browsing Support

Context menus or sidebar actions for viewing additional history for existing items, such as the history of files from within the changelist details panel. 

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
