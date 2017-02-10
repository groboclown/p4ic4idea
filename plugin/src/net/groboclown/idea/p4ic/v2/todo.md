# Refactoring of Connection To Dos:

## Critical

1. Is the client configuration saved and loaded correctly?  It doesn't look to be
    saved.
    - It's kind of saved.  Need to double check when this happens.  workspace.xml
        contains the config values.
    - It's saved right.  However, at load time, it's being rewritten with the
        default config.  Order of default check and get state is messing things up.
1. "Perforce password (%'P4PASSWD'%) invalid or unset."
    Update checks for this error code.  Additionally, it seems that an explicitly
    stored plaintext password is not used?


## General

1. ServerRunner does not perform authentication attempt.
    - This comes from the throwing of P4LoginException, which isn't handled as "needs password".
    - Test out fix.
1. Connection UI bugs
    * It's currently using a splitter bar to separate the refresh from the connection list.
        The connection list panel should instead have a maximum size set, otherwise it will
        grow.  Switch over to another UI element.  Half/half panel split would work best.
        - Changed, but now the stack won't be restricted to its own size - it grows, rather
            than using the scroll pane correctly.
        - Maybe use a jgoodies layout, with a vertical spacer between the components?
    * Changing the config always checks the connection.  This should only be done when the
        user asks for it.
        * Turned off (commented out "refresh" in the listener of the refresh panel)
            - This causes the "refresh" button to not load the new config.  There needs to
              be a separate code here.
    * Adding a connection needs to have a mouse listener to highlight what you're going
        to select?  Something.  Because right now, it doesn't look like much.  The mouse
        listener is there, but it doesn't do anything.
        * Switch to JBPopupFactory.createActionGroupPopup
    * The connection is tested when the config is loaded.  However, if the connection
        fails, then the user gets big fat error dialogs.  These should instead be added
        to the connection problem list.  The only thing that should generate a pop-up is
        a request for a password.
        * When this is fixed, turn on the refresh panel listener for updates.
            (uncomment the listener body in the refresh panel)
    * The order is changed when the UI is loaded.
        - Test out fix
    * Client name property field doesn't seem to work right.  It incorrectly recognizes
        a single config as multiple directories.
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


## Exception

This one is the really big blocker.

```
Caused by: com.perforce.p4java.exception.AccessException: Access for user 'nouser' has not been enabled by '%'p4 protect'%'.
        at net.groboclown.idea.p4ic.v2.server.connection.AuthenticatedServer.remakeException(AuthenticatedServer.java:505)
        at net.groboclown.idea.p4ic.v2.server.connection.AuthenticatedServer.checkoutServer(AuthenticatedServer.java:123)
        at net.groboclown.idea.p4ic.v2.server.connection.ClientExec$1.run(ClientExec.java:148)
        at net.groboclown.idea.p4ic.v2.server.connection.ServerRunner.p4RunWithSkippedPasswordCheck(ServerRunner.java:209)
        ... 39 more
Caused by: com.perforce.p4java.exception.AuthenticationFailedException: Access for user 'nouser' has not been enabled by '%'p4 protect'%'.
        at com.perforce.p4java.impl.mapbased.server.Server.createExceptionFromMessage(Server.java:4057)
        at com.perforce.p4java.impl.mapbased.server.Server.handleErrorStr(Server.java:4920)
        at com.perforce.p4java.impl.mapbased.server.Server.login(Server.java:650)
        at net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator$2.exec(ServerAuthenticator.java:259)
        at net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator$2.exec(ServerAuthenticator.java:249)
        at net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator.runExec(ServerAuthenticator.java:337)
        at net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator.authenticate(ServerAuthenticator.java:249)
        at net.groboclown.idea.p4ic.v2.server.authentication.ServerAuthenticator.authenticate(ServerAuthenticator.java:200)
        at net.groboclown.idea.p4ic.v2.server.connection.AuthenticatedServer$1.with(AuthenticatedServer.java:276)
        at net.groboclown.idea.p4ic.v2.server.connection.AuthenticatedServer$1.with(AuthenticatedServer.java:271)
        at net.groboclown.idea.p4ic.compat.auth.OneUseString.use(OneUseString.java:48)
```


## Exception

```
Cannot run synchronous submitTransactionAndWait from invokeLater. Please use asynchronous submit*Transaction. See TransactionGuard FAQ for details.
Transaction: com.intellij.openapi.options.newEditor.SettingsDialog$$Lambda$1422/688691600@66cc04f6
java.lang.Throwable
	at com.intellij.openapi.diagnostic.Logger.error(Logger.java:132)
	at com.intellij.openapi.application.TransactionGuardImpl.submitTransactionAndWait(TransactionGuardImpl.java:155)
	at com.intellij.openapi.options.newEditor.SettingsDialog.show(SettingsDialog.java:76)
	at com.intellij.openapi.ui.DialogWrapper.showAndGet(DialogWrapper.java:1652)
	at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:238)
	at com.intellij.ide.actions.ShowSettingsUtilImpl.editConfigurable(ShowSettingsUtilImpl.java:199)
	at net.groboclown.idea.p4ic.compat.idea163.UICompat163.editVcsConfiguration(UICompat163.java:37)
	at net.groboclown.idea.p4ic.v2.ui.alerts.AbstractErrorHandler.tryConfigChange(AbstractErrorHandler.java:82)
	at net.groboclown.idea.p4ic.v2.ui.alerts.InvalidRootsHandler.handleError(InvalidRootsHandler.java:70)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager$ErrorMsg.runHandlerInEDT(AlertManager.java:409)
	at net.groboclown.idea.p4ic.v2.server.connection.AlertManager.handleError(AlertManager.java:299)
```


## Exception
    
```
INFO - idea.p4ic.config.ConfigProblem - ConfigProblem from null
net.groboclown.idea.p4ic.server.exceptions.P4InvalidClientException: Invalid Perforce client: prodsc.austx.zilliant.com:1666
        at net.groboclown.idea.p4ic.v2.server.cache.CentralCacheManager.getClientCacheManager(CentralCacheManager.java:143)
        at net.groboclown.idea.p4ic.v2.server.connection.ServerConnectionManager$ServerConfigStatus.getConnectionFor(ServerConnectionManager.java:342)
        at net.groboclown.idea.p4ic.v2.server.connection.ServerConnectionManager.getConnectionFor(ServerConnectionManager.java:150)
        at net.groboclown.idea.p4ic.v2.server.connection.ConnectionUIConfiguration.checkConnection(ConnectionUIConfiguration.java:48)
        at net.groboclown.idea.p4ic.ui.config.ResolvedPropertiesPanel$4.runBackgroundProcess(ResolvedPropertiesPanel.java:144)
        at net.groboclown.idea.p4ic.ui.config.ResolvedPropertiesPanel$4.runBackgroundProcess(ResolvedPropertiesPanel.java:112)
        at net.groboclown.idea.p4ic.background.BackgroundAwtActionRunner$1.run(BackgroundAwtActionRunner.java:79)
        at com.intellij.openapi.application.impl.ApplicationImpl$2.run(ApplicationImpl.java:330)
        at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
        at java.util.concurrent.FutureTask.run(FutureTask.java:266)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at java.lang.Thread.run(Thread.java:745)
```


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


## Exception

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
