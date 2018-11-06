#!/usr/bin/env bash


if [ -z "$SWARM_VERSION" ]; then
    SWARM_VERSION=9.1
fi

# Version number
case "$SWARM_VERSION" in
    6)
        SWARM_URL=http://ftp.perforce.com/perforce/r17.1/bin.linux26x86_64/swarm.tgz
        ;;
    7)
        SWARM_URL=http://ftp.perforce.com/perforce/r17.3/bin.linux26x86_64/swarm.tgz
        ;;
    8)
        SWARM_URL=http://ftp.perforce.com/perforce/r17.4/bin.linux26x86_64/swarm.tgz
        ;;
    9)
        SWARM_URL=http://ftp.perforce.com/perforce/r18.1/bin.linux26x86_64/swarm.tgz
        ;;
    9.1)
        SWARM_URL=http://ftp.perforce.com/perforce/r18.2/bin.linux26x86_64/swarm.tgz
esac

if [ -z "$SWARM_URL" ]; then
    echo "Swarm version $SWARM_VERSION is not available for download."
    exit 1
fi

P4D_PLATFORM=linux26x86_64
if [ -z "$P4D_VERSION" ]; then
    P4D_VERSION=r18.1
fi

cd $(dirname "$0") || exit 1
cp ../../../p4java/src/test/resources/bin/$P4D_VERSION/bin.$P4D_PLATFORM/p4d p4d/p4d.bin || exit 1

if [ ! -f "swarm-$SWARM_VERSION.tgz" ]; then
    curl -L -o "swarm-$SWARM_VERSION.tgz" "$SWARM_URL" || exit 1
fi
test -f swarm/swarm.tgz && rm swarm/swarm.tgz
cp "$(pwd)/swarm-$SWARM_VERSION.tgz" swarm/swarm.tgz

( cd p4d && docker build -t local/p4d . ) || exit 1
( cd swarm && docker build -t local/swarm . ) || exit 1
