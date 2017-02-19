# Refactoring of Connection To Dos:

## Critical

1. Connection UI bugs
    * Loading the config by itself (say, from an error message) does not populate the
        config settings.


## General

1. The background AWT spinner icons should have the idle icon be an empty icon, rather than null.
    It should always be visible.  This way, it takes up the correct space, and doesn't cause
    a resize to the elements around it.
    * Maybe try out `useMask(true)` instead of `setVisible(false)` ?
1. Multiple server connections are active at once, each one with its own password management.
    * Figure out why the multiples are being created.  Are they with a different ID?  Are they
        just the loaded cached servers, each trying to refresh itself?  Perhaps there really are
        three copies all active, one per connection queue?
    * Could be that multiple requests are happening before the password problem is made apparent,
        for the same connection.  May need to keep track of these connection problems and the
        user responses.  That is, if the connection had an issue
        (could not connect, invalid config, no password, etc), then that state is maintained
        until the user confirms an action to take.
        * If this is the case, then there's an issue with the alert manager error stuff
            synching with the server connection thread.
        * The state based status should be, then, added to the ServerConnection and the StatusController.
            This would augment the existing "work offline" status to include much more state.
            "waitingOnConfigUpdate", "waitingOnUserChoice", "waitingOnPassword", "offline",
            "online".  The waiting might be condensed down to a single "waitingOnUser".
            This would mean, though, a thorough check of the dialog classes to make sure they
            perform the right call to change the state.
    * Added extra logging on the error handlers to report their server ID.
1. Why isn't SSLKeyStrengthProblemHandler being used?
1. Connection UI bugs
    * ResolvedPropertiesPanel should have better control over refreshing the configuration.
        Re-examine the conditions where the properties are loaded, and ensure that the
        configuration stack is reloaded at the right points.  See the "FIXME" comment
        around this point.
    * The list of client directories, when refreshed, changes to the first entry, rather than
        staying on the previously selected one.
        - Test fix
1. Switch P4ProjectConfigComponent to use a (local class) state object.  That means including "transient"
    key words.  A little bit of this work has started.
1. P4MultipleConnectionWidget could have some nice work
    1. Add to the status bar widget a "reload" button.  Maybe also to the VCS pop-up menu and app menu.
1. Clean up "LOG.info" spots.  Remember that "LOG.debug" should be wrapped in
    "LOG.isDebug" if it does anything more than print a static string.
1. "todos" and "fixmes" marked in the code.
1. Debugging, debugging, debugging.
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
   - Test fix out.


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


## Exception

```
Cannot run synchronous submitTransactionAndWait from invokeLater. Please use asynchronous submit*Transaction. See TransactionGuard FAQ for details.
Transaction: com.intellij.openapi.options.newEditor.SettingsDialog$$Lambda$1464/594451732@31288e55
java.lang.Throwable
	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:132)
	at com.intellij.openapi.application.TransactionGuardImpl.submitTransactionAndWait(TransactionGuardImpl.java:155)
	at com.intellij.openapi.options.newEditor.SettingsDialog.show(SettingsDialog.java:77)
	at com.intellij.openapi.ui.DialogWrapper.showAndGet(DialogWrapper.java:1652)
	at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:238)
	at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:199)
	at net.groboclown.idea.p4ic.compat.idea163.UICompat163.editVcsConfiguration(UICompat163.java:37)
	at net.groboclown.idea.p4ic.v2.ui.alerts.AbstractErrorHandler.tryConfigChange(AbstractErrorHandler.java:82)
	at net.groboclown.idea.p4ic.v2.ui.alerts.InvalidRootsHandler.handleError(InvalidRootsHandler.java:70)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager$ErrorMsg.runHandlerInEDT(AlertManager.java:409)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager.handleError(AlertManager.java:299)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager.access$800(AlertManager.java:50)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager$ErrorMsg.run(AlertManager.java:400)
	at java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:301)
	at java.awt.EventQueue.dispatchEventImpl(EventQueue.java:762)
	at java.awt.EventQueue.access$500(EventQueue.java:98)
	at java.awt.EventQueue$3.run(EventQueue.java:715)
	at java.awt.EventQueue$3.run(EventQueue.java:709)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:80)
	at java.awt.EventQueue.dispatchEvent(EventQueue.java:732)
	at com.intellij.ide.IdeEventQueue.defaultDispatchEvent(IdeEventQueue.java:827)
	at com.intellij.ide.IdeEventQueue._dispatchEvent(IdeEventQueue.java:655)
	at com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.java:365)
	at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:201)
	at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:116)
	at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:105)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:93)
	at java.awt.EventDispatchThread.run(EventDispatchThread.java:82)
```
- Test fix.

