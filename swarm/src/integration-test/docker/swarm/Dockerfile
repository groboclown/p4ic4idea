FROM alpine:3.8

EXPOSE 80

VOLUME /opt/swarm/data

# Uncompresses the tarballl into /opt, with the
# swarm version name as part of the directory.
ADD swarm.tgz /opt

RUN cp -R /opt/swarm-*/* /opt/swarm/. \
    && apk --no-cache add apache2 php7-apache2 \
    && mkdir /run/apache2

ADD httpd.conf /etc/apache2/
# ADD default.conf /etc/apache2/conf.d/
ADD swarm.conf /etc/apache2/conf.d/
ADD php.ini /etc/php7/
ADD config.php /opt/swarm/data/
ADD phpinfo.php /opt/swarm/public/

# Debug Mode: do not detach, only one worker.
#ENTRYPOINT [ "/usr/sbin/httpd", "-X" ]

# Exploratory mode
CMD /bin/sh
