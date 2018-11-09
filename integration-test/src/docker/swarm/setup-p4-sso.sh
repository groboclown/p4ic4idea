#!/usr/bin/env sh

# Official P4 Sample depot: use user `bruno` with no password.
# All connection settings should be done through environment variables that are
# set outside this script.

export P4PASSWD=
p4 -u bruno triggers -o > /tmp/triggers.txt || exit 1
echo -e "\tsso\tauth-check-sso\tauth\t\"/opt/server-sso-script.sh %user%\"" >> /tmp/triggers.txt
p4 -u bruno triggers -i < /tmp/triggers.txt || exit  1
