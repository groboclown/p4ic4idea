# To Do List

Some of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view
of Github.

## Update for Modern IDE Versions

The codebase is based on 2013 versions of the IDE, and slowly upgraded from then.  Work is going on to upgrade this to support >=203 of the IDE (probably should just go up to 213 of the IDE).  This requires replacing many of the old ways of doing things and removing use of deprecated APIs.

* Easy
  * Replace deprecated APIs with the modern usage.
    * P4Java is an exception, here; the deprecation usage is due to localized migrations away from the original Perforce code.
    * idea-test-core is a smaller exception, as it's test-only code. 
* Hard
  * Update `P4CheckinEnvironment` to use modern APIs.  The parent class has changed how it works in many ways, and this implementation should reflect the new way of doing things.
  * Change `InvalidPasswordMonitorComponent` with a per-project service.  There might be opportunities here to fix the password handling issues in general.  Maybe add to `PluginSetup`?
  * Change `ProjectConfigRegistry` / `ProjectConfigRegistryImpl` to a project service.
  * Change `PersistentRootConfigComponent` to a project service.  Include updates to how the storage is managed.
  * Change `CacheComponent` to a project service.  Include updates to how the storage is managed.
  * Change `CacheViewRefreshComponent` to a project service, or a listener extension on `FileCacheUpdatedMessage.addListener`
  * Change `P4ServerComponent` to a project service.
  * Change `SwarmConnectionComponent` to a project service.
  * Change `UserErrorComponent` to a project service.
  * Change `P4ChangeListDecorator` to a project service or extension point.
  * Change `VcsDockedComponent` to be a tool window extension point.
  * Update `P4CommittedChangesProvider` to use the modern API.
* Other
  * Use of `PrimitiveMap` is a major hack.  Need a better alternative.


## Retest

These items need retesting.

### Submit

1. Multi-root submit.
2. With / without jobs.
3. Sub-set of files with the default changelist.
    * Recheck before closing off #176
4. Sub-set of files with a numbered changelist. 
    * Recheck before closing off #176
5. Submit with empty comment.
    * Update #52:
    
        Note that `P4CheckinEnvironment` supplies an implementation for `createCommitSession()`, which has a hook for `boolean canExecute(Collection<Change> changes, String commitMessage)`, but that isn't used.  At least, it's not called for commit message updates.


### SSO

Connect with SSO server.

### Offline

1. Curate action combinations

### "import+" Streams

