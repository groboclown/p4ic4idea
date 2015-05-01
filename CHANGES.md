# IDEA Community VCS Integration for Perforce



## ::v0.5.1::

### Overview

* Internationalized the exception messages.
* Bug fixes

### Details

* Internationalized the exception messages.  Exceptions can be
    reported to the user, and so they need I18N support.
* Bug fixes
    * Not finding P4CONFIG in the hierarchy (#32)
    * Partial fix for incorrect parsing of P4Java API issue with fetching jobs (#33) 
    * Config "resolved values" should better reflect if it has been loaded (#34)
    * Reports of invalid configuration now better describe the source of the issue.



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


