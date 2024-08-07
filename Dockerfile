FROM gradle:jdk11-alpine
EXPOSE 2115
ENV LANG="en_US.UTF-8"
USER root

# Install packages
RUN apk update
RUN set -xe \
    && apk add --no-cache --purge -uU \
        curl icu-libs unzip zlib-dev musl \
        mesa-gl \
        libreoffice-writer \
        libreofficekit \
        ttf-freefont ttf-opensans ttf-inconsolata \
	      ttf-liberation ttf-dejavu \
        libstdc++ dbus-x11 \
    && rm -rf /var/cache/apk/* /tmp/*

# Custom font configuration
COPY ./00-fontconfig.conf /etc/fonts/conf.d/

# Create working folders
RUN mkdir -p /document-service/fonts /document-service/logs
COPY --chown=gradle:gradle . /home/gradle/project/
WORKDIR /home/gradle/project

# Build and install binary
RUN gradle clean buildJar --no-daemon --quiet
RUN cp ./document-service.jar /document-service/

# Run service
# * add-opens: https://github.com/ProxeusApp/document-service/issues/23#issuecomment-996166084
CMD ["java", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", "/document-service/document-service.jar"]
