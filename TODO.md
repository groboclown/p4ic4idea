# To Do List

Some of the to-dos are listed in the bug list on Github.  This list itself should be handled better in the project view
of Github.


## Bugs

### File Cache Conflict

"File Cache Conflict" popup happens every 5-10 minutes (#174).  Typically happens during heavy iteration of perforce
backed files. "I do a lot of editing followed by alt+tab to another application, back and forth. I think idea
automatically saves the file any time the window loses focus."

### Submit changelist that includes files not in project

**Priority: Critical**

When you submit a changelist, it can include files that are not visible, because they are ouside the scope of the
project.  The submission should only submit the files that the user sees.

The unseen files should be moved to the default changelist before submission.  If the default changelist is
submitted, then the code will need to create a changelist before submission, just like how the job association works.

### Removing a Config Does Not Remove Its Cached Data

**Priority: Critical**

If you remove a server or client configuration from a project, its cached values do not get deleted.  File and changelist
associations remain, causing bad UI display.

**This looks to be fixed, but it needs testing.**  Fix was in `PersistentRootConfigComponent`, where it now only loads
root paths that are declared as roots in the project.

### After Setting Password Active Connection is Still Offline

**Priority: Major**

Once the user has fixed the login problem by entering a password, the Active Connection panel always shows the
connection as offline, even after a refresh of both the Active Connection panel and the changelist view. 

### Weird Error

**Priority: Major**

Is this still happening?  May have been due to a preview idea issue.

```
java.lang.Throwable: Skipping invalid VCS root: VcsRoot{vcs=p4ic, path=null}
	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:134)
	at com.intellij.vcs.log.impl.VcsLogManager.findLogProviders(VcsLogManager.java:150)
	at com.intellij.vcs.log.util.VcsLogUtil.getActualRoot(VcsLogUtil.java:277)
	at com.intellij.vcs.log.history.VcsLogFileHistoryProviderImpl.canShowFileHistory(VcsLogFileHistoryProviderImpl.java:44)
	at com.intellij.openapi.vcs.actions.TabbedShowHistoryAction.canShowNewFileHistory(TabbedShowHistoryAction.java:85)
	at com.intellij.openapi.vcs.actions.TabbedShowHistoryAction.isEnabled(TabbedShowHistoryAction.java:73)
	at com.intellij.openapi.vcs.actions.TabbedShowHistoryAction.isEnabled(TabbedShowHistoryAction.java:59)
	at com.intellij.openapi.vcs.actions.TabbedShowHistoryAction.update(TabbedShowHistoryAction.java:47)
	at com.intellij.openapi.vcs.actions.AbstractVcsAction.performUpdate(AbstractVcsAction.java:71)
	at com.intellij.openapi.vcs.actions.AbstractVcsAction.update(AbstractVcsAction.java:43)
	at com.intellij.openapi.actionSystem.ex.ActionUtil.performDumbAwareUpdate(ActionUtil.java:171)
	at com.intellij.openapi.actionSystem.impl.Utils.doUpdate(Utils.java:202)
	at com.intellij.openapi.actionSystem.impl.Utils.expandActionGroup(Utils.java:151)
	at com.intellij.openapi.actionSystem.impl.Utils.expandActionGroup(Utils.java:177)
	at com.intellij.openapi.actionSystem.impl.Utils.expandActionGroup(Utils.java:177)
	at com.intellij.openapi.actionSystem.impl.ActionToolbarImpl.updateActionsImpl(ActionToolbarImpl.java:1120)
	at com.intellij.openapi.actionSystem.impl.ActionToolbarImpl.access$000(ActionToolbarImpl.java:53)
	at com.intellij.openapi.actionSystem.impl.ActionToolbarImpl$2.updateActionsImpl(ActionToolbarImpl.java:175)
	at com.intellij.openapi.actionSystem.impl.ToolbarUpdater$MyUpdateRunnable.run(ToolbarUpdater.java:186)
	...
```

### File Change Operations Do Not Refresh Change List View

**Priority: Minor** (first minor to tackle)

After file operations (add, edit, delete, move), the change list view does not refresh itself.  The user must manually
refresh the view before the changes show up.  Revert works correctly.

Looks like it could be due to pushing events off into worker threads.  The view refresh is triggered *after* the cache
updates, which should be a correct time for it.  However, even with that fix, it's still not refreshing.  Perhaps a
delay (in case the requested refresh happens too soon after the previous refresh), or a forced wait in-thread would do
the trick?

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

### Plugin Option Screen Revamp

Some options are not shown, some are not used.  These need to be cleaned up.


## Required Missing Functionality

In the 0.10 release, these pieces of old functionality are either broken or disabled.

### Ignore File

The `P4ChangeProvider` and `P4VFSListener` and `P4EditFileProvider` classes must inspect the `IgnoreFileSet` for
the corresponding .p4ignore file, to ensure that the add operation (and only add operation) acknowledges the ignore
file status.

The `P4EditAction` should respect the user's request and force the add.

### SSO

The SSO is not tested.

### Swarm Integration

Swarm integration needs to be re-instated.

### Revert Unchanged

Re-add implementation.

### Pending Action Consolidation

When a user performs an action, the internal mechanisms must first check the pending cache to see if it alters or
duplicates existing pending actions.  The pending action list must be altered to reflect the new action.

Some of this work has started.  It is handled in `PendingActionCurator`.


## Code Items Needing Attention

All the "FIXME" items in the codebase.

### ClientNamePartUI

If there's a login issue, such as a bad password, the user is not notified.
Specifically, the user isn't given an opportunity to enter the correct password.

### RemoteFileUtil

Causes the UI to show a relative path instead of the depot path.

### ActiveConnectionPanel

The tree is collapsed every time it is refreshed.

### P4LineAnnotationAspect

Implement showAffectedPaths

### P4MergeProvider

Needs implementation, and tie into P4Vcs.  If not, then delete it.

### P4IntegrateEnvironment

Needs implementation, and tie into P4Vcs.  If not, then delete it.

### P4CommittedChangesProvider

Implement getForNonLocal, createFilters for shelved files, and HAS_SHELVED.getValue.

### InvalidPasswordMonitorComponent

Better project detection.

### TopCommandRunner

A few low-level commands aren't implemented, because they aren't needed yet.

### ProjectConfigRegistryImpl

Needs to correctly dispose of removed client configs.

### P4LocalChangelistBuilder and P4LocalChangelistImpl.Builder

Choose one.

### ConnectCommandRunner

Few fiddly bits need corrections.

### SimpleConnectionManager

Some connection questions need answers.

### FileConfigPart

Need to figure out how to bundle message referenced here.

### CacheQueryHandlerImpl

Implement getCachedChangelist callback.

### P4ResolveType

Implement correct resolve types.


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

### Locally Cached Pending Changes

When a user makes a change to file (add, delete, move, edit), a cached version of the before-change should be kept.
See `com.intellij.history.integration.IdeaGateway#acquireAndUpdateActualContent()` for how the local data is preserved.
Use `VirtualFile#putUserData(custom key)` and `VirtualFile#getUserData(custom key)` to save off the data.  Note that
extreme care must be taken to properly clean up the cached data.  This means tight object lifecycle management.

### Check Description Length in Submit Dialog

Really old, long standing issue.  Can't submit with an empty description.
