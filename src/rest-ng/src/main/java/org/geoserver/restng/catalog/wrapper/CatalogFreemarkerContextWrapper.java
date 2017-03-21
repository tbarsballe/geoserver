package org.geoserver.restng.catalog.wrapper;

import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;

/**
 * Created by tbarsballe on 2017-03-21.
 */
public class CatalogFreemarkerContextWrapper extends DefaultFreemarkerContextWrapper {

    public CatalogFreemarkerContextWrapper(Object object) {
        super(object, object.getClass(), CatalogFreemarkerHTMLFormat.class, "templates");
    }

    public CatalogFreemarkerContextWrapper(Object object, Class clazz) {
        super(object, clazz, CatalogFreemarkerHTMLFormat.class, "templates");
    }
}
