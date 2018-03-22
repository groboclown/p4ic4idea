# About

Built from the Git repository.

## Build Instructions

If you use OpenJDK, make sure you also have JavaFX installed.

```
$ git clone https://github.com/JetBrains/intellij-community.git intellij-community
$ cd intellij-community
$ git checkout 172
$ git clone https://github.com/JetBrains/android.git android
$ ( cd android && git checkout 172 )
$ git clone https://github.com/JetBrains/adt-tools-base.git android/tools-base
$ ( cd android/tools-base && git checkout idea/172.990 )
$ ant
```

The build breaks at the android tools, which is fine.

## Assembly Instructions

```
$ jardir=$( this 172 directory )
$ cd out/idea-ce/classes/production
$ for i in *; do test -d "$i" && ( cd "$i" && zip -9r "$jardir/$i.jar" * ) ; done
$ cp LICENSE.txt NOTICE.txt "$jardir/."
$ rm "$jardir/android*.jar"
```

Note that in the current bundle, vcs-api-core.jar was not created, and platform-api was built wrong.

## Dependent Jars

The dependent jars that initellij depends on also need to be included.

```
$ jardir=$( this 172 directory )
$ mkdir "$jardir/deps"
$ cp lib/*.jar "$jardir/deps/."
$ cp license/* "$jardir/deps/."
```

