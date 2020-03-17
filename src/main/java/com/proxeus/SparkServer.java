package com.proxeus;

import com.proxeus.document.FileResult;
import com.proxeus.document.Template;
import com.proxeus.document.TemplateCompiler;
import com.proxeus.document.TemplateFormatter;
import com.proxeus.error.BadRequestException;
import com.proxeus.error.CompilationException;
import com.proxeus.error.NotImplementedException;
import com.proxeus.error.UnavailableException;
import com.proxeus.office.libre.LibreConfig;
import com.proxeus.office.libre.LibreOfficeAssistant;
import com.proxeus.office.libre.exe.Extension;
import com.proxeus.office.libre.exe.LibreOfficeFormat;
import com.proxeus.util.Json;
import com.proxeus.util.zip.Zip;
import com.proxeus.xml.template.TemplateHandlerFactory;
import com.proxeus.xml.template.TemplateVarParserFactory;
import com.proxeus.xml.template.jtwig.JTwigTemplateHandlerFactory;
import com.proxeus.xml.template.jtwig.JTwigTemplateVarParserFactory;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.eclipse.jetty.io.EofException;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.proxeus.Application.config;
import static org.apache.commons.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static org.apache.commons.fileupload.FileUploadBase.MULTIPART_FORM_DATA;
import static spark.Spark.*;

/**
 * SparkServer defines the protocol of this services.
 */
public class SparkServer {
    private Logger log = Logger.getLogger(this.getClass());

    private TemplateFormatter templateFormatter;
    private TemplateHandlerFactory templateHandlerFactory;
    private TemplateVarParserFactory templateVarParserFactory;
    private TemplateCompiler templateCompiler;

    private final Charset defaultCharset = StandardCharsets.UTF_8;
    private final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    private final String TEXT_CONTENT_TYPE = "text/plain; charset=UTF-8";

