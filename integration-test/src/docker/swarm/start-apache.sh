#!/bin/sh

test -f /opt/swarm/data/p4trust && rm /opt/swarm/data/p4trust
test -f /run/apache2/httpd.pid && rm /run/apache2/httpd.pid
cp /opt/config.php /opt/swarm/data
chmod -R g+rw /opt/swarm/data
chmod -R a+rw /opt/swarm/data

# Debug mode:
# /usr/sbin/httpd -X

/usr/sbin/httpd || exit 1

# Cron job to run workers.
while [ 1 = 1 ]; do
    curl --silent --insecure --output /dev/null --max-time 5 "http://localhost/queue/worker"
    sleep 5
done
