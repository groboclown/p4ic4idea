#!/bin/bash

set -e

# Changelist # specific stuff.  At the end to not upset the other stuff.

export P4USER=p4jtestsuper
export P4PASSWD=p4jtestsuper

# Job040762Test
# Needs changelist 6421 diffed against head for //depot/101Bugs/Bugs101_Job040762Test
# looks for file //depot/101Bugs/Bugs101_Job040762Test/test01.txt
