FROM alpine:3.8

VOLUME /opt/perforce
EXPOSE 1666

ENV P4JOURNAL /opt/perforce/journal
ENV P4LOG /opt/perforce/p4.log
ENV P4PORT ssl:1666
ENV P4ROOT /opt/perforce/depot.d
ENV P4SSLDIR /opt/perforce/ssl.d

ADD p4d.bin /usr/local/sbin/

# TODO libs are wrong for alpine + p4d:
# $ ldd /usr/local/sbin/p4d.bin
#         /lib64/ld-linux-x86-64.so.2 (0x7f6bda4a2000)
#         librt.so.1 => /lib64/ld-linux-x86-64.so.2 (0x7f6bda4a2000)
#         libdl.so.2 => /lib64/ld-linux-x86-64.so.2 (0x7f6bda4a2000)
#         libm.so.6 => /lib64/ld-linux-x86-64.so.2 (0x7f6bda4a2000)
#         libpthread.so.0 => /lib64/ld-linux-x86-64.so.2 (0x7f6bda4a2000)
#         libc.so.6 => /lib64/ld-linux-x86-64.so.2 (0x7f6bda4a2000)
# Error relocating /usr/local/sbin/p4d.bin: __rawmemchr: symbol not found
# Error relocating /usr/local/sbin/p4d.bin: sys_errlist: symbol not found
# Error relocating /usr/local/sbin/p4d.bin: sys_nerr: symbol not found
# Error relocating /usr/local/sbin/p4d.bin: __morecore: symbol not found

RUN chmod +x /usr/local/sbin/p4d.bin && \
    ( \
        test "$P4PORT" == ssl:1666 && ( \
            mkdir "$P4SSLDIR" && \
            chmod 700 $P4SSLDIR && \
            /usr/local/sbin/p4d.bin -Gc ) \
        || echo "No SSL" \
    )

WORKDIR /opt/perforce

# ENTRYPOINT /usr/local/sbin/p4d.bin -v 3
CMD /bin/sh
