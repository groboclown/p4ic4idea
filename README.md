# IDEA Community VCS Integration for Perforce

This is the location for the Perforce VCS integration into the IntelliJ IDEA Community Edition IDE.

**Currently Supported IDEA versions: 14.2**



# Status and Todo


## Functional Areas and their Problems


### Audit Code and Refactoring

* Investigate all remaining "FIXME" and "TODO" items
* Reduce the amount of LOG.info usage.


### Connection Widget

* The "did connect" message is sent too early, which means the
  widget still shows "disconnected" even when it's running in
  connected mode.  Clicking on the widget refreshes the
  status correctly.
   **Minor**


### Submit

* Submit is currently disabled, because it crashes Windows P4 servers.
   *Major Feature*


### Connection Settings

* Add in Trust Ticket to connection gui.
   **Minor**
* Selecting "Relative P4CONFIG" should ensure the client load button is really
  disabled.  It currently only sometimes works.
   **Minor**


### Diff

* The changelist-based "Ctrl-D" diff shows a difference against *head* revision, not *have*.
  This should probably be an option.


### Revert

* Reverting a moved file requires a project file refresh to
  see it moved into the correct location.
   **Minor**
* Has special code to handle the moved/renamed file pairs
  (reverting one end of the move/rename reverts the other
  one, too).


### Changelist

* A file which is edited but not checked out is not processed
  correctly.  It seems to always become checked out.
   **Minor**


### Move a file between changelists

* Move/add and move/delete files are both put into the moved changelist
  when only one is moved.  However, this is only reflected in the UI when the
  changelist view is refreshed (the changelist view only shows one file moved).
   **Minor**


### Add/Edit

* If a file was edited without telling VCS (edit during offline mode, etc),
  then the server is reconnected and the file is added (Alt+Shift+A),
  the file is not reflected in the correct changelist.  A refresh is needed
  to show it.  The file cannot be reverted until the refresh happens.
   **Minor**


### Rename / Move

* Moving a file will correctly show the old/new files in the changelist, but
  it can also incorrectly mark the directory and other files as "Modified without checkout".
  Refreshing the changelist fixes this, but adding that to the code (P4VCSListener) seems to
  invoke it too early; there's something triggering the dirty flag after the move listener
  fires.
   **Minor**
