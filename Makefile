.PHONY: all build build-docker clean clean-docker

all: build build-docker run-docker

build:
	gradle buildJar
build-docker:
	docker image build -f Dockerfile.dev -t document-service .
clean:
	rm -rf .gradle build document-service.jar
clean-docker:
	./rmalldockers.sh
