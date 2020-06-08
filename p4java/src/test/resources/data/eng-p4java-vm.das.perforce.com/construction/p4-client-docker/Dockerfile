FROM ubuntu:latest

RUN echo "Start install" \
    && apt-get update \
    && apt-get -y upgrade \
    && apt-get -y install wget zip \
    && mkdir -p /opt/p4d-base \
    && cd /tmp \
    && wget http://ftp.perforce.com/perforce/r17.1/bin.linux26x86_64/p4 \
    && mv /tmp/p4 /usr/local/bin/. \
    && chmod +x /usr/local/bin/p4 \
    && echo "Completed install"

COPY . /opt/client/
COPY docker-entrypoint.sh /

RUN echo "Setting up files" \
    && chmod +x /opt/client/*.sh \
    && mkdir -p /opt/client/bin \
    && cd /opt/client/bin \
    && cp /opt/client/text/text00.txt /opt/client/text/text01.txt /opt/client/text/text02.txt . \
    && gzip text00.txt \
    && mv text00.txt.gz bin00.bin \
    && gzip text01.txt \
    && mv text01.txt.gz bin01.bin \
    && gzip text02.txt \
    && mv text02.txt.gz bin02.bin \
    && echo "Completed setup"

ENV P4PORT perforce.local:1666

WORKDIR /opt/client

CMD [ "/bin/bash", "/docker-entrypoint.sh" ]
