package org.geoserver.restng.catalog;

import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example style resource controller
 */
@RestController
@RequestMapping(path = "/restng/styles", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class StyleController {

    private final Catalog catalog;
    private static final Logger LOGGER = Logging.getLogger(StyleController.class);

    @Autowired
    public StyleController(Catalog catalog) {
        this.catalog = catalog;
    }

    @RequestMapping(
        method = RequestMethod.GET)
    public @ResponseBody org.geoserver.restng.catalog.Styles test(
        @RequestParam(required = false) String workspace,
        @RequestParam(required = false) String layer)
    {

        List<StyleInfo> styles = catalog
            .getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return new Styles(styles);
    }


    @RequestMapping(path = "/{styleName}", method = RequestMethod.GET)
    protected Object getStyle(@RequestParam(required = false) String workspace,
        @PathVariable String styleName) {

        LOGGER.fine("GET style " + styleName);
        StyleInfo sinfo = workspace == null ? catalog.getStyleByName(styleName) :
            catalog.getStyleByName(workspace, styleName);

        //unwrapping could be done in a converter for sure.
        return sinfo;
    }
}
