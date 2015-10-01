# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.

1. Thread deadlock:
    * Changelist refresh thread (in AbstractIgnoredFilesHolder.cleanAndAdjustScope)
      waiting on ApplicationImpl.runReadAction
    * ServerConnection queue runner calling
      FileActionsServerCacheSync.innerLoadServerCache, which calls
      WorkspaceServerCacheSync.getClientRoots, which calls
      ProjectLevelVcsManagerImpl.getRootsUnderVcs, waiting on
      ApplicationImpl.runReadAction.
    * ChangelistConflictTracker waiting on ApplicationImpl.runReadAction
    * ide MergingUpdateQueue calling ApplicationImpl.runReadAction,
      which is waiting on ApplicationImpl.runReadAction
    * VcsDirtyScopeVfsListener$FileAndDirsCollector.markDirty,
      calling VcsDirtyScopeManagerImpl.filePathsDirty,
      waiting on ApplicationImpl.runReadAction
    * (The big one, all the runReadAction are waiting on)
      P4VFSListener.beforeContentsChange calling P4Vcs.getP4ServerFor
      which deeply calls P4Server.getProjectClientRoots,
      which calls ServerConnection.startImmediateAction,
      which waits on its lock.  Looks like this is wrapped in a
      WriteCommandAction.performWriteCommandAction.
      
    Looks like we need ServerConnection to obtain a readLock
    before obtaining its own lock.
1. Going offline for no apparent reason.  Reconnect attempts seem to be ignored.
   This may be fixed now.  Was probably due to the two conflicting
   APIs battling each other.
1. P4Edit isn't always editing the files.  Need to figure out what's going on.
   Looks like when offline, the queued file status isn't being recognized.




## Features that drive architecture

There are some features that drive how the inner architecture will work.

1. History, as this will dictate the necessary stored information for
   cached files and revision numbers.
   1. Need to implement `P4ContentRevision.getContent()` correctly based
      on the history implementation.
   
   
   
## Parts needing heavy testing

1. `ChangeListSync` needs to be thoroughly debugged.  It seems to be working, but it
   needs heavy testing.



## Features Needing Migration

1. `P4ChangelistListener`
1. `P4WorkOnlineAction` and the offline one.  These are the "turn them all on/off at once" actions.
1. `P4CheckinEnvironment` has remaining code in the submit method that needs to be implemented.


## Not-implemented behavior in existing features

(see `// FIXME` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include `LOG.isDebugEnabled()` calls if debug is used.
1. `AlertManager` - UI display for messages.


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.
