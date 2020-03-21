.PHONY: all build build-docker clean

all: build build-docker

build:
	gradle test buildJar

build-docker:
	docker image build -t document-service .

clean:
	rm -rf .gradle build document-service.jar
