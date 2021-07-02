FROM fedora:33

ENV LC_ALL=C KUBECTL_VERSION=v1.21.2

COPY scripts/ /usr/local/bin/

RUN install-kubectl

ENTRYPOINT [ "/usr/local/bin/entrypoint" ]
