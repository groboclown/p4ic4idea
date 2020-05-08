FROM ubuntu:latest

EXPOSE 1666

VOLUME /opt/p4d-base

RUN echo "Start install" \
    && apt-get update \
    && apt-get -y upgrade \
    && apt-get -y install wget \
    && mkdir -p /opt/p4d-base \
    && cd /tmp \
    && wget http://ftp.perforce.com/perforce/r17.1/bin.linux26x86_64/p4d \
    && mv /tmp/p4d /usr/local/bin/. \
    && chmod +x /usr/local/bin/p4d \
    && echo "Completed install"

WORKDIR /opt/p4d-base

CMD [ "/usr/local/bin/p4d", "-A", "audit.log", "-L", "p4d.log" ]
