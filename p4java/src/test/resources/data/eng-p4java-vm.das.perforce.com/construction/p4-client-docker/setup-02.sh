#!/bin/sh

set -e

# Stream depot files.

export P4USER=p4jtestuser
export P4PASSWD=p4jtestuser

export P4CLIENT=p4java_stream_dev

# hard-coded root at /tmp/client/p4java_stream_dev

mkdir -p /tmp/client/p4java_stream_dev
cd /tmp/client/p4java_stream_dev
p4 sync ...

cp "$SOURCE_ROOT/text/text00.txt" ./text00.txt
p4 add text00.txt
p4 submit -d 'Job035290Test: revision 1'



# Now merge the files into main.
export P4CLIENT=p4java_stream_main
mkdir -p /tmp/client/p4java_stream_main
cd /tmp/client/p4java_stream_main
p4 copy //p4java_stream/dev/... ...
p4 submit -d 'initial population of main'