(See #167) Investigate support for imports in streams.


## Bugs

### Memory Leak

(#193)

Possible contenders:
    * AbstractCacheUpdateEvent - should be minor in memory size, but if something keeps these objects around, it will explode in memory size.  Debugging added to check for creation / deletion.
    * P4ServerName, P4ClientRef - simple objects that could be reused.  Might lead to additional memory problems, and they don't take up much memory on their own, anyway.
    * WriteActionCacheImpl - might be too heavyweight, since it is created with most write cache actions.
    * CacheComponent - ensure access to internal cache objects is better restricted, to prevent leaking data out where it shouldn't be stored.
    * ConnectCommandRunner - creates a whole bunch of objects.  Is there room for improvement?
    * BasicAction - recursively collects files.  Need to ensure nothing keeps hold of this data.
    * P4ServerComponent - can create new instances, leading to a memory leak.
    * Project and Application component types - ensure that if they are created outside the standard lifecycle, they are cleaned up right.
    * P4ChangeProvider - does a whole bunch of memory stuff during each call.  May also need file cache cleanup implemented.
    * P4AnnotatedFileImpl - seems to use a bunch of memory (by necessity), but the IDE might hold on to it, or it might call some kind of dispose that isn't listened to.
    * ProjectConfigRegistryImpl, TopCommandRunner
    * SubmitModel / SubmitPanel - not a normal model for the components.
    

Alternative ideas:
    * Hash key for configs in stores aren't correct - this would cause the cache state to explode while showing incorrect cache state across different calls.  Contenders are P4ServerName, ServerConfig, ClientConfig, ClientServerRef.  State might be right if they all have listeners.
    * The issue may be happening when the server needs to reconnect (people complaining had this issue).  Maybe it's with the code related to creating the new server connection.  Old server connection is kept around somewhere?
    * Background tasks eat up memory, or hold onto a large object graph.

### Revert File Logic

**Priority: Critical**

(#181) Revert file functionality might have an issue in terms of where it's being applied.

* Request to move a file when the source is known by the server and the target is open for add, edit, or delete; the target is reverted.  This logic MUST be re-examined for correct behavior.
* Multiple file operations.  Because of the OpenFileStatus grouped files, only the similarly organized files can be reverted.

Because of the complexity of file moving, the logic is now in the specialized class `MoveFile`.

(Dropped down to Critical due to additional mitigating code, and inability to reproduce.)

This might also be affected by the `PendingActionCurator`, as it can drop actions leading to an unexpected revert if the connection to the server is slow.

To help with this, include more unit tests for `MoveFile` and `PendingActionCurator`.

### File Cache Conflict

**Priority: Critical**

(#174) "File Cache Conflict" popup happens every 5-10 minutes.  Typically happens during heavy iteration of perforce backed files. "I do a lot of editing followed by alt+tab to another application, back and forth. I think idea automatically saves the file any time the window loses focus."

### Undo File Move

**Priority: Major**

(#184) Steps to reproduce:

1. Open a file for edit, so that the contents are different locally than what's on the server.
2. In the IDE, move the file to another directory.
3. Perform "Undo" (Ctrl-Z in Windows/Linux) to cause the IDE to undo the move operation.

This leaves the IDE state and the Perforce state in conflict. Perforce keeps the file in the "p4 move" state, but the local file state reflects the pre-move state.

This issue is particularly hairy because in order to fix the "p4 move" state, you need to perform a revert, but that undoes local changes to the file.

### Annotation diff

**Priority: Major**

(#88) Selecting "Show Diff" from the annotation gutter shows only "Can not load data to show diff".

### File Change Operations Do Not Refresh Change List View

**Priority: Minor** (first minor to tackle)

(#185) After file operations (add, edit, delete, move), the change list view does not refresh itself.  The user must manually refresh the view before the changes show up.  Revert works correctly.

Looks like it could be due to pushing events off into worker threads.  The view refresh is triggered *after* the cache updates, which should be a correct time for it.  However, even with that fix, it's still not refreshing.  Perhaps a delay (in case the requested refresh happens too soon after the previous refresh), or a forced wait in-thread would do the trick?

### Remove Pending Action requires Refresh

**Priority: Minor**

(#186) When the pending actions in the Active Connection panel are removed, the UI does not refresh.  A forced refresh shows it removed.   `CachePendingActionHandlerImpl` will need to send out an event on remove.

This will need to be done in order to have IdeChangelistCacheStore remove action links if the action is deleted.


## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### Symlink Files

The symlink file checks that were a major effort in the old plugin were not transferred over.

The code was located in `FileActionsServerCacheSync`.  The new code needs to be located in `ConnectCommandRunner.addEditFile`.

### Ignore File

The `P4ChangeProvider` and `P4VFSListener` and `P4EditFileProvider` classes must inspect the `IgnoreFileSet` for the corresponding .p4ignore file, to ensure that the add operation (and only add operation) acknowledges the ignore file status.

The `P4EditAction` should respect the user's request and force the add.


## Near-Term Functionality

These pieces of functionality are not required for the 0.10 release, but should be implemented soon after release.

### Auto-Refresh of Active Connections

The Active Connections view requires the user to click refresh, even when empty.  This has to do with component loading order, but it still is an extra action the user shouldn't have to do.

### Shelved file support

Re-add support for showing shelved files and managing shelved files.  Shelving files is currently only supported when a Swarm review is created.

### Swarm Integration

Swarm integration creates the review, but does not allow for modifying an existing review.  There is also no way to see beforehand which changelist has a review associated with it.

### Revert Unchanged

Re-add implementation.

### Additional History Browsing Support

Context menus or sidebar actions for viewing additional history for existing items, such as the history of files from within the changelist details panel. 

### Pending Changes View

Re-add the pending changes view, for job and shelved file inspection.

### Use Windows Registry Passwords

The passwords stored in the Windows Registry (through the `p4 set PASSWD` command and other password commands) is stored in an encrypted way.  The code should include handling the encrypted values.  The publicly available p4 cli C code contains the implementation for how it's done.  However, that code is non-trivial. 

### OSX plist Configuration Support

(#191) The plugin does not look in the plist files for the Perforce configuration information.  

### Relative P4CONFIG support

The choice was made to eliminate the use of relative P4CONFIG files, and instead managed through the VCS Root mechanism. However, without this, the full environment support won't work.  This needs to be re-added, with support of the VSC Root mapping mechanism in `P4Vcs`.  However, the user needs to be able to manage it, and that requires new UI support.  This is a big feature, and will require some careful planning to handle correctly.

### Locally Cached Pending Changes

When a user makes a change to file (add, delete, move, edit), a cached version of the before-change should be kept.  See `com.intellij.history.integration.IdeaGateway#acquireAndUpdateActualContent()` for how the local data is preserved.  Use `VirtualFile#putUserData(custom key)` and `VirtualFile#getUserData(custom key)` to save off the data.  Note that extreme care must be taken to properly clean up the cached data.  This means tight object lifecycle management.

### Check Description Length in Submit Dialog

Really old, long standing issue.  The UI should prevent submitting a change if the description is empty.

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or duplicates existing pending actions.  The pending action list must be altered to reflect the new action.

Most of this work has been done.  It is handled in `PendingActionCurator`.  A few other cases still should be handled, but it's not necessary for correct operation.

### Annotation Gutter Options

There are many grayed-out options in the context menu for the Annotation gutter.  Explore ways to enable these.

### `P4CommandRunner.FutureResult`

This class isn't used as initially intended.  Need to re-examine the users of this class to ensure that it's still needed (think about possible future functionality needs).  If it isn't needed, then rethink how it should be used instead.
