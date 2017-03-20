package org.geoserver.restng;

import java.util.List;

import org.geoserver.restng.converters.JSONMessageConverter;
import org.geoserver.restng.converters.XMLMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Configure various aspects of Spring MVC, in particular message converters
 */
@Configuration
public class MVCConfiguration extends WebMvcConfigurationSupport {

    @Autowired ApplicationContext applicationContext;

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new JSONMessageConverter(applicationContext));
        converters.add(new XMLMessageConverter(applicationContext));
    }
}
