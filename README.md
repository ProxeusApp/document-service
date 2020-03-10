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
You can simply interact with the server using `curl`.


```
# To compile a template to pdf (pdf is the default)

curl --form template=@template.odt --form data=@data.json http://<server>/compile > result.pdf

# To compile a template to odt (available format are pdf, odt, docx or doc) 

curl --form template=@template.odt --form data=@data.json http://<server>/compile?format=odt > result.pdf

# To embed the template rendering error in the pdf result (add the `error` query parameter 

curl --form template=@template.odt --form data=@data.json http://<server>/compile?error > result.pdf

# To get the variables used in a template

curl --data-binary @template.odt http://<server>/vars
curl --form template=@template.odt  http:/<server>/vars 

# To get the subset of the variable starting with a given prefix 

curl --data-binary @template.odt http://<server>/vars?prefix=foo
curl --form template=@template.odt  http:/<server>/vars?prefix=bar 

# To add asset files
 
curl --form template=@template.odt --form data=@data.json --form asset1=@asset1.png http://<server>/compile > result.pdf

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

Document Service is licensed under @BlockFactory AG.
