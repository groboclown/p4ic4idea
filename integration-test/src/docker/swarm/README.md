# About

A docker configuration that can run Perforce Swarm server, as well as any Perforce command line operation against the
dockerized Perforce server.

This is designed to work with the sibling p4d docker container.  See the [docker-compose.yml](../docker-compose.yml)
file for setting up the two docker containers to work in conjunction.

# Run Swarm

Run with the command `[ "/opt/start-apache.sh" ]`

# Server Setup Commands

The image also comes with scripts so you can perform easy operations to configure the server.

## Setup Perforce Server as SSO

`/opt/setup-p4-sso.sh`

Creates a trigger on the server that invokes `server-sso-script.sh` on login.
