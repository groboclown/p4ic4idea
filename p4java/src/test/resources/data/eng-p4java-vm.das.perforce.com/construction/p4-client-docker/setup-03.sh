#!/bin/bash

set -e

export P4USER=p4jtestsuper
export P4PASSWD=p4jtestsuper

# PasswordTest ...
echo 'User: testuser-job059485
Email: testuser-job059485@nowhere
FullName: testuser-job059485
' | p4 user -i -f

# Password with 2 tabs at the start and 2 spaces at the end
p4 passwd -P $'\t\tabc123  ' testuser-job059485

# Test out the password using an alternate approach
export P4USER=testuser-job059485
P4PASSWD=$( echo -e '\t\tabc123  ' )
export P4PASSWD
p4 users > /dev/null
