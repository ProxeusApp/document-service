.PHONY: all build build-docker clean

all: test build build-docker

docker: build-docker run-docker

test:
	gradle test

build:
	gradle buildJar

build-docker:
	docker image build --ulimit nofile=65536:65536 -t document-service .

copy-docker:
	docker cp `docker ps -alq`:/document-service/document-service.jar .

run-docker:
	docker run -p 2115:2115 document-service

test-docker:
	docker run document-service gradle test --no-daemon

publish-docker:
	docker tag document-service proxeus/document-service:latest
	docker push proxeus/document-service

clean:
	rm -rf .gradle build document-service.jar

start:
	java -jar document-service.jar

watch:
	gradle clean test --continuous
