#!/bin/sh -x

set -e

# Basic files.

export P4USER=p4jtestuser
export P4PASSWD=p4jtestuser
export P4CLIENT=p4TestUserWS

# hard-coded root at /tmp/client/p4TestUserWS

mkdir -p /tmp/client/p4TestUserWS
cd /tmp/client/p4TestUserWS
p4 sync ...


# General Tests

mkdir -p SandboxTest/Attributes
cp "$SOURCE_ROOT/text/text00.txt" SandboxTest/Attributes/test01.txt
p4 add SandboxTest/Attributes/test01.txt
p4 submit -d "sandbox test files 1"
p4 edit SandboxTest/Attributes/test01.txt
cp "$SOURCE_ROOT/text/text01.txt" SandboxTest/Attributes/test01.txt
p4 submit -d "sandbox test files 2"

# TODO this requires more work
mkdir -p client/ResolveFileStreamTest
cp "$SOURCE_ROOT/text/text01.txt" client/ResolveFileStreamTest/test01.txt
cp "$SOURCE_ROOT/text/text02.txt" client/ResolveFileStreamTest/test02.txt
p4 add client/ResolveFileStreamTest/test01.txt client/ResolveFileStreamTest/test02.txt
p4 submit -d "ResolveFileStreamTest 1"


# features 112 tests

