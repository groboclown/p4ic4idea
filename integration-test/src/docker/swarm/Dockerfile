FROM frolvlad/alpine-glibc:alpine-3.8_glibc-2.28

# Include the glibc libraries.

EXPOSE 80

VOLUME /opt/swarm/data


ENV P4PORT ssl:perforce:1666

# Uncompresses the tarballl into /opt, with the
# swarm version name as part of the directory.
ADD swarm.tgz /opt

ADD setup-p4-sso.sh /opt/
ADD start-apache.sh /opt/
ADD p4.bin /usr/local/bin/

# Note: Alpine's "php7" is NOT php 7.0, so it will not work with
# the Swarm shared library (its php70 is compiled against php 7.0).
RUN apk --no-cache update && \
    ln -s /usr/local/bin/p4.bin /usr/local/bin/p4 && \
    cp -R /opt/swarm-*/* /opt/swarm/. && \
    apk --no-cache add \
        dos2unix \
        curl \
        apache2 \
        libstdc++ \
        php5-apache2 \
        php5-dom \
        php5-bz2 \
        php5-zip \
        php5-json \
        php5-ctype \
        php5-iconv \
        php5-bcmath \
        php5-mcrypt \
        php5-xmlrpc \
        php5-openssl \
        php5-gettext \
        php5-xmlreader \
        && \
    echo '[PHP]' >> /etc/php5/php.ini && \
    echo 'extension=/opt/swarm/p4-bin/bin.linux26x86_64/perforce-php56.so' >> /etc/php5/php.ini && \
    mkdir /run/apache2 && \
    dos2unix \
        /opt/setup-p4-sso.sh \
        /opt/start-apache.sh && \
    chmod +x \
        /opt/setup-p4-sso.sh \
        /opt/start-apache.sh \
        /usr/local/bin/p4.bin


ADD httpd.conf /etc/apache2/
ADD swarm.conf /etc/apache2/conf.d/
# The config.php file should exist in /opt/swarm/data, but that's a mounted volume, so
# it must be copied over every time.
ADD config.php /opt/
ADD phpinfo.php /opt/swarm/public/

# Apache in debug mode: do not detach, only one worker.  Perfect for testing apache in a docker container.
#CMD [ "/opt/start-apache.sh" ]

# Default: Exploratory mode
CMD /bin/sh
