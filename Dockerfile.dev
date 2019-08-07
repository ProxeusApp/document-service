FROM ubuntu:18.04

## Fix installation of openjdk-8-jre-headless (https://github.com/nextcloud/docker/issues/380)
RUN mkdir -p /usr/share/man/man1
RUN apt-get update && apt-get install -y \
        software-properties-common \
        language-pack-en-base \
        openjdk-8-jre-headless \
        libreoffice \
    && apt-get clean && rm -rf /var/cache/* /var/lib/apt/lists/*

#font configuration
COPY ./00-fontconfig.conf /etc/fonts/conf.d/

RUN mkdir /document-service /document-service/fonts /document-service/logs

COPY ./document-service.jar /document-service/
#COPY ./config.json /document-service/
COPY ./ui_service /document-service/
COPY ./run /document-service/

RUN chmod +x /document-service/ui_service
RUN chmod +x /document-service/run

ENV LANG="en_US.UTF-8"

WORKDIR /document-service

#document-service
EXPOSE 2115
#UI
EXPOSE 58082

CMD ["./run"]
