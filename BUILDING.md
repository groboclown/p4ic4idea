# How to build the Plugin


## Prerequisite Software

First, you need to install the prerequisite software.

1. **JDK 1.8** - Starting with IntelliJ IDEA 16, you need to be running with
   a version of JDK 1.8.  Note that only the IDEA 16 and later compatible modules
   actually needs JDK 1.8; the rest are written for JDK 1.6 for earlier
   IDE version compatibility.  *Additionally, the build is not currently working for JDK10 or better.  Haven't tried
   with JDK9.*
1. **Git repo** - clone the Git repo from GitHub.
   
   ```(bash)
   $ git clone (repository name)
   ```

## Building

From the root directory, simply run:

```(bash)
$ ./gradlew build
```


## Debugging In IntelliJ

For the most part, the project is now easy to get running from within
IntelliJ.  You should be able to open the cloned directory as a project
in IntelliJ and start running.

You may need to adjust the project SDK dependency to be a JDK 1.8 jvm on
your local computer.  Additionally, the "plugin" module will need to
use your actively running IntelliJ directory as the JDK.


# Contributing

## Incremental Changes

If you have a bug fix or feature that you'd like to see added to the plugin,
please follow these steps:

1. Fork the project on Github.
1. Make your changes in your fork, making sure to include the Apache license header
   and follow the formatting standard for the project.
1. Commit your changes and push them up into your Github repository.
1. Make a *pull request* in the parent project.


## Updating the IntelliJ Libraries

Because the IntelliJ libraries are built from source, new versions
must be carefully constructed using the general instructions provided
in the [README.md](lib/README.md) file.


## Updates to the Perforce C Client

When new versions of the Perforce C client source are released, the script
[generate-error-codes.py](p4java/generate-error-codes.py) should be run.  This will
update the message code ID interface.  It's good practice to compare the update against
the previous version, so that changes in the code can be tracked.


# Source Code Documentation

The source code contains JavaDoc and comments to help in understanding the immediate
concerns of the code, but the general overall documentation is located under the
[developer documentation](docs/dev).  Expect this to be moderately
out of date.
