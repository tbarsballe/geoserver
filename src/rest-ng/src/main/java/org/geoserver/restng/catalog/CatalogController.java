package org.geoserver.restng.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.restng.RestController;

/**
 * Base controller for catalog info requests
 */
public class CatalogController extends RestController {

    protected final Catalog catalog;

    public CatalogController(Catalog catalog) {
        super();
        this.catalog = catalog;
        this.pathPrefix = "templates";
    }
}
