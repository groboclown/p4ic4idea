#!/bin/bash

# Wait up to 120 seconds for the Perforce server to be available.

echo "Using Perforce server ${P4PORT}"
ec=0
p4d_up=no
while [ ${ec} -lt 12 ] && [ ${p4d_up} = no ] ; do
  p4 info >/dev/null 2>&1
  if [ $? = 0 ] ; then
    p4d_up=yes
  else
    echo "Waiting 10 seconds for server to come up..."
    ec=$(( ec + 1 ))
    sleep 10
  fi
done
if [ ${p4d_up} = no ] ; then
  echo "Could not find Perforce server; halting."
  exit 1
fi

export SOURCE_ROOT=/opt/client
for i in /opt/client/setup-*.sh ; do
  "$i" || exit 1
done


