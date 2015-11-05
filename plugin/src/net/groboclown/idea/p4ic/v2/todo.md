# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before features.

1. Move a file: it is not marked as move, but as add; the source delete is lost.


## Big Bugs

There are multiple "todos" and "fixmes" marked in the code, but these are the bugs
that should be fixed up.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.
1. When project root is at (say) c:\a\b\c\, and .p4config exists in c:\a\b\c\ and c:\a, the
   c:\a is picked up.
1. The working online/offline icon isn't being correctly updated
   when state changes. *Fixed, needs check.*
1. error (idea):
    ```
    ERROR - ellij.ide.impl.DataManagerImpl - cannot share data context between Swing events; initial event count = 142537; current event count = 142559 
    java.lang.Throwable
        at com.intellij.openapi.diagnostic.Logger.error(Logger.java:115)
        at com.intellij.ide.impl.DataManagerImpl$MyDataContext.getData(DataManagerImpl.java:357)
        at com.intellij.openapi.actionSystem.DataKey.getData(DataKey.java:75)
        at com.intellij.openapi.actionSystem.AnActionEvent.getData(AnActionEvent.java:165)
        at com.intellij.openapi.vcs.changes.actions.RollbackAction.getChanges(RollbackAction.java:148)
        at com.intellij.openapi.vcs.changes.actions.RollbackAction.actionPerformed(RollbackAction.java:120)
    ```
1. error (idea):
    ```
    ERROR - ellij.ide.impl.DataManagerImpl - cannot share data context between Swing events; initial event count = 142537; current event count = 142559 
    java.lang.Throwable
        at com.intellij.openapi.diagnostic.Logger.error(Logger.java:115)
        at com.intellij.ide.impl.DataManagerImpl$MyDataContext.getData(DataManagerImpl.java:357)
        at com.intellij.openapi.actionSystem.DataKey.getData(DataKey.java:75)
        at com.intellij.openapi.actionSystem.AnActionEvent.getData(AnActionEvent.java:165)
        at com.intellij.openapi.vcs.changes.actions.RollbackAction.getModifiedWithoutEditing(RollbackAction.java:180)
        at com.intellij.openapi.vcs.changes.actions.RollbackAction.actionPerformed(RollbackAction.java:122)    
    ```


## Not-implemented behavior in existing features

(see `// FIXME` comments)

1. Remove the excessive logging.  Move down to debug if necessary,
   and include `LOG.isDebugEnabled()` calls if debug is used.
1. `ClientCacheManager`


## Smaller Bugs

1. Replace "IntelliJ" and "IDEA" from the strings with the actual IDE name.
   Text display should fetch the name of the IDE, and not hard-code "IntelliJ" or
    "IDEA".

## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?

