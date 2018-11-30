# About

Built from the Git repository.

## Build Instructions

If you use OpenJDK, make sure you also have JavaFX installed.

Your computer must be online to pull the files and run the build.

```(bash)
$ git clone --depth 1 git://git.jetbrains.org/idea/community.git intellij-community
$ cd intellij-community
$ git fetch -t
$ git checkout -b local-183 idea/183.3647.2
$ ./getPlugins.sh
$ ( cd android && git checkout -b local-183 idea/183.4749 )
$ ( cd android/tools-base && git checkout -b local-183 idea/183.4749 )
$ ant
```

The build will probably break when building the distribution files, but
that's fine.  We don't want those files anyway.

## Assembly Instructions

```
$ jardir=$( this 183 directory )
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
