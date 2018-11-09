#!/bin/sh

test -f /run/apache2/httpd.pid && rm /run/apache2/httpd.pid
cp /opt/config.php /opt/swarm/data
chmod -R g+rw /opt/swarm/data
chmod -R a+rw /opt/swarm/data
exec /usr/sbin/httpd -X
