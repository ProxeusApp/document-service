FROM gradle:jdk11-alpine
EXPOSE 2115
ENV LANG="en_US.UTF-8"

# Build binary
COPY --chown=gradle:gradle . /home/gradle/project/
WORKDIR /home/gradle/project
RUN gradle buildJar --no-daemon --quiet

# Install binary
RUN mkdir /document-service /document-service/fonts /document-service/logs
COPY document-service.jar /document-service/

# Install packages
RUN apk update
RUN set -xe \
    && apk add --no-cache --purge -uU \
        curl icu-libs unzip zlib-dev musl \
        mesa-gl mesa-dri-swrast \
        libreoffice=6.4.6.2-r11 \
        ttf-freefont ttf-opensans ttf-inconsolata \
	      ttf-liberation ttf-dejavu \
        libstdc++ dbus-x11 \
    && rm -rf /var/cache/apk/* /tmp/*

# TODO: libreoffice-common + libs for headless mode

# Custom font configuration
COPY ./00-fontconfig.conf /etc/fonts/conf.d/

# Run service
CMD ["java", "-jar", "/document-service/document-service.jar"]
