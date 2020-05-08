#!/bin/bash -x

set -e

# Setup users, protections, and clients.

export P4USER=p4jtestsuper
export P4PASSWD=p4jtestsuper

p4 protect -i protect-specs/protect.spec

for i in user-specs/*.spec ; do
  username=$( basename "${i}" .spec )
  p4 user -f -i < "${i}"
  p4 passwd -P "${username}" "${username}"
done

for i in depot-specs/*.spec ; do
  p4 depot -i < "${i}"
done

for i in stream-specs/*.spec ; do
  p4 stream -i < "${i}"
done

for i in client-specs/*.spec ; do
  p4 client -i < "${i}"
done

for i in job-specs/*.spec ; do
  p4 job -i < "${i}"
done

for i in branch-specs/*.spec ; do
  p4 branch -i < "${i}"
done

for i in group-specs/*.spec ; do
  p4 group -i < "${i}"
done

# explicit counters at initialization time.
p4 counter -f job 100
