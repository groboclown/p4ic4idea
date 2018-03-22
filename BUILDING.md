# How to build the Plugin


## Prerequisite Software

First, you need to install the prerequisite software.

1. **JDK 1.8** - Starting with IntelliJ IDEA 16, you need to be running with
   a version of JDK 1.8.  Note that only the IDEA 16 and later compatible modules
   actually needs JDK 1.8; the rest are written for JDK 1.6 for earlier
   IDE version compatibility.
1. **Git repo** - clone the Git repo from GitHub.
   
   ```
   $ git clone (repository name)
   ```

## Building

From the root directory, simply run:

```
$ ./gradlew build
```


## Debugging In IDEA

The IDEA project is constructed differently than how the Ant build constructs the
project, so that it is possible to debug the plugin in IDEA.

The biggest difficulty is in constructing your Java configuration so that it
will actually run the plugin.  In my environment, every time IDEA tries to
launch the plugin sandbox environment, it thinks the IDEA bootstrap jars
exist in the JDK lib directory.  To work around this, I copied my JDK
to a new location, and explicitly copied the IDEA lib files it expected,
into the JDK lib directory.


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
in the [README.md](lib/173/README.md) file.


# Source Code Documentation

The source code contains JavaDoc and comments to help in understanding the immediate
concerns of the code, but the general overall documentation is located under the
[developer documentation](plugin/docs/developers).
There's also a few notes in [todo.md](plugin/src/net/groboclown/idea/p4ic/vc/todo.md),
but that needs to be moved out into more formal places.
