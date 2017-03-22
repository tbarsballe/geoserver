package org.geoserver.restng.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.restng.RestController;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Base controller for catalog info requests
 */
public class CatalogController extends RestController {

    protected final Catalog catalog;

    @Autowired
    public CatalogController(Catalog catalog) {
        super();
        this.catalog = catalog;
        this.pathPrefix = "templates";
    }
}