# GetOpenedFilesTest
mkdir -p 112Dev/GetOpenedFilesTest
echo "00" > 112Dev/GetOpenedFilesTest/00.txt
echo "01" > 112Dev/GetOpenedFilesTest/01.txt
echo "02" > 112Dev/GetOpenedFilesTest/02.txt
echo "03" > 112Dev/GetOpenedFilesTest/03.txt
echo "04" > 112Dev/GetOpenedFilesTest/04.txt
echo "05" > 112Dev/GetOpenedFilesTest/05.txt
echo "06" > 112Dev/GetOpenedFilesTest/06.txt
echo "07" > 112Dev/GetOpenedFilesTest/07.txt
echo "08" > 112Dev/GetOpenedFilesTest/08.txt
echo "09" > 112Dev/GetOpenedFilesTest/09.txt
p4 add 112Dev/GetOpenedFilesTest/*
p4 submit -d "feature 112 GetOpenedFilesTest 1"

mkdir -p 112Dev/GetOpenedFilesTest/bin/gnu/getopt
cp "$SOURCE_ROOT/text/text00.txt" 112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties
p4 add 112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties
p4 submit -d "feature 112 GetOpenedFilesTest 2"

p4 integrate 112Dev/GetOpenedFilesTest/bin/gnu/getopt/MessagesBundle_es.properties#1 112Dev/GetOpenedFilesTest/bin/gnu/getopt/main303385669/MessagesBundle_es.properties
p4 submit -d "feature 112 GetOpenedFilesTest 3a"
p4 edit 112Dev/GetOpenedFilesTest/bin/gnu/getopt/main303385669/MessagesBundle_es.properties
cp "$SOURCE_ROOT/text/text01.txt" 112Dev/GetOpenedFilesTest/bin/gnu/getopt/main303385669/MessagesBundle_es.properties
p4 submit -d "feature 112 GetOpenedFilesTest 3b"
p4 delete 112Dev/GetOpenedFilesTest/bin/gnu/getopt/main303385669/MessagesBundle_es.properties
p4 submit -d "feature 112 GetOpenedFilesTest 3c"

p4 integrate 112Dev/GetOpenedFilesTest/bin/gnu/getopt/main303385669/MessagesBundle_es.properties#1 112Dev/GetOpenedFilesTest/bin/gnu/getopt/release303385669/MessagesBundle_es.properties
p4 submit -d "feature 112 GetOpenedFilesTest 5a"
p4 delete 112Dev/GetOpenedFilesTest/bin/gnu/getopt/release303385669/MessagesBundle_es.properties
p4 submit -d "feature 112 GetOpenedFilesTest 5b"

mkdir -p 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd
cp "$SOURCE_ROOT/text/text00.txt" 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java
p4 add 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java
p4 submit -d "feature 112 GetOpenedFilesTest 6a"
p4 edit 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java
cp "$SOURCE_ROOT/text/text01.txt" 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java
p4 submit -d "feature 112 GetOpenedFilesTest 6b"
p4 edit 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java
cp "$SOURCE_ROOT/text/text02.txt" 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java
p4 submit -d "feature 112 GetOpenedFilesTest 6c"

p4 integrate 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdDispatcher.java#1 112Dev/GetOpenedFilesTest/src/com/perforce/branch534212163/P4CmdDispatcher.java
p4 submit -d "feature 112 GetOpenedFilesTest 7a"
p4 delete 112Dev/GetOpenedFilesTest/src/com/perforce/branch534212163/P4CmdDispatcher.java
p4 submit -d "feature 112 GetOpenedFilesTest 7b"

cp "$SOURCE_ROOT/text/text00.txt" 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java
p4 add 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java
p4 submit -d "feature 112 GetOpenedFilesTest 8a"
p4 edit 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java
cp "$SOURCE_ROOT/text/text01.txt" 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java
p4 submit -d "feature 112 GetOpenedFilesTest 8b"
p4 edit 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java
cp "$SOURCE_ROOT/text/text02.txt" 112Dev/GetOpenedFilesTest/src/com/perforce/p4cmd/P4CmdLogListener.java
p4 submit -d "feature 112 GetOpenedFilesTest 8c"

mkdir -p 112Dev/xbin
cp "$SOURCE_ROOT/bin/bin00.bin" 112Dev/xbin/eclipse.exe
p4 add -t xbinary 112Dev/xbin/eclipse.exe
p4 submit -d "feature 112 binary type 1"
cp "$SOURCE_ROOT/bin/bin00.bin" 112Dev/xbin/eclipse1175795839.exe
p4 add -t xbinary 112Dev/xbin/eclipse1175795839.exe
p4 submit -d "feature 112 binary type 2"
p4 delete 112Dev/xbin/eclipse1175795839.exe
p4 submit -d "feature 112 binary type 3"



# Job035290Test:
#   requires file at revision 4.
mkdir -p 92bugs/Job035290Test

cp "$SOURCE_ROOT/text/text00.txt" 92bugs/Job035290Test/Job035290TestNew.txt
p4 add 92bugs/Job035290Test/Job035290TestNew.txt
p4 submit -d 'Job035290Test: revision 1'
p4 edit 92bugs/Job035290Test/Job035290TestNew.txt
cp "$SOURCE_ROOT/text/text01.txt" 92bugs/Job035290Test/Job035290TestNew.txt
p4 submit -d 'Job035290Test: revision 2'
p4 edit 92bugs/Job035290Test/Job035290TestNew.txt
cp "$SOURCE_ROOT/text/text00.txt" 92bugs/Job035290Test/Job035290TestNew.txt
p4 submit -d 'Job035290Test: revision 3'
p4 edit 92bugs/Job035290Test/Job035290TestNew.txt
cp "$SOURCE_ROOT/text/text01.txt" 92bugs/Job035290Test/Job035290TestNew.txt
p4 submit -d 'Job035290Test: revision 4'

echo "Testing Job035290Test compatibility..."
p4 -Ztag files //depot/92bugs/Job035290Test/Job035290TestNew.txt#4



# Job036949Test
#   requires annotation output with last line of "(no newline)"
#   can't figure out how to get it to do that.  Note that explicitly
#   adding that line still has the diff terminate with a newline.
mkdir -p 92bugs/Job036949Test
echo -n 'line 1
line 2
line 3
' > 92bugs/Job036949Test/annotatetest.txt
p4 add 92bugs/Job036949Test/annotatetest.txt
p4 submit -d 'Job036949Test: added file'
p4 edit 92bugs/Job036949Test/annotatetest.txt
echo -n 'line 1
line 2
line 99
(no newline)' > 92bugs/Job036949Test/annotatetest.txt
p4 submit -d 'Job036949Test: altered file'

echo "Testing Job036949Test compatibility..."
p4 annotate //depot/92bugs/Job036949Test/annotatetest.txt


# Job043500Test
#   performs a copy + submit, so the target must exist.
mkdir -p 111bugs/Bugs111_Job043500Test/src
cp "$SOURCE_ROOT/text/text01.txt" 111bugs/Bugs111_Job043500Test/src/test01.txt
mkdir -p 111bugs/Bugs111_Job043500Test/tgt
cp "$SOURCE_ROOT/text/text01.txt" 111bugs/Bugs111_Job043500Test/tgt/test01.txt
p4 add 111bugs/Bugs111_Job043500Test/src/test01.txt 111bugs/Bugs111_Job043500Test/tgt/test01.txt
p4 submit -d "Job043500Test: new files"


# Job040703Test - needs at least changes with jobs
echo 'Change: new
Client: '$P4CLIENT'
User: '$P4USER'
Status: new
Jobs:
    job000001
Description:
    test Job040703Test jobs; Job040649: required files
' > /tmp/ch1.spec
job040703_change1=$( p4 change -i < /tmp/ch1.spec | cut -f 2 -d ' ' )


# Job040649
mkdir -p 101Bugs/Bugs101_Job040649Test
cp "$SOURCE_ROOT/text/text01.txt" 101Bugs/Bugs101_Job040649Test/test01.txt
cp "$SOURCE_ROOT/text/text02.txt" 101Bugs/Bugs101_Job040649Test/test02.txt
p4 add -c "${job040703_change1}" 101Bugs/Bugs101_Job040649Test/test01.txt 101Bugs/Bugs101_Job040649Test/test02.txt
p4 submit -c "${job040703_change1}"



# Job040703Test - needs at least changes with jobs
echo 'Change: new
Client: '$P4CLIENT'
User: '$P4USER'
Status: new
Jobs:
    job000002
Description:
    test Job040703Test jobs; Job040877Test: required files
' > /tmp/ch2.spec
job040703_change2=$( p4 change -i < /tmp/ch2.spec | cut -f 2 -d ' ' )


# Job040877Test
mkdir -p 101Bugs/Bugs101_Job040877Test
cp "$SOURCE_ROOT/text/text01.txt" 101Bugs/Bugs101_Job040877Test/test01.txt
p4 add -c "${job040703_change2}" 101Bugs/Bugs101_Job040877Test/test01.txt
p4 submit -c "${job040703_change2}"


# ReconcileWorkspaceFilesTest
mkdir -p reconcile
( cd "${SOURCE_ROOT}/text" && zip -9r /tmp/client/p4TestUserWS/reconcile/TestFramework.zip * )
p4 add reconcile/TestFramework.zip
p4 submit -d "ReconcileWorkspaceFilesTest: add zip file for tests"


# Job040762Test - requires an exact changelist for the edit.  Original number is 6421.
mkdir -p 101Bugs/Bugs101_Job040762Test
cp "$SOURCE_ROOT/text/text01.txt" 101Bugs/Bugs101_Job040762Test/test01.txt
p4 add 101Bugs/Bugs101_Job040762Test/test01.txt
p4 submit -d "Job040762Test: add file"
p4 edit 101Bugs/Bugs101_Job040762Test/test01.txt
cp "$SOURCE_ROOT/text/text02.txt" 101Bugs/Bugs101_Job040762Test/test01.txt
res=$( p4 submit -d "Job040762Test: add file at exact changelist 33" )
res_cl=$( echo "${res}" | sed -E '{ N; s/Change ([0-9]+) submitted\./\1/ ; D }' )
test "${res_cl}" = "33"


# FileActionReplacedTest
mkdir -p 112Dev/testreplacing1
cp "$SOURCE_ROOT/text/text00.txt" 112Dev/testreplacing1/testfile1.txt
mkdir -p 112Dev/testreplacing2
cp "$SOURCE_ROOT/text/text01.txt" 112Dev/testreplacing2/testfile1.txt
p4 add 112Dev/testreplacing1/testfile1.txt 112Dev/testreplacing2/testfile1.txt
p4 submit -d "FileActionReplacedTest: add files"
