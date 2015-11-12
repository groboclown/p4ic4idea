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
