# About

Built from the Git repository.  Future instructions should avoid using android/tools-base, as it
isn't used anymore.  Indeed, it doesn't work with 173, but unfortunately I forced it to work.
But the Android libs are removed anyway.

## Build Instructions

If you use OpenJDK, make sure you also have JavaFX installed.

```
$ git clone https://github.com/JetBrains/intellij-community.git intellij-community
$ cd intellij-community
$ git checkout 173
$ ./getPlugins.sh
$ ( cd android && git checkout 173 )
$ ( cd android/tools-base && git checkout idea/173 )
$ ant
```

The build breaks at the android tools, which is fine.

## Assembly Instructions

```
$ jardir=$( this 173 directory )
$ cd out/idea-ce/classes/production
$ for i in *; do test -d "$i" && ( cd "$i" && zip -9r "$jardir/$i.jar" * ) ; done
$ cp LICENSE.txt NOTICE.txt "$jardir/."
$ rm "$jardir/android*.jar"
```

## Dependent Jars

The dependent jars that initellij depends on also need to be included.

```
$ jardir=$( this 173 directory )
$ mkdir "$jardir/deps"
$ cp lib/*.jar "$jardir/deps/."
$ cp license/* "$jardir/deps/."
```

