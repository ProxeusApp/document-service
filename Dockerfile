FROM gradle:jdk8 as build

COPY --chown=gradle:gradle . /home/gradle/project/
WORKDIR /home/gradle/project
RUN gradle clean test buildJar --no-daemon

FROM ubuntu:18.04
EXPOSE 2115
ENV LANG="en_US.UTF-8"

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
COPY --from=build /home/gradle/project/document-service.jar /document-service/

CMD ["/usr/bin/java", "-jar", "/document-service/document-service.jar"]
