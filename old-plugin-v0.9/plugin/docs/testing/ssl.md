# Testing SSL Connections

## Setup P4D for ssl

### Generate SSL keys

```(bash)
$ export P4SSLDIR=$(pwd)/ssl.d
$ mkdir -p $P4SSLDIR
$ chmod 700 $P4SSLDIR
$ ./p4d -Gc
```

### Run P4D with SSL

```(bash)
$ export P4SSLDIR=$(pwd)/ssl.d
$ ./p4d -v 3 -p ssl:1666
```


## Client Setup

### Setup Client and Trust the server

```(bash)
$ export P4USER=user
$ export P4PORT=ssl:localhost:1666
$ export P4PASSWD=xyz
$ p4 trust
$ p4 user user
$ p4 passwd
```
