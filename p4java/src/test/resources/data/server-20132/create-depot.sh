#!/bin/bash

cd $(dirname $0)
here=$(pwd)
p4c=$(which p4 2>/dev/null)
if [ -z "$p4c" ]; then
    if [ -z "$1" ]; then
        echo "Usage: $0 (p4 location)"
        exit 1
    fi
    p4c="$1"
fi
case "`uname`" in
    Darwin*)
        p4d="$here/../../bin/r17.1/bin.darwin90x86_64"
        ;;
    Linux*)
        p4d="$here/../../bin/r17.1/bin.linux26x86_64"
        ;;
esac

# Initialize server
mkdir "$here/root" || exit 1
"$p4d" -r "$here/root" -C1 -p 20132 -J off &
p4dpid=$!
echo $p4dpid > "$here/p4d.pid"
if [ $( pgrep -c -F "$here/p4d.pid" ) -le 0 ]; then
    rm -r "$here/root"
    rm "$here/p4d.pid"
    exit 2
fi

# Setup users (password is done last)
export P4PORT=localhost:20132
echo 'User: luser
Email: luser@nowhere
FullName: Local User' | "$p4c" -u luser user -i || exit 3
echo 'User: lsuper
Email: lsuper@nowhere
FullName: Local Super User' | "$p4c" -u lsuper user -i || exit 3

# Setup client
mkdir "$here/n" || exit 2
cd "$here/n"
echo "
Client: n
Owner:  luser
Description:
        Created by luser.
Root:   $here/n
Options:        noallwrite noclobber nocompress unlocked nomodtime normdir
SubmitOptions:  submitunchanged
LineEnd:        local
View:
        //depot/... //n/...
" | "$p4c" -u luser client -i || exit 3

# Setup basic data

## Job 38602: Extended ascii file
### TODO what contents are we testing for?  What file type?
mkdir -p "$here/n/depot/101Bugs/Bugs101_Job038602Test" || exit 2
touch "$here/n/depot/101Bugs/Bugs101_Job038602Test/test01.txt" || exit 2
"$p4c" -u luser -c n add depot/101Bugs/Bugs101_Job038602Test/test01.txt || exit 3
"$p4c" -u luser -c n submit -d "job38602 - extended ascii file" || exit 3


# Finally, setup protections and passwords
echo 'protections:
  write user luser *.*.*.* //depot/...
  super user lsuper *.*.*.* //...' | "$p4c" -u lsuper protect -i || exit 3
"$p4c" -u luser client -d n
echo -n 'password1
password1' | "$p4c" -u luser passwd || exit 3
echo -n 'password2
password2' | "$p4c" -u lsuper password || exit 3

# Stop the server and capture the depot
pkill -F "$here/p4d.pid"
( cd "$here/root" && tar zcf "$here/depot.tar.gz" * )

# Clean up
rm -r n root "$here/p4d.pid"
