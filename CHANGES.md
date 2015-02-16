# IDEA Community VCS Integration for Perforce

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


