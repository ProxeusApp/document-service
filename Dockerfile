FROM gradle:jdk11-alpine
EXPOSE 2115
ENV LANG="en_US.UTF-8"

# Install packages
RUN apk update
RUN set -xe \
    && apk add --no-cache --purge -uU \
        curl icu-libs unzip zlib-dev musl \
        mesa-gl mesa-dri-swrast \
        libreoffice-common=6.4.6.2-r11 \
        libreoffice-writer=6.4.6.2-r11 \
        libreofficekit=6.4.6.2-r11 \
        ttf-freefont ttf-opensans ttf-inconsolata \
	      ttf-liberation ttf-dejavu \
        libstdc++ dbus-x11 \
    && rm -rf /var/cache/apk/* /tmp/*

# Custom font configuration
COPY ./00-fontconfig.conf /etc/fonts/conf.d/

# Create working folders
COPY --chown=gradle:gradle . /home/gradle/project/
WORKDIR /home/gradle/project
RUN mkdir -p /document-service/fonts /document-service/logs

# Build and install binary
RUN gradle clean buildJar --no-daemon --quiet
COPY document-service.jar /document-service/

# Run service
CMD ["java", "-jar", "/document-service/document-service.jar"]
