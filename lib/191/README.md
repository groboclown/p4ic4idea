# About

Built from the Git repository.

## Build Instructions

If you use OpenJDK, make sure you also have JavaFX installed.

Your computer must be online to pull the files and run the build.

**WARNING**: Because 191 is not out yet, this directory should not be checked in.  Once it's been released, then remove
the `.gitignore` file from this directory, and check in the jars.

To find the 191 files, look in the intellij-community directory `.git/refs/tags/idea/191.*` and get the release version.
Likewise for the android and android tools trees.

```(bash)
$ git clone --depth 1 git://git.jetbrains.org/idea/community.git intellij-community
$ cd intellij-community
$ git fetch -t
$ git checkout -b local-191 idea/191.???
$ ./getPlugins.sh
$ ( cd android && git checkout -b local-191 idea/191.??? )
$ ( cd android/tools-base && git checkout -b local-191 idea/191.??? )
$ ant
```

The build will probably break when building the distribution files, but
that's fine.  We don't want those files anyway.

## Assembly Instructions

```
$ jardir=$( this 181 directory )
$ cd out/idea-ce/classes/production
$ for i in *; do test -d "$i" && ( cd "$i" && zip -9r "$jardir/$i.jar" * ) ; done
$ cp ../../dist.all/LICENSE.txt ../../out/idea-ce/dist.all/NOTICE.txt "$jardir/."
$ rm "$jardir/android*.jar"
$ rm "$jardir/intellij.android*.jar"
```

## Dependent Jars

The dependent jars that initellij depends on also need to be included.

```
$ jardir=$( this 181 directory )
$ mkdir "$jardir/deps"
$ cp lib/*.jar "$jardir/deps/."
$ cp license/* "$jardir/deps/."
$ cp build/dependencies/build/kotlin/Kotlin/lib/kotlin-stdlib.jar "$jardir/deps/kotlin-runtime.jar"
$ cp out/idea-ce/dist.all/lib/trove4j.jar "$jardir/deps/."
```

The `kotlin-runtime.jar` is pulled by the dependency build (run by `ant init`), which in turn downloads it from
`http://plugins.jetbrains.com/maven/com/jetbrains/plugins/org.jetbrains.kotlin/1.2.31-release-IJ2018.1-1/org.jetbrains.kotlin-1.2.31-release-IJ2018.1-1.zip`.
