# All the remaining tasks for the v2 migration:


## Critical and Blocker Bugs

These bugs need to be handled before release.

1. Files open for add seem to stick around in the local cache if the push to
   the server failed. *TODO may be fixed; check*
1. IDEA 150:
```
java.lang.NoSuchMethodError: com.intellij.openapi.vcs.history.VcsHistoryUtil.showDifferencesInBackground(Lcom/intellij/openapi/project/Project;Lcom/intellij/openapi/vcs/FilePath;Lcom/intellij/openapi/vcs/history/VcsFileRevision;Lcom/intellij/openapi/vcs/history/VcsFileRevision;Z)V
    at net.groboclown.idea.p4ic.compat.idea150.HistoryCompat150$1.showDiffForOne(HistoryCompat150.java:33)
```
    *Fixed, need to check*
1. *Fixed, need to check*
```
setSelectedIndex: 0 out of bounds
java.lang.IllegalArgumentException: setSelectedIndex: 0 out of bounds
    at javax.swing.JComboBox.setSelectedIndex(JComboBox.java:620)
    at net.groboclown.idea.p4ic.ui.config.P4ConfigPanel$9.runAwtProcess(P4ConfigPanel.java:546)
```
1. *Fixed, need to check*
```
null
java.lang.AssertionError
    at net.groboclown.idea.p4ic.config.P4ConfigProject.announceBaseConfigUpdated(P4ConfigProject.java:107)
    at net.groboclown.idea.p4ic.ui.config.P4ConfigurationProjectPanel.saveSettings(P4ConfigurationProjectPanel.java:51)
    at net.groboclown.idea.p4ic.ui.config.P4ProjectConfigurable.apply(P4ProjectConfigurable.java:62)
```
1. Error dialog from config UI can report empty message.
1. Warnings just show the exception message.  The whole UI needs to be revamped.


## Big Bugs

There are multiple "todos" and "fixmes" marked in the code, but these are the bugs
that should be fixed up.

1. Change view state doesn't correctly match what's in the actual changes.
   *TODO partial fix; it requires a full "mark everything as dirty" call.
   Double check what remains to fix with this.*
1. Warnings in batch followed immediately by warnings means the old ones are removed.
1. Open multiple files for edit, with one of them (the first one?) already
   open for edit, causes none of them to be opened.
   The first file is triggered to be opened for edit, but none of the
   others are.  Looks like an IDEA bug.
1. The working online/offline icon isn't being correctly updated
   when state changes. *Fixed, needs check.*
1. Config panel can use ErrorDialog to show errors, which the user won't see until the dialog is dismissed.
   Need to find out which dialog messages cause this issue.



## Smaller Bugs


## Long term features

1. Keep backups of edited files, to allow for simulated reverts and limited diffs while in
   offline mode.  Maybe take advantage of IDEA's built-in VCS?


