# About

Built from the Git repository.

## Build Instructions

If you use OpenJDK, make sure you also have JavaFX installed.

```
$ git clone https://github.com/JetBrains/intellij-community.git intellij-community
$ cd intellij-community
$ git checkout 171
$ git clone https://github.com/JetBrains/android.git android
$ ( cd android && git checkout 171 )
$ git clone https://github.com/JetBrains/adt-tools-base.git android/tools-base
$ ( cd android/tools-base && git checkout 171 )
$ ant
```

It's okay for the build to fail while assembling the final archives.  We only
care about the class files.

## Assembly Instructions

```
$ jardir=$( this 171 directory )
$ cd out/classes/production
$ for i in *; do test -d "$i" && ( cd "$i" && zip -9r "$jardir/$i.jar" * ) ; done
$ cp LICENSE.txt NOTICE.txt "$jardir/."
$ rm "$jardir/android*.jar"
```

Due to how the custom gradle plugin works, a little extra step is required:

```
$ mv instrumentation-util-8.jar instrumentation-util-jre8.jar
```

## Dependent Jars

The dependent jars that initellij depends on also need to be included.

```
$ jardir=$( this 171 directory )
$ mkdir "$jardir/deps"
$ cp lib/*.jar "$jardir/deps/."
$ cp license/* "$jardir/deps/."
```

