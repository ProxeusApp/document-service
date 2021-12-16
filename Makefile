.PHONY: all build build-docker clean

all: test build build-docker

docker: build-docker run-docker

test:
	gradle test

build:
	gradle buildJar

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
