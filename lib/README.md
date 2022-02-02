# Library Dependencies

This project includes all library dependencies under this directory.  Each included file has its own license.


# IntelliJ Dependencies

The directories with numbers (e.g. [183](183)) are that version of the IntelliJ jars used by this project to build.

These jars were built from source pulled from the IntelliJ community project.

## Building IntelliJ Dependencies

Building the dependencies requires pulling the source, running the build, and finding the smallest set of dependent jars.

To build, you will need to have these tools installed:

* JDK 1.8, with `JAVA_HOME` pointing to it.  You will also need JavaFX for this version (sometimes called OpenJFX).
* Optionally, if you have JDK 1.6 installed, point `JDK_16_x64` to it.
* Ant version 1.9 or better, with `ANT_HOME` pointing to it.
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

Listing of releases to the tag is at [the other download section](https://www.jetbrains.com/idea/download/other.html).  The "build" value on this page refers to the Git tag.


#### Clone Just The Tag

Create a shallow clone for just the tag you care about.

```bash
$ git clone git://git.jetbrains.org/idea/community.git --branch=tags/idea/191.8026.42 idea-community
$ cd idea-community
$ git clone git://git.jetbrains.org/idea/android.git --branch=tags/idea/191.8026.42 android
$ cd tools-base
$ git clone git://git.jetbrains.org/idea/adt-tools-base.git --branch=tags/idea/191.8026.42 tools-base
```

#### Clone All The Tags

Pull the source from the remote repository, and include the tags.

```bash
$ git clone --bare git://git.jetbrains.org/idea/community.git idea-community
$ cd idea-community
$ git fetch --all --tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
$ git clone --bare git://git.jetbrains.org/idea/android.git android
$ cd android
$ git fetch --all --tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
$ git clone --bare git://git.jetbrains.org/idea/adt-tools-base.git tools-base
$ cd tools-base
$ git fetch --all -tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
```

That might be paranoid, but it works and keeps you from running into odd compile failures, especially after a fetch in an existing repository.

If you're updating an existing clone, then you can run:

```bash
$ cd idea-community
$ git fetch --all --tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
$ git clean -f -e android
$ cd android
$ git fetch --all --tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
$ git clean -f -e tools-base
$ cd tools-base
$ git fetch --all --tags --prune
$ git checkout -f tags/idea/191.8026.42 -b 191
$ git clean -f
```


### Run the Build

```bash
$ $ANT_HOME/bin/ant
```

This may fail.  See the tagged version notes below for how the source was built to get past these issues.

### Collect the Files

First, get the jars.  This differs from version to version, but for >= 181, this bash script works:

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

Create a copy of `buildSrc/src/main/groovy/p4ic/ext/ideaVersionExample.groovy` named based on the version number.  Change the matcher regex to match the version.  Update the `IdeaVersion.groovy` sibling file to include the new version number file in the `VERSION_LIB_MATCHERS` list.

Next up, is pulling in the libraries and matching those to the library names referenced in the build scripts.  This involves finding the jars created above, pulling them into the `lib/(version)` directory, and mapping the jar name to the bundle name in the version groovy script.  Trial and error is slowest, but best.

Also with this is pulling in the IDE dependencies.  Make sure to grab the license files, too!  Some of these you need to dig deep on.

* 193:
  * annotations: pulled from `build/dependencies/build/build-scripts-deps/annotations-java5-17.0.0.jar`
  * picocontainer: only pulled now from the Kotlin compiler?  That's weird.  Anyway, I grabbed a previously bundled version.  This is only used for tests, so that should be fine.
* 201:
  * annotations: pulled from `build/dependencies/build/build-scripts-deps/annotations-java5-19.0.0.jar`

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

Alternatively, you can search the `out/idea-ce/classes/production` directory for the class file, but identifying the corresponding jar is a touch more work.


### Update SCM

Add the files to Git, commit, and push.


### Jar Tagged Versions

* 171 - tag ?, version ?
* 172 - tag ?, version ?
* 173 - tag ?, version ?
* 181 - tag ?, version ?
* 182 - tag ?, version ?
* 183 - tag ?, version ?
* 191 - tag `191.8026.42`, IDE version 2019.1.4
    * 191.8026.42: Fails to build module 'intellij.java.impl'
* 192 - tag `192.7142.36`, version 2019.2.4
    * Fails to build module 'intellij.java.impl'.  "Fixed" by emptying the contents of `java/java-impl/src`.
    * Fails on community plugins.  Because, by this point, the necessary parts had been built, the error was ignored and the rest of the build was skipped.
* 193 - tag `idea/193.7288.26`, IDE version 2019.3.5, build-tool tag `idea/193.6911.18`
  * Fails on compiling `android.sdktools.lint-api` with `Could not transfer artifact org.jetbrains.kotlin:kotlin-plugin-ij193:pom:1.3.61-release-180 from/to maven4 (https://repo.labs.intellij.net/jet-sign): repo.labs.intellij.net: Name or service not known: Unknown host: repo.labs.intellij.net: Name or service not known`.
* 201 - tag `idea/201.8743.12`, IDE version 2020.1.4, build-tool tag `idea/201.7223.18`
  * The debugging tree will fail if you don't have `JAVA_HOME` set.
  * Fails building android.sdktools.lint-api with `Could not find artifact org.jetbrains.kotlin:kotlin-plugin-ij201:jar:1.3.72-release-468 in central (https://cache-redirector.jetbrains.com/maven-central)`.  But that's beyond the point that we care about for the jars necessary for this project.
* 202 - tag 202.8194.7 (2020.2.4)
* 203 - tag 203.8084.24 (2020.3.4)
* 211 - tag 211.7628.21 (2021.1.3)
* 212 - tag 212.5457.46 (2021.2.3)
* 213 - tag 213.5744.223 (2021.3)
