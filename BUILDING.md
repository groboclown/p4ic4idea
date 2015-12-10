# How to build the Plugin

First, you need to install the prerequisite software.

1. *JDK 1.6* - because the plugin needs to run on both the Android IDE and
   IntelliJ for Mac, it needs JDK 1.6 support.  Technically, the build can
   run in 1.6 emulation with a current JDK, but 1.6 is best.
1. *Ant 1.8 or better* - the source uses Ant as the build tool.
1. *Git repo with submodules* - clone the Git repo from GitHub, and make
   sure to pull in the submodules.
   ```
   git clone (repository name)
   git pull --recurse-submodules
   ```
   The submodule contains the IntelliJ libraries for different versions.
   It's put into a submodule, because GitHub has a hard limit on file
   sizes, whereas the SourceForge git repo doesn't.


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
