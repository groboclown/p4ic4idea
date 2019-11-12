# Library Dependencies

This project includes all library dependencies under this directory.  Each included file has its own license.


# IntelliJ Dependencies

The directories with numbers (e.g. [183](183)) are that version of the IntelliJ jars used by this project to build.

These jars were built from source pulled from the IntelliJ community project.

## Building IntelliJ Dependencies

Building the dependencies requires pulling the source, running the build, and finding the smallest set of dependent jars.

To build, you will need to have these tools installed:

* JDK 1.8, with `JAVA_HOME` pointing to it.
* Optionally, if you have JDK 1.6 installed, point `JDK_16_x64` to it.
* Ant version 1.9 or better.
* git


### Pull the Source

There are several options for pulling down the source, but they all involve work with the `git` command, and knowing the release you want to build.

Releases are stored by tag, and the tag is a combination of the product name and the build version.  For this project, we only care about the `idea` releases.  The tags for the release can be compared against the version number found on the website (last found [here](https://www.jetbrains.com/idea/download/other.html); see the below list for details).

You will need to perform three sets of clones, for these repositories, in this order:

1. `git://git.jetbrains.org/idea/community.git` - put in the root directory of where you want the files.
1. `git://git.jetbrains.org/idea/android.git` - put in the `android` directory under the cloned `community` directory.
1. `git://git.jetbrains.org/idea/adt-tools-base.git` - put in the `tools-base` directory under the `community/android` directory.

For example, if you want to just pull everything down (not recommended) into a directory named `idea-community`, the process would be:

```bash
$ git clone git://git.jetbrains.org/idea/community.git idea-community
$ git clone git://git.jetbrains.org/idea/android.git idea-community/android
$ git clone git://git.jetbrains.org/idea/adt-tools-base.git idea-community/android/tools-base
```

If you want to find the list of tags from a cloned environment, you can run:

```bash
$ git tag --list "idea/191.*"
```



#### Clone Just The Tag

Create a shallow clone for just the tag you care about.

```bash
$ git clone git://git.jetbrains.org/idea/community.git --branch=tags/idea/191.8026.42 idea-community
```

#### Clone All The Tags

Pull the source from the remote repository, and include the tags.

```bash
$ git clone --bare git://git.jetbrains.org/idea/community.git idea-community
$ cd idea-community
$ git fetch --all --tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
$ git clean -f -e android -e tools-base
```

That might be paranoid, but it works and keeps you from running into odd compile failures, especially after a fetch in an existing repository.


### Run the Build

```bash
$ ant
```

This may fail.  See the tagged version notes below for how the source was built to get passed these issues.

### Get the Files

First, get the jars.  This differs from version to version, but for >= 181, the general method is:

```bash
$ cd out/idea-ce/classes/production
$ for i in *; do
  if [ -d "$i"] ; then
    n=$( basename "$i" )
    ( cd "$i" && zip -9r ../../../../"$n".jar * )
  fi
done 
```

This creates the jar for each package, allowing for a smaller library footprint.  Don't copy these into the library directory just yet.

Then, get the license files.  Copy the `LICENSE.txt` and `NOTICE.txt` from the root directory into the `lib/(version)` directory.  Once these license files are added, the build will trigger that directory as an IDE version to build against.  Until you complete the next step, your builds won't work. 


### Update the Library Matcher

Create a copy of `buildSrc/src/main/groovy/p4ic/ext/ideaVersionExample.groovy` named based on the version number.  Change the matcher regex to match the version.

Next up, is pulling in the libraries and matching those to the library names referenced in the build scripts.  This involves finding the jars created above, pulling them into the `lib/(version)` directory, and mapping the jar name to the bundle name in the version groovy script.  Trial and error is slowest, but best.

Also with this is pulling in the IDE dependencies.  Make sure to grab the license files, too!  Some of these you need to dig deep on.

* annotations: pulled from `build/dependencies/build/kotlin/Kotlin/lib/annotations-13.0.jar`
* picocontainer: only pulled now from the Kotlin compiler?  That's weird.  Anyway, I grabbed a previously bundled version.

This script helps.  I call it `ziplist.sh`:

```bash
#!/bin/bash

sep=" "
for i in "$@"; do
        unzip -qql "$i" 2>/dev/null | tr -s ' ' | cut -f 5- -d ' ' | awk '{print "'"$i$sep"'" $0}'
done
```

Then run it with a chain:

```bash
find . -name '*.jar' -print0 | xargs -0 ziplist.sh | grep my/file/Name.class
```

### Update SCM

Add the files to Git, and upload.


### Jar Tagged Versions

* 171 - tag ?, version ?
* 172 - tag ?, version ?
* 173 - tag ?, version ?
* 181 - tag ?, version ?
* 182 - tag ?, version ?
* 183 - tag ?, version ?
* 191 - tag `191.8026.42`, version 2019.1.4
    * 191.8026.42: Fails to build module 'intellij.java.impl'
* 192 - tag `192.7142.36`, version 2019.2.4
    * Fails to build module 'intellij.java.impl'.  "Fixed" by emptying the contents of `java/java-impl/src`.
    * Fails on community plugins.  Because, by this point, the necessary parts had been built, the error was ignored and the rest of the build was skipped.

