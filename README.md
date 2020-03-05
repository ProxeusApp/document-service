![](doc/logo.png)

# What is it?
Short explanation:
`Template` + `JSON` = `Document` with the format `PDF`, `ODT`, `DOCX` or `DOC`.

![](doc/what_is_it.png)

## Technical Overview
The template language is [[jtwig]](https://github.com/jtwig/jtwig-core), it should be a piece of cake for all who are a little bit familiar with `django`, `php-twig` or similar.
The code is directly written as content and can be styled how ever you like, to keep your templates readable.
The `Document-Service` is designed to support any document format based on `XML`. It is using the `XML` fundamentals to separate code from the actual content and to place it meaningful in the `XML` structure. So we can produce the expected document result.

![](doc/Concept_Architecture_Doc.png)

Currently supported template format is `ODT`. Other formats like `DOCX` are not ready yet.

## Install, Build and Run
#### Prerequisites
+ JDK 8
+ gradle
+ docker

#### Building for development
```
 gradle buildJar
 sudo docker image build -f Dockerfile.dev -t document-service .
 
 #run 2115 for the document-service and 58082 for the UI in case it is needed
 sudo docker run -p 2115:2115 -p 58082:58082 document-service
 
 #single cmd
 gradle buildJar && sudo docker image build -t document-service -f ./Dockerfile.dev . && sudo docker run -p 2115:2115 -p 58082:58082 document-service

 #for removing all dockers you can run
 sudo ./rmalldockers.sh
```
To run a development environment use this repository: https://git.proxeus.com/docker/proxeus-platform

#### Building for production
```
gradle buildJar
sudo docker image build -t document-service -f ./Dockerfile .
```

## Commandline client
```
#compile usage:
./dsclient -t tmpl.odt -o my.pdf
./dsclient -u http://123.12.12.111:8888 -f pdf -t my/tmpl.odt -d data.json -a myImage.png -a my/images/dir -o output.pdf 
#print vars usage:
./dsclient -m vars -t my/tmpl.odt -p input.

  -a value
        asset files, provide directory or file like -a file1 -a file2 -a dir1
  -d string
        JSON file path
  -e    embed compilation error into the returned document
  -f string
        result format, possible values: pdf, odt, docx or doc (default "pdf")
  -m string
        compile or vars (default "compile")
  -o string
        output path, extension will be attached if not provided (default "result")
  -p string
        var prefix to filter vars
  -t string
        ODT template path
  -u string
        Document-Service URL (default "http://localhost:2115")

```

## Playground UI
If you want to get familiar with the Document-Service, you should start the docker with `-p 58082:58082` to expose the UI.
The UI provides access to the API, documentation and examples. You can play around with all available methods.
It is recommended to disable the UI under production.

## API
The API documentation can be accessed by `<host>:<port>/api` or over the playground UI on the top right corner.

Here a simple overview:

![](doc/Concept_Architecture.png)

If you are looking for an example implementation of the API please checkout the client source [[here]](client/document_service_client.go).

## License

See the LICENSE file
