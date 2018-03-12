/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Default GeoServerLoader which loads and persists configuration from the classic GeoServer data
 * directory structure.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DefaultGeoServerLoader extends GeoServerLoader {

    ConfigurationListener listener;
    GeoServerPersister persister;

    public DefaultGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        catalog.setResourceLoader(resourceLoader);

        readCatalog(catalog, xp);

        if (!legacy) {
            //add the listener which will persist changes
            catalog.addListener(new GeoServerPersister(resourceLoader, xp));
        }
    }

    protected void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception {
        if (listener == null) {
            // add event listener which persists changes
            final List<XStreamServiceLoader> loaders =
                    GeoServerExtensions.extensions(XStreamServiceLoader.class);
            listener = new ServicePersister(loaders, geoServer);
        } else {
            // avoid re-dumping all service config files during load,
            // we'll attach it back once done
            geoserver.removeListener(listener);
        }

        try {
            if (this.persister != null) {
                // avoid having the persister write down new config files while we read the config,
                // otherwise it'll dump it back in xml files
                geoserver.removeListener(persister);
            } else {
                // lazy creation of the persister at the first need
                this.persister = new GeoServerPersister(resourceLoader, xp);
            }
            readConfiguration(geoServer, xp);
        } finally {
            // attach back the catalog persister and the service one
            geoserver.addListener(persister);
            geoserver.addListener(listener);
        }
    }

    @Override
    protected void initializeStyles(Catalog catalog, XStreamPersister xp) throws IOException {
        //add a persister temporarily in case the styles don't exist on disk
        GeoServerPersister p = new GeoServerPersister(resourceLoader, xp);
        catalog.addListener(p);

        super.initializeStyles(catalog, xp);

        catalog.removeListener(p);
    }

}
