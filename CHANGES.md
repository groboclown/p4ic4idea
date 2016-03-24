# IDEA Community VCS Integration for Perforce

## ::v0.7.12::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * More fixes for the authentication prompting problem. (#109)


## ::v0.7.11::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Alleviated some of the issues surrounding the plugin
      constantly reporting that the password is invalid.
      (#109)


## ::v0.7.10::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Android Studio and other non-JRE 8 IDEs can fail to load the
      plugin.  Now, all the class loading errors should be caught
      when loading the compatibility libraries. (#113 & #110)


## ::v0.7.9::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Android Studio and other non-JRE 8 IDEs can fail to load the
      plugin.  Now, all the class loading errors should be caught
      when loading the compatibility libraries. (#113)


## ::v0.7.8::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Initial project VCS setup causes "OK" and "Apply"
      buttons to not react. (#111)
    * Initial project loading in Android Studio 1.5.1 with a master password
      store causes the project to deadlock on startup. (#110 and #112)
    * Further progress on fixing the false password authentication
      reporting issues (#109 and #108).


## ::v0.7.7::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Fixed an issue where the user may be
      told repeatedly that their password is wrong,
      even though it's correct. (#105, #107)


## ::v0.7.6::

### Overview

* Switched to using MD5 hash codes when comparing if a file is actually
  edited but not checked out.
* Added user preference to disable server-side file comparison when checking
  for edited but not checked out status.
* Added user preference for switching between showing revision numbers and
  changelist numbers.
* Bug fixes.

### Details

* Switched to using MD5 hash codes when comparing if a file is actually
  edited but not checked out.
    * Before, when a file was noted by IDEA as being edited, and
      the file was not open for edit, the plugin would call out
      to the server to verify that it was in fact not changed.
      This behavior has been altered to instead cache the server's
      HAVE version's MD5, and compares that instead against the
      local file.
* Added user preference to disable server-side file comparison when checking
  for edited but not checked out status.
    * This may be less necessary now that the MD5 comparison is in place,
      but for Perforce depots that have very large files, or where the IDE
      marks many things as edited, it could still be a major performance
      drag.  This checkbox will turn that functionality off.
    * Note that with this functionality off, you may see files incorrectly
      marked as "changed but not open for edit". 
* Added user preference for switching between showing revision numbers and
  changelist numbers.
    * Changes which numbers are shown in annotations and history.
* Bug fixes.
    * Fixed the build so that it will correctly check the primary
      plugin against all supported IDEA versions, rather than just
      the earliest.
    * Should now always authenticate once the server reports that the
      user requires authentication. (#95)  This should also fix the
      issue where the plugin can continuously ask for the password;
      this could happen if you use the configuration file without
      specifying an authentication ticket or a password.
    * Fixed a deadlock associated with initial startup when asking the
      user for the master IDEA password.
    * Fixed submit issue where only the error would be reported;
      now, all messages are reported to the user.
    * Fixed ignored tick marks (') in messages.
    * Fixed compilation errors against the latest IDEA version of the
      primary plugin.  This may result in you needing to reload your
      Perforce configuration.
    * Updated the project to enable better plugin debugging.


## ::v0.7.5::

### Overview

* Switched to JDK 1.8 for compiling in order to support IDEA 16.
* Allow for pushing the open-for-edit on keystroke to operate
  in a background thread (#99), as a **User Preference** option.
* Bug fixes.

### Details

* Switched to JDK 1.8 for compiling in order to support IDEA 16.
    Only future API changes in IDEA will be compiled with JDK 1.8,
    so that older IDE versions will continue to work. (#92)
* Allow for pushing the open-for-edit on keystroke to be pushed
    out to a background thread.  Some users were encountering
    performance issues when opening a file for edit (#99),
    and this new option will allow for moving as much of that
    processing to away from the event thread.  The option is
    in the **User Preferences** of the Perforce VCS panel, titled
    "Open for edit in background."
* Bug fixes.
    * Trimmed whitespace on job IDs in the submit dialog (#96).
    * Annotation set before / after popup now shows correct list of
      revisions.
    * Removed the obsolete user preference for setting the maximum
      number of concurrent connections.  This is now limited to one
      per workspace.
    * Fixed an IllegalStateException that could occur when refreshing
      the changelist view, when the P4 changelist was submitted or
      deleted on the server.


## ::v0.7.4::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Changelist refresh now cleans out the cached data if the user is online and
      there are no pending updates to send to the server.  This should prevent the
      "old data" syndrome that would sometimes creep in, and it should make
      the synchronize file action work as expected.  (#87 and #90)
    * "Silently go offline on disconnect" option causes the gone offline
      dialog to repeatedly show up on disconnect.  (#83)


## ::v0.7.3::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Annotated lines would show the most recent revision to have the line, rather than
      when it first appeared. (#86)
    * Changelist details display for an annotated line would use wrong changelist number
      from the file status. (#86)
    * Clicking on the annotated line could generate a "changelist -1" error. (#86)


## ::v0.7.2::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * P4ProjectConfigurable can save settings without reading config sources. (#82)
    * On startup, workspaces do not load root paths if password stored in IDEA. (#81)
    * Changing the Perforce workspace root and moving the project directory to
      the new location no longer reports a directory mismatch. (#84) 


## ::v0.7.1::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * In some circumstances, the error dialog was only closing when "Cancel" was selected.
    * Some circumstances cause the user to not be prompted for the password. (#80)
    * When the user selected to not store the password, it wouldn't be used.
    * Added the underlying problem to the error report in the warnings panel.


## ::v0.7.0::

### Overview

* Major overhaul to the server connection logic.
* Multiple server connection status in status bar widget.
* Passwords are now stored with better security.
* P4IGNORE support.
* Version selection for synchronize with depot.
* Bug fixes.

### Details

* Major changes to the server connection logic. (#70)
    * Allows for limited offline work.  Edit, add, delete, and move operations are
      cached for replay after you reconnect to the server.  Reverts only
      have limited support.
    * Warnings are displayed in the messages docked view.
    * Errors are now more consistently displayed and handled.
    * Note that changes to the client workspace spec will cause cached updates to be lost
      (the plugin won't be able to figure out how the files now map to the server).
* Multiple server connection status in status bar widget. (#19)
    * The online state can now be controlled per-server through
      the connection status widget.
* Passwords are now stored with better security.
    * Uses the IntelliJ password storage, so you may be prompted for a
      master password now if you setup the IDE to use that.
    * This eliminates the "persist password locally" setting.
* P4IGNORE support (#69)
    * Support the `P4IGNORE` environment variable.  The default filename is
      `.p4ignore`
    * Only files that are not known by the server are ignored.
* Version selection for synchronize with depot. (#41)
    * When "Update Directory" or "Update File" is run from the P4 context menu,
      you are given the option to select which revision or changelist to
      sync to.
* Bug fixes.
    * The configuration UI could make UI requests outside the AWT event dispatch
      thread.
    * Finally identified the sources of the infinite changelist refresh issues
      and fixed it, so that new improvements to the changelist sorting code can
      be attempted.
    * Internal communication of events are now handled in the correct
      message bus, which should lead to more consistent responses to
      going offline or changing configuration.
    * Add trusted ticket file support (#4)
    * Changelist sync can discover null Idea changelists (#64)
    * Initial creation of file asks if it should be added to perforce, but it is not added (#66)
    * Files loaded from the Perforce server are restricted to the user-defined
      maximum file size (the `idea.max.vcs.loaded.size.kb` property).
    * Falsely reporting that JCE is not present (#72)
    * First time project configuration does not detect UI values (#65)
    * Copy/paste and cut/paste of files do not trigger P4 actions (#71)
    * "Move changes" action from non-active changelist modification action moves back into
      original changelist (#68) 
    * History difference did not work on IDEA 15.0 due to an IDEA API change.
    * Show changelist number in file history view (#76)
    * Abort a submit if the checkin comment is empty. (#52)
    

## ::v0.6.6.1::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Fixed an infinite changelist refresh due to incorrect file mappings.


## ::v0.6.6::

### Overview

* Added user preference (not available through UI yet) that allows selecting whether a copy
  command means an integrate.
* Started work on adding in the rollback on history view.
* Bug fixes.

### Details

* Added user preference (not available through UI yet) that allows selecting whether a copy
  command means an integrate.
    * UI will be available by v0.7.
* Bug fixes.
    * Move operations could not find the Perforce version of the locally moved file (#62).
      This also affected the copy operation.
    * Rewrote how the IDEA changelists sync up with the Perforce changelists.  It should now
      be more accurate and eliminate some of the null mappings that occurred before.


## ::v0.6.5::

### Overview

* Added new 'P4HOST' setting.
* Bug fixes.

### Details

* Added new 'P4HOST' setting (#61).
    * It overrides the JVM discovery of the client hostname.
    * It will be read from the environment settings, and from the P4CONFIG
      file (it cannot be altered if you use the "Client Password"
      or "Authorization Ticket" connection methods).
* Bug fixes.
    * If the JVM reports the hostname as including the full domain name, then
      it will be stripped down to just the hostname (#61).
    * Fixed an IDEA 15 API incompatibility issue with file refreshing.
      The `VcsFileUtil.refreshFiles(Project, Collection<VirtualFile>)` is no
      longer in the API.



## ::v0.6.4::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * Configuration tabs need scroll pane (#59)
    * Changelists can show a single file in multiple changelists (#58)
    * Removed a stack overflow error with the server fingerprint.
    * Switched requests for services to requests for components.


## ::v0.6.3::

### Overview

* Switched to open source P4Java library.
* Uses SSL fingerprint when connecting to SSL server (#56).
* Identified when a connection fails due to an SSL server fingerprint mismatch (#56).
* Bug fixes.

### Details

* Switched to open source P4Java library.
* Uses SSL fingerprint when connecting to SSL server (#56).
    * Authorization ticket and password entry connection methods can specify
      the trusted SSL fingerprint for the server.  Config file connection
      methods will use the `P4TRUST` setting, or the default p4 trust file.
      you will need to trust the server using the `p4 trust` command line
      in order to manage the `P4TRUST` file's contents.
* Identified when a connection fails due to an SSL server fingerprint mismatch.
    * Problems due to a trust issue with the server are now clearly identified.
* Bug fixes.
    * On connection check using a relative config file, you will no longer see
        the "connection is fine" message after seeing errors with the connection (#57).



## ::v0.6.2::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    * If the user's P4CONFIG environment variable or windows registry setting is set,
        it can still be loaded, even if the user doesn't want to use those settings.
        This isn't a problem unless a relative path is used, in which case a strange
        "FileNotFoundException" will be generated.
    * The default changelist will show up as the synthetic text font (by default, blue),
        rather than looking like the active changelist color.  This avoids confusion
        if the active changelist is not the default changelist.
    * Relative configs could still report problems on the "check config" button.


## ::v0.6.1::

### Overview

* Add user preferences.
* Begin SSL support.  Requires installing the unlimited strength JCE package for the IDE's JRE
    in order to use.
* Many bug fixes.

### Details

* Add user preferences (#39)
    * User preferences are available on the Perforce configuration
      panel, in a new tab.  The current options are for setting the maximum
      number of open connections to each server, and the maximum timeout
      for waiting on those connections.
* Begin SSL support.
    * Standard Java does not support the 256-bit encryption level ciphers required by the
      Perforce server.  You must download and install the unlimited strength JCE package
      for the IDE's JRE in order to connect to an SSL-enabled Perforce server.
    * Added support for specifying the Perforce server SSL fingerprint.
    * Start of trust support for server connections.  Still needs correct
      wiring to allow the user to be notified of fingerprint changes, and
      for correct authenticating of the fingerprints.  It will reuse
      the SSL trust ticket if the user has already authenticated (with "p4 trust")
      the connection.
* Bug fixes.
    * Client selection drop-down is disabled (#53)
    * NPE in Authorization Ticket connection when selecting the file chooser (#54)
    * Bad display of P4PORT text field tooltip.
    * Fixed error if the client is disconnected while refreshing the changelist view.
    * Fixed error when choosing "Specific P4CONFIG file" related to incorrect widget
      initialization.
    * De-register VCS module on project close (#55)



## ::v0.6.0::

### Overview

* Allow selecting the revision or changelist to sync to.
* Synchronizing on a checked-out file does not report future merge problem.
* Bug fixes.
* Code cleanup.

### Details

* Allow selecting the revision or changelist to sync to (#29).
    * Now the menu option "Update files..." brings up a dialog that allows the
      user to select which revision to sync to (head, revision `#` value,
      changelist etc. `@` value), as well as whether to force the sync.
* Synchronizing on a checked-out file does not report future merge problem (#30).
    * If a synchronize will cause a future merge issue, you will now see an
      error reported with the details.  The sync will still happen.
* Bug fixes.
    * Add/Edit without adding to Perforce incorrectly then adds the file to Perforce (#6).
    * Changelists show files that are unchanged as locally modified without checkout (#49).
    * Fixed NPE that can happen if synchronizing files causes a merge issue.
* Code cleanup.
    * Reduced logging.


## ::v0.5.5::

### Overview

* Full file history support (#47).
* Bug fixes.

### Details

* Full file history support (#47).
    * File history used to not show branching history if it went outside the current client.  Now, it will show
      a full history of the file, and allow for diffing against any part of the depot in which the user has
      read access.
* Bug fixes.
    * Fixed error when handling deletion of files not owned by Perforce.
    * Fixed a lag issue related to saving files when working offline.
    * Minor code cleanups.


## ::v0.5.4::

### Overview

* Bug fixes.

### Details

* Bug fixes.
    *  Changelists refresh event may be incorrectly skipped (#45)
       This fix keeps the timeout, but changes it to a cache expiration concept.
       IntelliJ can call the changelist refresh multiple times very quickly, so
       this addition will maintain a short-lived cache that will be reused as
       long as IntelliJ indicates that no IDE-managed files need updates.
       When the cache expires (currently set at 10 seconds), the server
       will be queried at the next refresh time.



## ::v0.5.3::

### Overview

* Added "Update files" to file context menu to allow easy synchronization with depot.
* Added "Revert unchanged files" to file context menu and changelist context menu.
* Bug fixes.

### Details

* Added "Update files" to file context menu to allow easy synchronization with depot.
    * Right click on the Project view file or directory, select the **P4** sub-menu,
      and the **Update files...** option will synchronize to the head revision.
* Added "Revert unchanged files" to file context menu and changelist context menu.
    * Right click on a changelist in the **Version Control** view, and select "Revert unchanged".
      All files in the changelist, for all clients, will be reverted if they have not
      been altered from the depot version.
    * Right click on selected files in the project view, and select "Revert unchanged".
      All files in the changelist, for all clients, will be reverted if they have not
      been altered from the depot version.
* Bug fixes.
    * Improved support for rollback of files that aren't explicitly checked out by Perforce.
    * Add missing tooltips on UI elements (#43)



## ::v0.5.2::

### Overview

* Reduced the number of server calls.
* Bug fixes.

### Details

* Reduced the number of server calls. (#38)
    * Cached the files open for edit on Perforce.  This list is reloaded on "version control" panel refresh,
      and when the files are considered out of sync.
    * Cached the jobs loaded from the server.
    * Added a monitor to track the number of actual server calls.
    * Capped the number of simultaneous connections to the same server/workspace (2).
    * Capped the frequency at which changelists will be refreshed (once every two seconds).
      This is due to editing files triggering a changelist refresh, which can become quite noisy.
* Bug fixes.
    * Reduced the amount of log messages.
    * Eliminated NPE related to changelist caching.


## ::v0.5.1::

### Overview

* Internationalized the exception messages.
* Bug fixes

### Details

* Internationalized the exception messages.  Exceptions can be
    reported to the user, and so they need I18N support.
* Bug fixes
    * Not finding P4CONFIG in the hierarchy (#32)
    * Partial fix for incorrect parsing of P4Java API issue with fetching jobs (#33).
      This appears to be a server error during the returning of the job.
      An error is still reported to the user, but it is friendlier, and better
      details are logged.
    * Config "resolved values" should better reflect if it has been loaded (#34)
    * Reports of invalid configuration now better describe the source of the issue.
    * "Add file" while disconnected causes NPE (#35) 



## ::v0.5.0::

### Overview

* Added support for IntelliJ on JDK 1.6
* Added job support to check-ins.
* Bug fixes

### Details

* Added support for IntelliJ on JDK 1.6. (#28)
    * Compiled under JDK 1.6, and removed JDK 1.7-specific API calls.
* Added job support to check-ins. (#25 & #26)
    * Experimental: the list of acceptable job status values are pulled
      from the server.  If these are inaccessible, then the default
      list is shown.
* Bug fixes
    * Adding files (Ctrl+Alt+A) would sometimes call an unimplemented function.
    * Multiple clients per project would label a file as being under the wrong client.
    * Deleted files would not be submitted.
    * Annotated file can encounter null depot file path, which causes an error.
    * File history can cause an error when one file in the path was removed.
    * Submitting a changelist which causes an error incorrectly reports the submit as
      successful. (#31)
    * Files would move incorrectly from the correct changelist into the default
      changelist. (#22)
    * Move file across clients should use integrate when clients share a server (#15)
    * Revert a moved file does not refresh status (#7) 



## ::v0.4.3::

### Overview

* Added basic synchronization with server.
* Added connection properties display to VCS configuration panel.
* Minor bug fixes

### Details

* Added basic synchronization with server.
    * Only available through the VCS menu, not through the context menu.
    * Does not handle merging.  Any potential issues with existing
      checked-out files being synched are not reported.
* Added connection properties display to VCS configuration panel.
    * Allows for the user to view the connection properties that will
      actually be used to connect to Perforce.
    * For relative configurations, you can select the child path
      (and see which child paths are detected to contain config files)
      to see its specific configuration properties.
    * Password is not displayed, but it indicates whether it is explicitly
      set or not.
* Minor bug fixes.
    * Files in changelists are not always reported correctly.  This was due
      to incorrect caching that came from an earlier refactoring effort.
    * Files moved between changelists seems to be fixed (#22).
    * Typo in the plugin description.
    * Code cleanup.



## ::v0.4.2::

### Overview

* Minor bug fixes.
* Fixed the change list view refresh.
* Enabled submit.

### Details

* Submit enabled.  Very early version of submit is now possible.  It does not allow for
  setting job status or adding jobs to the submitted changelist.
* Fixed the change list view refresh.  The magic invocation to have the "Version Control"
  panel properly refresh was discovered.
    * Should fix bugs #8, #9, #14
* Minor bug fixes.
    * A `P4PORT=1666` format would fail parsing, because the P4 Java API only supports
      `hostname:port` or `schema://hostname:port` formats.  As a fix,
      `localhost` is prefixed for any P4PORT setting that does not have a colon in
      the actual port part.
    * Under some circumstances, having 2 or more clients in the same project could
      lead to a `NullPointerException` while mapping Perforce change lists to
      IDEA change lists.


## ::v0.4.1::

### Overview

* Minor bug fix with changelist association with the default changelist.

### Details



## ::v0.4.0::

### Overview

* Major refactoring to the management of changelists.
* Bug fixes.

### Details

* Major refactoring to the management of changelists.
    * Perforce Changelists are now cached application-wide.
      This allows for code clean-up that should hopefully make
      some of the file migration between changelists
      less common (#22).
* Bug fixes.
    * non-numeric perforce server port (#23)
    * Background threads were causing refresh problems.  Turned them
      off to limit the number of issues they caused.
* Add support for IDEA 141.2.2


## ::v0.3.3::

### Overview

* Add in support for all versions between 13.5 and 14.1


## ::v0.3.2::

### Overview

* Minor UI bug fixes.
* Fix for default settings panel

### Details

* Minor UI bug fixes.
      * Returning online can cause error "user selected work offline" (#20).
* Fix for default settings panel
      * If you edit the default settings for the Perforce plugin
        (File -> Other Settings -> Default Settings), the panel will throw
        an exception (#21).

## ::v0.3.1::

### Overview

* Add support for IDEA 13.5 (e.g. Android Studio).
* Minor UI bug fixes.
* Improvements to Perforce server communication performance.

### Details

* Add support for IDEA 13.5 (e.g. Android Studio):
    * Major refactoring to allow for multiple versions of IDEA.
      This was tested against Android Studio version 1.0.1. (#16)
* UI bug fixes:
    * Changelist decorator can show the same client name multiple times.
    * Incompatible versions need to have better UI notification (#18).
    * Deadlock issue on disconnect (#17)
    * Relative P4CONFIG Connection setting does not always disable the
      client button (#11)
    * Connection status widget does not show current status correctly (#3)
* Perforce server communication performance:
    * Corrected connection flag that forced several API hacks (#12).


## ::v0.3.0::

Initial public release


