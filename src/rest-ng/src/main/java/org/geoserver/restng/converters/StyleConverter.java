package org.geoserver.restng.converters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geotools.styling.Style;
import org.geotools.util.Version;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.xml.sax.EntityResolver;

/**
 * Style converters based on the old StyleFormat
 */
public class StyleConverter implements HttpMessageConverter {

    private final List<MediaType> supportedMediaTypes;

    private final Version version;

    private final StyleHandler handler;

    private final EntityResolver entityResolver;

    public StyleConverter(String mimeType, Version version, StyleHandler handler, EntityResolver entityResolver) {
        supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.valueOf(mimeType));
        this.handler = handler;
        this.version = version;
        this.entityResolver = entityResolver;
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return Style.class.equals(clazz) && supportedMediaTypes.contains(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        return Styles.style(handler.parse(inputMessage.getBody(), version, null, entityResolver));
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
    }
}
