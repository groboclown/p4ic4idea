#!/bin/bash

# Run this script with a brand new P4 server root directory.
port=1666
export P4CLIENT=.p4client

mkdir project1-c1
mkdir project2-c2
mkdir -p allprojects-client/p1
mkdir -p allprojects-client/p2
mkdir -p allprojects-client/p2/integration/c3

# ----------------------------------------------------------------------------
pushd project1-c1
echo 'P4USER=user1
P4PORT='$port'
P4CLIENT=project1-c1
' > .p4config
echo 'Client: project1-c1

Owner: user1

Host:

Description:
	Client 1

Root: '$(pwd)'

Options: noallwrite noclobber nocompress unlocked nomodtime rmdir

SubmitOptions: submitunchanged

LineEnd: local

View:
	//depot/project1/... //project1-c1/...
' | p4 client -i
mkdir -p src/main
echo '#!/usr/bin/python' > src/main/main.py
p4 add src/main/main.py




popd
# ----------------------------------------------------------------------------
pushd project2-c1
echo 'P4USER=user1
P4PORT='$port'
P4CLIENT=project2-c1
' > .p4config




popd
# ----------------------------------------------------------------------------
pushd allprojects-client/c1
echo 'P4USER=user1
P4PORT='$port'
P4CLIENT=project2-c1
' > .p4config


popd
# ----------------------------------------------------------------------------
pushd allprojects-client/c2
echo 'P4USER=user1
P4PORT='$port'
P4CLIENT=project2-c1
' > .p4config


popd
# ----------------------------------------------------------------------------
pushd allprojects-client/p2/integration/c3
echo 'P4USER=user1
P4PORT='$port'
P4CLIENT=project3
' > .p4config





popd
