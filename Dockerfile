FROM eu.gcr.io/blockfactory-01/document-service-base

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
