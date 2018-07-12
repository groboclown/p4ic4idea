# To Do List

Many of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view of Github.


## Bugs

### Creating a changelist can cause an error

The plugin will attempt to create a new changelist in some cases when it already exists in Perforce (say, after a failed submit).

Additionally, if Perforce contains multiple changelists with the same name, the creation of these will fail due to a colliding name. ("Attempt to create duplicate changelist")

### Files move to default changelist on refresh

When the changelist view is refreshed, the opened files in all the changelists can be moved to the default changelist.

### Connection State

The connection state, as the events are passed around, are not properly represented in the Active Connection panel.  Either the panel isn't showing the right state, or the events are wrong.

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or duplicates existing pending actions.  The pending action list must be altered to reflect the new action. 


## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### SSO and Manual Passwords

The SSO and asking the user for passwords are not well tested.

### Manage Pending Operations

If there are pending actions that failed to go through due to errors, the user needs a way to manage these operations.  This should be done through the active connection panel.

### Swarm Integration

Swarm integration needs to be re-instated.

### Sync files

Need to allow for pulling latest or other revisions.

### Revert Unchanged

Re-add implementation.

### Move Files

Finish implementing move files capability.



## Near-Term Functionality

These pieces of functionality are not required for the 0.10 release, but should be implemented soon after release.

### Caching File Contents

The cache mechanism should support making a copy of a file when an operation happens, to allow for better offline support.

### Relative P4CONFIG support

The choice was made to eliminate the use of relative P4CONFIG files, and instead managed through the VCS Root mechanism.
However, without this, the full environment support won't work.  This needs to be re-added, with support of the VSC Root
mapping mechanism in `P4Vcs`.  However, the user needs to be able to manage it, and that requires new UI support.  This
is a big feature, and will require some careful planning to handle correctly.

### Load Project from VCS

An old feature request.  Still needs to be added.

### Repository View

The repository view doesn't show any commits.

### Shelved file support

Re-add support for showing shelved files and managing shelved files.

### Additional History Browsing Support

Context menus or sidebar actions for viewing additional history for existing items, such as the history of files from within the changelist details panel. 

### Use Windows Registry Passwords

The passwords stored in the Windows Registry (through the `p4 set PASSWD` command and other password commands) is stored
in an encrypted way.  The code should include handling the encrypted values.  The publicly available p4 cli C code
contains the implementation for how it's done.  However, that code is non-trivial. 
