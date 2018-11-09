FROM frolvlad/alpine-glibc:alpine-3.8_glibc-2.28

# P4D requires glibc libraries.

VOLUME /opt/perforce/logs.d
VOLUME /opt/perforce/root.d
EXPOSE 1666

ENV P4JOURNAL /opt/perforce/logs.d/journal
ENV P4LOG /opt/perforce/logs.d/p4.log
ENV P4PORT ssl:1666
ENV P4ROOT /opt/perforce/root.d
ENV P4SSLDIR /opt/perforce/ssl.d

ADD p4d.bin /usr/local/sbin/
ADD p4.bin /usr/local/bin/

COPY sampledepot.tar.gz /opt/
ADD p4d-start.sh /opt/
ADD server-sso-script.sh /opt/

RUN apk --no-cache update && \
    apk --no-cache add \
        dos2unix \
        && \
    dos2unix \
        /opt/p4d-start.sh \
        /opt/server-sso-script.sh && \
    chmod +x \
        /usr/local/sbin/p4d.bin \
        /opt/p4d-start.sh \
        /opt/server-sso-script.sh \
        /usr/local/bin/p4.bin

WORKDIR /opt/perforce

# ENTRYPOINT /usr/local/sbin/p4d.bin -v 3
# ENTRYPOINT /opt/p4d-start.sh
CMD /bin/sh
