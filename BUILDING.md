# How to build the Plugin


## Prerequisite Software

First, you need to install the prerequisite software.

1. **JDK 1.8** - Starting with IntelliJ IDEA 16, you need to be running with
   a version of JDK 1.8.  Note that only the IDEA 16 and later compatible modules
   actually needs JDK 1.8; the rest are written for JDK 1.6 for earlier
   IDE version compatibility.
1. **Java 6 Runtime Libraries** - In order to properly build with JDK 1.6
   compatibility, you *should* have the Java 6 runtime libraries installed
   in a directory.  These files are:
   * `jce.jar`
   * `jsse.jar`
   * `rt.jar`
1. **Ant 1.8 or better** - the source uses Ant as the build tool.
1. **Git repo with submodules** - clone the Git repo from GitHub, and make
   sure to pull in the submodules.
   
   ```
   git clone --recursive (repository name)
   ```
   
   The submodule contains the IntelliJ libraries for different versions.
   It's put into a submodule, because GitHub has a hard limit on file
   sizes, whereas the SourceForge git repo doesn't.


## Telling Ant About Your Environment

Once you have the prerequisites installed, you will need to copy
the file `local.properties.template` from
your root git repo directory, to a new file named `local.properties`,
in the same directory.

You will need to tell it the location of the JDK 1.8 version that you
use to build.

```
project.jdk.home=/opt/jdk1.8
```

You also need to tell it the location of the Java 6 classpath location.
Note that if you don't have this, then you can point it to the
JDK 1.8 `jre/lib` directory, but that may cause issues if
you accidentally use JDK 1.7 or higher APIs.

```
java6.lib=/opt/jdk1.6/jre/lib
```

## Building

From the root directory, simply run:

```
$ $ANT_HOME/bin/ant all
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

Because the IntelliJ libraries are stored in the separate SourceForge Git project
`p4ic4idealib`, the process for updating is a little different.
 
1. Get the `p4ic4idealib` repository (separate from the GitHub backed `p4ic4idea`
   project). *TODO explain how*
1. Add the libraries into the new directory.  Follow the `about.txt` for details on
   trimming the `idea.jar` file down by about 50 mb.
1. Commit the `p4ic4idealib` and push to master.
1. In the `p4ic4idea` project:
    ```
    $ cd intellij-lib
    $ git checkout master
    $ git pull
    $ cd ..
    $ git commit -m 'Updated IntelliJ libraries to version X' intellij-lib
    ```


# Source Code Documentation

The source code contains JavaDoc and comments to help in understanding the immediate
concerns of the code, but the general overall documentation is located under the
[developer documentation](plugin/docs/developers).
There's also a few notes in [todo.md](plugin/src/net/groboclown/idea/p4ic/vc/todo.md),
but that needs to be moved out into more formal places.