    SparkServer(Config config) {
        removeTheDiskCacheOfDocs();
        Logger log = Logger.getLogger(this.getClass());
        threadPool(config.getMax(), config.getMin(), config.getTimeoutMillis());
        port(config.getPort());
        ipAddress(config.getHost());
        try {
            templateFormatter = new LibreOfficeAssistant(Config.by(LibreConfig.class));
            templateHandlerFactory = new JTwigTemplateHandlerFactory();
            templateVarParserFactory = new JTwigTemplateVarParserFactory();
            templateCompiler = new TemplateCompiler(config.getTmpFolder(), templateFormatter, templateHandlerFactory, templateVarParserFactory);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        staticFiles.location("/public");
        get("/api", (request, response) -> {
            api(response);
            return 0;
        });
        get("/", (request, response) -> {
            api(response);
            return 0;
        });
        get("/how-it-works", (request, response) -> {
            try {
                boolean inline = request.queryMap().hasKey("inline");
                response.raw().setContentType(LibreOfficeFormat.PDF.getContentType());
                response.raw().setHeader(CONTENT_DISPOSITION, (inline ? "inline" : "attachment") + "; filename=\"how.it.works.pdf\"");
                streamAndClose(getODTAsPDFFromResources(config, "how.it.works.odt"), response.raw().getOutputStream());
            } catch (Exception e) {
                notFound(response);
            }
            return 0;
        });
        get("/example", (request, response) -> {
            try {
                InputStream is;
                LibreOfficeFormat format;
                if (request.queryMap().hasKey("raw")) {
                    format = LibreOfficeFormat.ODT;
                    is = SparkServer.class.getResourceAsStream("/example/tmpl.odt");
                } else {
                    format = LibreOfficeFormat.PDF;
                    is = getDirAsPDFFromResources(config, "example");
                }
                boolean inline = request.queryMap().hasKey("inline");
                response.raw().setContentType(format.getContentType());
                response.raw().setHeader(CONTENT_DISPOSITION, (inline ? "inline" : "attachment") + "; filename=\"example." + format.getExt() + "\"");
                streamAndClose(is, response.raw().getOutputStream());
            } catch (Exception e) {
                notFound(response);
            }
            return 0;
        });

        // curl --form template=@myfile.odt -data=@data.json -asset1=myasset.jpg http://document-service/compile > myfile.pdf
        post("/compile", (request, response) -> {
            try {
                StopWatch sw = StopWatch.createStarted();
                Template template;
                if (request.contentType().startsWith(MULTIPART_FORM_DATA)) {
                    request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
                    template = Template.fromFormData(request.raw().getParts(), request.queryParams("format"));
                } else {
                    template = Template.fromZip(request.raw().getInputStream(), request.queryParams("format"));
                }
                FileResult result = templateCompiler.compile(template, request.queryMap().hasKey("error"));
                response.header("Content-Type", result.contentType);
                response.header("Content-Length", "" + result.target.length());
                try {
                    streamAndClose(new FileInputStream(result.target), response.raw().getOutputStream());
                } finally {
                    result.release();
                }
                log.info("request took: " + sw.getTime(TimeUnit.MILLISECONDS));
            } catch (EofException | MultipartStream.MalformedStreamException eof) {
                try {
                    response.raw().getOutputStream().close();
                } catch (Exception idc) {
                }
            } catch (CompilationException e) {
                e.printStackTrace();
                error(422, response, e);
            } catch (BadRequestException e) {
                e.printStackTrace();
                error(HttpURLConnection.HTTP_BAD_REQUEST, response, e);
            } catch (NotImplementedException e) {
                error(HttpURLConnection.HTTP_NOT_IMPLEMENTED, response, e);
            } catch (UnavailableException e) {
                error(HttpURLConnection.HTTP_UNAVAILABLE, response, e);
            } catch (Exception e) {
                error(HttpURLConnection.HTTP_INTERNAL_ERROR, response, e);
            }
            return 0;
        });

        get("/extension", (request, response) ->

        {
            try {
                //request.queryParams("app")
                //app is meant for future releases
                //right now there is just libre so we can ignore this param
                Extension extension = templateFormatter.getExtension(request.queryParams("os"));
                response.raw().setContentType(extension.getContentType());
                response.raw().setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + extension.getFileName() + "\"");
                streamAndClose(extension.getInputStream(), response.raw().getOutputStream());
            } catch (Exception e) {
                notFound(response);
            }
            return 0;
        });

        post("/vars", (request, response) ->
        {
            try {
                Template template;
                if (request.contentType().startsWith("application/x-www-form-urlencoded")) {
                    InputStream is = request.raw().getInputStream();
                    template = Template.fromODT(is);
                    is.close();
                } else if (request.contentType().startsWith("multipart/form-data")) {
                    request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
                    template = Template.fromFormData(request.raw().getParts(), request.queryParams("format"));
                } else {
                    template = Template.fromZip(request.raw().getInputStream(), request.queryParams("format"));
                }

                response.type(JSON_CONTENT_TYPE);
                OutputStream os = response.raw().getOutputStream();
                Set<String> result = templateCompiler.vars(template, request.queryParams("prefix"));
                os.write(Json.toJson(result).getBytes(defaultCharset));
                os.flush();
                os.close();
            } catch (org.eclipse.jetty.io.EofException | MultipartStream.MalformedStreamException eof) {
                try {
                    response.raw().getOutputStream().close();
                } catch (Exception idc) {
                }
            } catch (CompilationException e) {
                error(422, response, e);
            } catch (BadRequestException e) {
                error(HttpURLConnection.HTTP_BAD_REQUEST, response, e);
            } catch (NotImplementedException e) {
                error(HttpURLConnection.HTTP_NOT_IMPLEMENTED, response, e);
            } catch (UnavailableException e) {
                error(HttpURLConnection.HTTP_UNAVAILABLE, response, e);
            } catch (Exception e) {
                error(HttpURLConnection.HTTP_INTERNAL_ERROR, response, e);
                e.printStackTrace();
            }
            return 0;
        });
        spark.Spark.init();
        log.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<]]][ Document Service started ][[[>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    }

    private void api(Response response) {
        try {
            response.type("text/html; charset=UTF-8");
            streamAndClose(SparkServer.class.getResourceAsStream("/api.html"), response.raw().getOutputStream());
        } catch (Exception e) {
            notFound(response);
        }
    }

    private InputStream getODTAsPDFFromResources(Config config, String name) throws Exception {
        File f = new File(config.getTmpFolder(), name);
        if (f.exists()) {
            return new FileInputStream(f);
        } else {
            streamAndClose(SparkServer.class.getResourceAsStream("/" + name), new FileOutputStream(f));
            templateFormatter.Convert(f, f, "pdf", false);
            return new FileInputStream(f);
        }
    }

    private InputStream getDirAsPDFFromResources(Config config, String name) throws Exception {
        File f = new File(config.getTmpFolder(), name);
        if (f.exists()) {
            return new FileInputStream(f);
        } else {
            File zip = Zip.resourceDir(config.getTmpFolder(), name);
            if (zip == null) {
                return null;
            }
            //compile

            Template template = Template.fromZip(new FileInputStream(zip), "pdf");
            template.setFormat("pdf");
            FileResult result = templateCompiler.compile(template, false);
            Files.move(result.target.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new FileInputStream(f);
        }
    }

    private void removeTheDiskCacheOfDocs() {
        //remove the disk cache to load it again from the jar
        new File(config.getTmpFolder(), "how.it.works.odt").delete();
        try {
            //remove the disk cache to load it again from the jar
            FileUtils.deleteDirectory(new File(config.getTmpFolder(), "example"));
        } catch (Exception e) {
            //not important
        }
    }

    private void notFound(Response response) {
        try {
            response.status(HttpURLConnection.HTTP_NOT_FOUND);
            OutputStream os = response.raw().getOutputStream();
            //close the response stream to prevent spark from fooling around with the return value
            os.flush();
            os.close();
        } catch (Exception e) {
            //not important
            System.err.println("couldn't send the not found response");
        }
    }

    private void streamAndClose(InputStream is, OutputStream os) throws Exception {
        IOUtils.copy(is, os);
        os.flush();
        os.close();
        is.close();
    }

    private void error(int status, Response response, Exception e) {
        try {
            response.status(status);
            response.type(TEXT_CONTENT_TYPE);
            OutputStream os = response.raw().getOutputStream();
            String msg = e.getMessage();
            if (msg == null) {
                msg = "null";
            }
            os.write(msg.getBytes(defaultCharset));
            os.flush();
            os.close();
        } catch (Exception ee) {
            System.err.println("couldn't send the error response");
        }
    }
}
