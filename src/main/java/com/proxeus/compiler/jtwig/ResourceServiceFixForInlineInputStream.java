package com.proxeus.compiler.jtwig;

import com.google.common.base.Optional;

import org.jtwig.resource.ResourceService;
import org.jtwig.resource.loader.ResourceLoader;
import org.jtwig.resource.loader.TypedResourceLoader;
import org.jtwig.resource.metadata.ResourceMetadata;
import org.jtwig.resource.metadata.ResourceResourceMetadata;
import org.jtwig.resource.reference.ResourceReference;
import org.jtwig.resource.reference.ResourceReferenceExtractor;
import org.jtwig.resource.resolver.RelativeResourceResolver;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ResourceServiceFixForInlineInputStream extends ResourceService {

    public ResourceServiceFixForInlineInputStream(Map<String, ResourceLoader> loaderMap, List<TypedResourceLoader> loaderList, Collection<String> absoluteResourceTypes, Collection<RelativeResourceResolver> relativeResourceResolvers, ResourceReferenceExtractor resourceReferenceExtractor) {
        super(loaderMap, loaderList, absoluteResourceTypes, relativeResourceResolvers, resourceReferenceExtractor);
    }

    public ResourceMetadata loadMetadata(ResourceReference reference) {
        if(reference.getType().equals(MyResourceReferenceInlineInputStream.INLINE_INPUT_STREAM)){
            return new ResourceResourceMetadata(new ResourceLoader() {
                public Optional<Charset> getCharset(String path) {
                    return Optional.absent();
                }
                public boolean exists(String path) {
                    return true;
                }
                public Optional<URL> toUrl(String path) {
                    return Optional.absent();
                }
                public InputStream load(String path) {
                    return ((MyResourceReferenceInlineInputStream)reference).getInputStream();
                }
            }, reference);
        }
        return super.loadMetadata(reference);
    }
}
