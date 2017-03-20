package org.geoserver.restng.catalog.wrapper;

import org.geoserver.restng.converters.FreemarkerHTMLMessageConverter;

/**
 * A wrapper for all responses that get processed by the {@link FreemarkerHTMLMessageConverter}
 */
public interface FreemarkerContextWrapper {

    /**
     * Get the wrapped object
     *
     * @return the wrapped object
     */
    Object getObject();

    /**
     * Get the class of the wrapped object (or class of the collection contents)
     *
     * @return
     */
    Class getObjectClass();

    /**
     * Get the class used for template loading (templates will be loaded relative to this class)
     *
     * @return the class for template loading
     */
    Class getClassForTemplateLoading();

    /**
     * Prefix appended to all templates during search.
     * A folder can be returned to further narrow the path to load the templates.
     *
     * @return template prefix
     */
    String getTemplatePrefix();
}