## Exception

```
Cannot run synchronous submitTransactionAndWait from invokeLater. Please use asynchronous submit*Transaction. See TransactionGuard FAQ for details.
Transaction: com.intellij.openapi.options.newEditor.SettingsDialog$$Lambda$1146/177452819@d7c8766
java.lang.Throwable
	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:132)
	at com.intellij.openapi.application.TransactionGuardImpl.submitTransactionAndWait(TransactionGuardImpl.java:155)
	at com.intellij.openapi.options.newEditor.SettingsDialog.show(SettingsDialog.java:77)
	at com.intellij.openapi.ui.DialogWrapper.showAndGet(DialogWrapper.java:1652)
	at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:238)
	at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:199)
	at net.groboclown.idea.p4ic.compat.idea163.UICompat163.editVcsConfiguration(UICompat163.java:37)
	at net.groboclown.idea.p4ic.v2.ui.alerts.AbstractErrorHandler.tryConfigChange(AbstractErrorHandler.java:82)
	at net.groboclown.idea.p4ic.v2.ui.alerts.InvalidRootsHandler.handleError(InvalidRootsHandler.java:70)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager$ErrorMsg.runHandlerInEDT(AlertManager.java:409)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager.handleError(AlertManager.java:299)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager.access$800(AlertManager.java:50)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager$ErrorMsg.run(AlertManager.java:400)
	at java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:301)
	at java.awt.EventQueue.dispatchEventImpl(EventQueue.java:762)
	at java.awt.EventQueue.access$500(EventQueue.java:98)
	at java.awt.EventQueue$3.run(EventQueue.java:715)
	at java.awt.EventQueue$3.run(EventQueue.java:709)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:80)
	at java.awt.EventQueue.dispatchEvent(EventQueue.java:732)
	at com.intellij.ide.IdeEventQueue.defaultDispatchEvent(IdeEventQueue.java:827)
	at com.intellij.ide.IdeEventQueue._dispatchEvent(IdeEventQueue.java:655)
	at com.intellij.ide.IdeEventQueue.dispatchEvent(IdeEventQueue.java:365)
	at java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:201)
	at java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:116)
	at java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:105)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)
	at java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:93)
	at java.awt.EventDispatchThread.run(EventDispatchThread.java:82)
```
- Test fix.


## Error Report

```
Unsafe modality: ModalityState:com.intellij.openapi.ui.impl.DialogWrapperPeerImpl$MyDialog[dialog0,256,463,640x141,layout=java.awt.BorderLayout,APPLICAT
ION_MODAL,title=Check Connection,defaultCloseOperation=DO_NOTHING_ON_CLOSE,rootPane=com.intellij.openapi.ui.impl.DialogWrapperPeerImpl$MyDialog$DialogRo
otPane[,8,31,624x102,layout=javax.swing.JRootPane$RootLayout,alignmentX=0.0,alignmentY=0.0,border=,flags=449,maximumSize=,minimumSize=,preferredSize=],r
ootPaneCheckingEnabled=true]
java.lang.Throwable
        at com.intellij.openapi.diagnostic.Logger.error(Logger.java:132)
        at com.intellij.openapi.application.TransactionGuardImpl.submitTransactionAndWait(TransactionGuardImpl.java:155)
        at com.intellij.openapi.options.newEditor.SettingsDialog.show(SettingsDialog.java:76)
        at com.intellij.openapi.ui.DialogWrapper.showAndGet(DialogWrapper.java:1652)
        at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:238)
        at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:199)
        at net.groboclown.idea.p4ic.compat.idea163.UICompat163.editVcsConfiguration(UICompat163.java:37)
```
- Test fix.

