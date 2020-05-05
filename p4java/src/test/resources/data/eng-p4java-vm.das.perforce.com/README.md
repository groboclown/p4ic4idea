# About

A locally hosted depot version of what the integration tests run against.  These are intended to run against the Perforce servers internal to the Perforce company, but as we don't have access to those servers, this is the placeholder. 

## Double Check

When you work on creating this, follow this checklist:

* `Host:` line in each client is set to an empty value.
* `Root:` line is a non-descriptive directory (say, `/tmp/client`)
* Each user has a password set, which is equal to the user name.


## Construction

If this depot needs updates, follow these steps.

First, create a docker server (or run locally, you know, if you want).  For tests, we need an audit log and a log.

```bash
docker run -it --rm --name p4d -p 11666:1666 -v /tmp/p4d-depot:/opt/p4d/depot ubuntu
(now inside the docker container)
apt-get update && apt-get -y upgrade && apt-get -y install wget
cd /tmp/p4d-depot
wget http://ftp.perforce.com/perforce/r17.1/bin.linux26x86_64/p4d
chmod +x p4d
./p4d -A audit.log -L p4d.log
```

Then, in another shell, setup your environment for manipulation.

```bash
export P4ENVIRO=
export P4PORT=localhost:11666
export P4CLIENT=p4TestUserWS (or whatever is required by the test)
export P4USER=p4jtestuser
export P4PASSWD=p4jtestuser
```

(Super user is `p4jtestsuper`, normal user is `p4jtestuser`)

or for Windows:

```cmd
set P4ENVIRO=
set P4PORT=localhost:11666
set P4CLIENT=p4TestUserWSNT
set P4USER=p4jtestuser
set P4PASSWD=p4jtestuser
```

To package this up, run this in another shell:

```bash
cd (this data resource directory)
_here=$( pwd )

rm -f /tmp/depot/checkpoint.* ; \
docker exec -it p4d /bin/sh -c "cd /opt/p4d/depot && ./p4d -jc" && \
rm -f /tmp/depot/checkpoint.*.md5 ; \
mv /tmp/depot/checkpoint.* checkpoint ; \
rm depot.tar.gz ; \
( cd /tmp/depot/ && tar zcf "$_here"/depot.tar.gz depot p4java_stream ) ; \
rm -f checkpoint.gz ; \
gzip -9 checkpoint
```
