#!/usr/bin/env sh

# Script run by Perforce server trigger to authenticate SSO logins.

client_username="$1"
read client_auth_code < /dev/stdin
if [ -z "$client_auth_code" ]; then
    echo "No authentication."
    exit 1
fi
if [ "$client_auth_code" = "sso pass" ]; then
    exit 0
fi
echo "Unauthorized"
exit 1