* Need to test out moving a file that has already been:
   * edited - has a problem; keeps original file open for edit.
        p4v works by opening the file for edit (even if it's already open for edit),
        then performing the move operation.
   * moved - p4v works by opening the initially renamed file for edit, then
        running the move on the initial renamed file to the new name.
   * added

   P4V seems to do the exact same procedure for all of these operations -
   open for edit then move.

   P4V reports an error if you move a file to a file that already exists with
   the same name.

   P4V reports an error if you delete a file, then move a file to be the same
   name as the just-deleted file.  You need to submit the delete first.

   Fortunately, IDEA keeps the deleted file in the changelist.


### Copy

* Need to retest these scenarios:
  * target is deleted on depot, not on client
  * target is open for edit or add
* Copying a file that is currently opened for integrate or move will just
  add a new file, and not make it be integrated from the underlying source.
  *This only matters if the `use integration` flag is set, which currently can't be set.*
   Once `use integration` flag is enabled, this will be a **Minor** issue.


### Errors

* Errors need to be localized
   **Minor**


### VCS Popup

* Works fine.


### History

* Seems to work fine.


### Delete

* Delete seems to work.


### Synchronize a file

* Seems to work fine.


## Improvements

### General

* Proper P4IGNORE support.  The VCS itself provides some level of API support for this, but it will need to be
  integrated deeper into the calls.


### Checkout

* "Checkout" provider is essentially the sync in Perforce terms.  Implement this.
* Add sync (checkout) to popup menu.


### UI Changelists

* Add Job add/remove components to the changelist modify dialog.
* On submit, allow for setting jobs and job status.  See TodoCheckinHandler for
  how to get UI elements in the checkin handler.  Although,
  P4OnCheckinPanel looks to be where it should be.
* Look at the com.intellij.openapi.vcs.changes.ui.NewChangelistDialog class to see where
  the new changelist dialog is instantiated.  Doesn't look like it can be extended
  to add new options.  May need to just replace the corresponding action
  (com.intellij.openapi.vcs.changes.actions.AddChangeListAction), which is
  defined in the VcsActions.xml file with id "ChangesView.NewChangeList".
  Replace the whole "ChangesViewToolbar" group?


### UI File Icon

* Mark files in the project folder with perforce status - does it need
  resolving?  Is it out of date?  Text color already shows the general
  add/edit/move status.
   * StructureViewTreeElement.  The Vcs apparently plugs into this to show
     the lock icon.
   * The P4StatusUpdateEnvironment looks like it should be handling this,
     but it never seems to be invoked.


### History

* Add full getVcsBlockHistoryProvider impls.
* P4CommittedChangesProvider.java needs implemented methods.
  

### Connection Settings

* Look into handling the P4ENVIRO file.
* Support SSO


### Integrate on Copy

* There's a flag that allows for selecting whether the user wants to
  perform an integrate on copy or just an add.  Right now, this is
  hard coded to "add", but can be selectable in the future.  This
  might be an option that can be added in the copy dialog.


### Hacks

* P4CheckinEnvironment isn't correctly being returned by
  P4Vcs.getCheckinEnvironment.
  A hack is in place to make it work, but it needs a proper fix.
* Same thing with P4RollbackEnvironment.
* BackgroundTask should reference the VcsConfiguration to determine which task
   is run in the background.
* Support for files with wildcard characters (#@%\*) works, but is kind of
   messy.  It's currently contained in FileSpecUtil, and P4FileInfo, and
   a bit of really awful functionality in P4Exec.addFiles.


### Tests

* Add unit tests


# Code Style

## Exceptions

In general, only `VcsException` and `CancellableException` should be thrown.
The only place the deeper `P4JavaException` and family are thrown is in the
`P4Exec` class, and that's only when wrapped in the p4RunFor call.


## FilePath

`FilePath.getPath()` usually returns the value for the contained `VirtualFile`; however, for
a FilePath that references the source of a moved file (deleted), it will return the
moved-to destination.  Looks like an IDEA bug.  To avoid this, I add this to
all such calls:

// Warning: for deleted files, fp.getPath() can be different than the actual file!!!!
// use this instead: getIOFile().getAbsolutePath()

Also, try to block directories from going into Perforce file specs.  The only place
the code works with directories is finding open files. 


## Threading

Without special care, deadlocks are common.  Never call "invokeAndWait"
from the EDT thread.  On the rarest occasion that you need a result
from a user, and the code can be run from the EDT or a background thread,
then use a VcsFutureSetter and its "runInEdt" call to make sure the
right, non-deadlock code is invoked.

Also, any bit of code that is synchronized cannot call out to the EDT,
unless it is carefully placed in a background thread.
This means no invoking P4 calls in a synchronized block unless it's in
a background thread.  Be careful here!  There are some parts of the
`VcsUtil` that obtain a vcs write lock that can block.


## Connection Status

Connection status must be carefully handled to avoid deadlock problems.

Status changes on connection (connected or disconnected) are run through the MessageBus.


## Handling FileSpecs

Do not use the P4 Java API class `FileSpecBuilder`.  Instead, use the `P4FileInfo`
where possible, and when needing to get deep into the Perforce interaction, use the
`FileSpecUtil`.  The *ONLY* exception is when dealing with "p4 add", because that
has special usage.


# P4Java API bugs

* Creating a new changelist, or fetching a single changelist, both
  incorrectly reload the Changelist object from the map iterator
  when the map is empty, rather than on the first pass when it actually
  contains data.  See P4Exec for the work-around.
* p4 submit -i crashes Windows servers, most likely because the incoming
  submit map is wrong.  Although I believe this also happens when this is
  changed to pass everything as arguments rather than the input map.
  *Need to retest now that the P4Exec is under better control, but it
  should still be re-implemented to better conform to the p4d requirements.*
* Looks like passing the password in the property doesn't work right.
  It still needs a "login", but at least we can not store the login
  in the ticket file.
* Loading the file contents can return extra information regarding the
  Perforce server performance running the operation.  -In order to combat this,
  the command performs an extra step to read the extended file status
  to pull in the actual file size, then trim down the returned file
  to the actual size.-  The file is scanned backwards to find the
  start of the stats (size doesn't work due to server stored size vs.
  client download size differences; think -k and line ending changes).
  his needs to be monitored to make sure there isn't too much being trimmed.
* p4java API can flood the temp directory with "p4j\*.tmp" files.
  The plugin has a watch-dog thread that cleans these up
  periodically.
