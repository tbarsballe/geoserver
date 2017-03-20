package org.geoserver.restng.converters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.util.Collection;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.resources.ResourceResource;
import org.geoserver.restng.catalog.Styles;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Base message converter behavior
 */
public abstract class BaseMessageConverter implements HttpMessageConverter {

    protected final Catalog catalog;

    protected final XStreamPersisterFactory xpf;

    protected  final GeoServer geoServer;

    protected final ApplicationContext applicationContext;

    public BaseMessageConverter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }
}
