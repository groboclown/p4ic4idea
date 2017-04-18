# Refactoring of Connection To Dos:


## General

1. The background AWT spinner icons should have the idle icon be an empty icon, rather than null.
    It should always be visible.  This way, it takes up the correct space, and doesn't cause
    a resize to the elements around it.
    * Maybe try out `useMask(true)` instead of `setVisible(false)` ?
    * `JBList` has a spinner that could be used (`setPaintBusy`), for when it's loading.
1. Switch P4ProjectConfigComponent to use a (local class) state object.  That means including "transient"
    key words.  A little bit of this work has started.
1. P4MultipleConnectionWidget could have some nice work
    1. Add to the status bar widget a "reload" button.  Maybe also to the VCS pop-up menu and app menu.
1. "todos" and "fixmes" marked in the code.
1. There might be more "files stuck in cached state", but I haven't found more.
1. Moved files should be grouped together:
    1. move between changelist: one moves, then the other should also move.
    1. revert: one is reverted, the other is also reverted.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.


## Exception

- This can happen if the login fails, causing the workspace roots to be empty.

```
INFO - server.connection.AlertManager - Critical error
com.intellij.openapi.vcs.VcsException: no valid roots
        at net.groboclown.idea.p4ic.v2.server.cache.sync.WorkspaceServerCacheSync.getClientRoots(WorkspaceServerCacheSync.java:240)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.ClientCacheManager$CacheImpl.getClientRoots(ClientCacheManager.java:355)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.FileActionsServerCacheSync.getClientRootSpecs(FileActionsServerCacheSync.java:763)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.FileActionsServerCacheSync.innerLoadServerCache(FileActionsServerCacheSync.java:111)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.CacheFrontEnd.loadServerCache(CacheFrontEnd.java:67)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.CacheFrontEnd.access$000(CacheFrontEnd.java:34)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.CacheFrontEnd$1.query(CacheFrontEnd.java:52)
        at net.groboclown.idea.p4ic.v2.server.cache.sync.CacheFrontEnd$1.query(CacheFrontEnd.java:43)
        at net.groboclown.idea.p4ic.v2.server.connection.ServerConnection$4.perform(ServerConnection.java:248)
        at net.groboclown.idea.p4ic.v2.server.connection.Synchronizer$ServerSynchronizer$ConnectionSynchronizer.runImmediateAction(Synchronizer.java:188)
        at net.groboclown.idea.p4ic.v2.server.connection.ServerConnection.query(ServerConnection.java:242)
        at net.groboclown.idea.p4ic.v2.server.P4Server$4.query(P4Server.java:413)
        at net.groboclown.idea.p4ic.v2.server.P4Server$4.query(P4Server.java:405)
        at net.groboclown.idea.p4ic.v2.server.connection.ServerConnection.cacheQuery(ServerConnection.java:354)
        at net.groboclown.idea.p4ic.v2.server.P4Server.getOpenFiles(P4Server.java:405)
        at net.groboclown.idea.p4ic.v2.changes.P4ChangeProvider$MappedOpenFiles.<init>(P4ChangeProvider.java:427)
        at net.groboclown.idea.p4ic.v2.changes.P4ChangeProvider.getOpenedFiles(P4ChangeProvider.java:282)
        at net.groboclown.idea.p4ic.v2.changes.P4ChangeProvider.syncChanges(P4ChangeProvider.java:152)
        at net.groboclown.idea.p4ic.v2.changes.P4ChangeProvider.getChanges(P4ChangeProvider.java:142)
        at com.intellij.openapi.vcs.changes.ChangeListManagerImpl.actualUpdate(ChangeListManagerImpl.java:680)
        at com.intellij.openapi.vcs.changes.ChangeListManagerImpl.iterateScopes(ChangeListManagerImpl.java:594)
        at com.intellij.openapi.vcs.changes.ChangeListManagerImpl.lambda$updateImmediately$6(ChangeListManagerImpl.java:487)
        at com.intellij.openapi.progress.impl.CoreProgressManager$3.run(CoreProgressManager.java:181)
        at com.intellij.openapi.progress.impl.CoreProgressManager.registerIndicatorAndRun(CoreProgressManager.java:587)
        at com.intellij.openapi.progress.impl.CoreProgressManager.executeProcessUnderProgress(CoreProgressManager.java:532)
        at com.intellij.openapi.progress.impl.ProgressManagerImpl.executeProcessUnderProgress(ProgressManagerImpl.java:66)
        at com.intellij.openapi.progress.impl.CoreProgressManager.runProcess(CoreProgressManager.java:166)
        at com.intellij.openapi.vcs.changes.ChangeListManagerImpl.updateImmediately(ChangeListManagerImpl.java:487)
        at com.intellij.openapi.vcs.changes.ChangeListManagerImpl.access$500(ChangeListManagerImpl.java:68)
        at com.intellij.openapi.vcs.changes.ChangeListManagerImpl$ActualUpdater.run(ChangeListManagerImpl.java:397)
        at com.intellij.openapi.vcs.changes.UpdateRequestsQueue$MyRunnable.run(UpdateRequestsQueue.java:269)
        at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
        at java.util.concurrent.FutureTask.run(FutureTask.java:266)
        at com.intellij.util.concurrency.SchedulingWrapper$MyScheduledFutureTask.run(SchedulingWrapper.java:237)
        at com.intellij.util.concurrency.BoundedTaskExecutor$2.run(BoundedTaskExecutor.java:212)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at java.lang.Thread.run(Thread.java:745)
```

