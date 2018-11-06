FROM alpine:3.8

VOLUME /opt/perforce
EXPOSE 1666

ENV P4JOURNAL /opt/perforce/journal
ENV P4LOG /opt/perforce/p4.log
ENV P4PORT 1666
ENV P4ROOT /opt/perforce/depot_root

ADD p4d.bin /usr/local/sbin/

WORKDIR /opt/perforce

ENTRYPOINT /usr/local/sbin/p4d.bin
