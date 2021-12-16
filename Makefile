.PHONY: all build build-docker clean

all: build build-docker

docker: build-docker run-docker

build:
	gradle test buildJar

build-docker:
	docker image build -t document-service .

run-docker:
	docker run -p 2115:2115 document-service

clean:
	rm -rf .gradle build document-service.jar

start:
	java -jar document-service.jar

watch:
	gradle clean test --continuous
