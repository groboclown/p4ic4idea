#!/bin/sh

cd /opt/perforce

P4D=/usr/local/sbin/p4d.bin

if [ ! -d "$P4ROOT" ]; then
    mkdir -p "$P4ROOT" || exit 1
fi
if [ ! -d "$P4ROOT/HR" -a -f /opt/sampledepot.tar.gz ]; then
    ( cd /tmp && tar zxf /opt/sampledepot.tar.gz ) || exit 1
    rm -r "$P4ROOT"/* || exit 1
    mv /tmp/PerforceSample/* "$P4ROOT" || exit 1
    $P4D -jr "$P4ROOT"/checkpoint || exit 1
    $P4D -xu || exit 1
fi

if [ "$P4PORT" = "ssl:1666" ]; then
    if [ ! -d "$P4SSLDIR" ]; then
            mkdir -p "$P4SSLDIR" || exit 1
            chmod 700 $P4SSLDIR || exit 1
            $P4D -Gc || exit 1
    fi
fi

exec $P4D -v 3
