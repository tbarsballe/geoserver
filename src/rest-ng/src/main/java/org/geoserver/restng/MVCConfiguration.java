package org.geoserver.restng;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.rest.StyleFormat;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.restng.converters.JSONMessageConverter;
import org.geoserver.restng.converters.StyleConverter;
import org.geoserver.restng.converters.XMLMessageConverter;
import org.geotools.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.xml.sax.EntityResolver;

/**
 * Configure various aspects of Spring MVC, in particular message converters
 */
@Configuration
public class MVCConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Catalog catalog = (Catalog) applicationContext.getBean("catalog");

        converters.add(new JSONMessageConverter(applicationContext));
        converters.add(new XMLMessageConverter(applicationContext));

        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                converters.add(new StyleConverter(sh.mimeType(ver), ver, sh, entityResolver));
            }
        }
    }
}
