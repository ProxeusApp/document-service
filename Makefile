.PHONY: all build build-docker clean clean-docker

all: build build-docker

build:
	gradle buildJar
build-docker:
	docker image build -t document-service .
clean:
	rm -rf .gradle build document-service.jar
clean-docker:
	./rmalldockers.sh
