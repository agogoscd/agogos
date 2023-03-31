FROM fedora:37

ENV LC_ALL=C KUBECTL_VERSION=v1.21.2

COPY scripts/ /usr/local/bin/

RUN install-kubectl

USER 31415

ENTRYPOINT [ "/usr/local/bin/entrypoint" ]
