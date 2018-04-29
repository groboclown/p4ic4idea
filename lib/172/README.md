# About

Built from the Git repository.

## Build Instructions

If you use OpenJDK, make sure you also have JavaFX installed.

```(sh)
$ git clone https://github.com/JetBrains/intellij-community.git intellij-community
$ cd intellij-community
$ git checkout 172
$ git clone https://github.com/JetBrains/android.git android
$ ( cd android && git checkout 172 )
$ git clone https://github.com/JetBrains/adt-tools-base.git android/tools-base
$ ( cd android/tools-base && git checkout idea/172.1268 )
$ test -d out && rm -r out
```

The standard "ant" execution has too often died during the
`gradle dependenciesFile` step, so a little adjustment to the
`platform/build-scripts/groovy/org/jetbrains/intellij/build/GradleRunner.groovy`
method `runInner(String...)` helps it get by, and shows more verbose
information.

```(groovy)
  private boolean runInner(String... tasks) {
    def gradleScript = SystemInfo.isWindows ? 'gradlew.bat' : 'gradlew'
    List<String> command = new ArrayList()
    command.add("${projectDir.absolutePath}/$gradleScript".toString())
    command.addAll(tasks)
    command.add('--stacktrace')
    command.add('--no-daemon')
    command.add('--info')
    def processBuilder = new ProcessBuilder(command).directory(projectDir)
    messages.warning("Running gradle script (from ${projectDir}, JAVA_HOME=${javaHome}) ${gradleScript} ${command.join(' ')} ")
    processBuilder.environment().put("JAVA_HOME", javaHome)
    def process = processBuilder.start()
    process.consumeProcessOutputStream((OutputStream)System.out)
    process.consumeProcessErrorStream((OutputStream)System.err)
    //return process.waitFor() == 0
    def ret = process.waitFor()
    messages.warning("Gradle script returned ${ret}")
    return ret == 0
  }
```

I was also finding that the build tried using `build/jdk/1.8` as the JDK
for running the gradle task, which was causing problems in my
environment (a `java` executable didn't exist).  I had to create a soft
link on the `build/jdk/1.8` to my locally installed JDK to get it to
work.  If it still tries to use the wrong path, just hard code a correct
path in the `GradleRunner.groovy` file

Additionally, some of the Android build dependencies aren't properly
setup as module dependencies, so we need to configure those correctly.
The Idea project files:

* `android/tools-base/sdk-common/sdk-common.iml`
* `android/tools-base/lint/libs/lint-api/lint-api-base.iml`

need to be modified to remove this line (if it exists):

```(xml)
<orderEntry type="library" exported="" name="builder-model" level="project" />
```

and add in this line:

```(xml)
<orderEntry type="module" module-name="builder-model" exported="" />
```

There are also issues with the Android repo compiling against a different
version of the adt-tools repo.  I haven't been able to completely resolve
this the correct way (e.g. find the right tag or checkin to sync to),
so instead I just hand fixed the problems.  The goal is not to make
*correct* jars, but rather API signature jars.

Start with an update to
`android/tools-base.build-system/builder-model/src/main/java/com/android/builder/model/AndroidProject.java`
to include this inside the interface definition:

```(java)
    int PROJECT_TYPE_APP = 0;
    int PROJECT_TYPE_ATOM = 1;
    int PROJECT_TYPE_LIBRARY = 2;
```

Then update the file
`android/tools-base/perflib/src/main/java/com/android/tools/perflib/vmtrace/VmTraceData.java`
to include this inside the class definition:

```(java)
    public long getStartTimeUs() {
        throw new UnsupportedOperationException();
    }
```

Then we need a new class file
`android/tools-base/sdk-common/src/main/java/com/android/ide/common/resources/ResourceValueMap.java`
that looks like

```(java)
package com.android.ide.common.resources;
import com.android.ide.common.rendering.api.ResourceValue;
import java.util.HashMap;
import java.util.Map;
public class ResourceValueMap extends HashMap<String, ResourceValue> {
  public ResourceValueMap(Map<String, ResourceValue> resource) {
    super(resource);
  }
}
```

and then `AbstractResourceRepository`, `ResourceRepository`, and
`ResourceResolver` need to be fixed to work with this so that
`android/android/src/com/android/tools/idea/configurations/ResourceResolverCache.java`
compiles.  It's a pain.

And then continue on to the build...

```(sh)
$ ant
```

and hope that everything worked fine.

## Assembly Instructions

```(sh)
$ jardir=$( the p4ic4idea lib/172 directory )
$ pushd out/idea-ce/classes/production
$ for i in *; do test -d "$i" && ( cd "$i" && zip -9r "$jardir/$i.jar" * ) ; done
$ popd
$ cp LICENSE.txt NOTICE.txt "$jardir/."
$ rm "$jardir"/android*.jar "$jardir"/builder-model.jar
```

## Dependent Jars

The dependent jars that initellij depends on also need to be included.

```(sh)
$ jardir=$( the p4ic4idea lib/172 directory )
$ mkdir "$jardir/deps"
$ cp lib/*.jar "$jardir/deps/."
$ cp license/* "$jardir/deps/."
$ cp build/dependencies/build/kotlin/Kotlin/lib/kotlin-runtime.jar "$jardir/deps/."
```

The `kotlin-runtime.jar` file is created by the dependency fetch, which downloads the build from
`http://plugins.jetbrains.com/maven/com/jetbrains/plugins/org.jetbrains.kotlin/1.1.4-release-IJ2017.2-3/org.jetbrains.kotlin-1.1.4-release-IJ2017.2-3.zip`.
