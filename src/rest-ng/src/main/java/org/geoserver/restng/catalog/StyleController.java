package org.geoserver.restng.catalog;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestletException;
import org.geoserver.restng.ForbiddenException;
import org.geoserver.restng.ResourceNotFoundException;
import org.geotools.util.logging.Logging;
import org.restlet.data.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example style resource controller
 */
@RestController @RequestMapping(path = "/restng", produces = {
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE })
public class StyleController {

    private final Catalog catalog;

    private static final Logger LOGGER = Logging.getLogger(StyleController.class);

    @Autowired
    public StyleController(Catalog catalog) {
        this.catalog = catalog;
    }

    @RequestMapping(value = "/styles", method = RequestMethod.GET)
    public Styles test(@RequestParam(required = false) String workspace,
        @RequestParam(required = false) String layer) {

        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return new Styles(styles);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST, consumes = { "text/xml", "application/xml" })
    @ResponseStatus(HttpStatus.CREATED)
    public String postStyle(@RequestBody StyleInfo style) {
        return postStyleInfoInternal(style, null, null);
    }

    @RequestMapping(value = "/workspaces/{workspaceName}/styles",
        method = RequestMethod.POST, consumes = { "text/xml", "application/xml" })
    @ResponseStatus(HttpStatus.CREATED)
    public String postStyleInfoToWorkspace(@RequestBody StyleInfo styleInfo,
        @PathVariable String workspaceName) {
        return postStyleInfoInternal(styleInfo, workspaceName, null);
    }

    public String postStyleInfoInternal(StyleInfo style, String workspace, String layer)
    {
        if (layer != null) {
            StyleInfo existing = catalog.getStyleByName(style.getName());
            if (existing == null) {
                throw new ResourceNotFoundException();
            }

            LayerInfo l = catalog.getLayerByName(layer);
            l.getStyles().add(existing);

            //todo wire this up
            //            //check for default
            //            String def = getRequest().getResourceRef().getQueryAsForm().getFirstValue("default");
            //            if ( "true".equals( def ) ) {
            //                l.setDefaultStyle( existing );
            //            }
            catalog.save(l);
            LOGGER.info("POST style " + style.getName() + " to layer " + layer);
        } else {

            if (workspace != null) {
                style.setWorkspace(catalog.getWorkspaceByName(workspace));
            }

            catalog.add(style);
            LOGGER.info("POST style " + style.getName());
        }

        return style.getName();
    }

    @RequestMapping(path = "/workspaces/{workspaceName}/styles/{styleName}", method = RequestMethod.GET)
    protected StyleInfo getStyleFromWorkspace(
        @PathVariable String styleName,
        @PathVariable String workspaceName) {
        return getStyleInternal(styleName, workspaceName);
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.GET)
    protected StyleInfo getStyle(
        @PathVariable String styleName) {
        return getStyleInternal(styleName, null);
    }

    protected StyleInfo getStyleInternal(String styleName, String workspace) {
        LOGGER.fine("GET style " + styleName);
        StyleInfo sinfo = workspace == null ?
            catalog.getStyleByName(styleName) :
            catalog.getStyleByName(workspace, styleName);

        if (sinfo == null) {
            throw new ResourceNotFoundException();
        } else {
            return sinfo;
        }
    }

    @RequestMapping(
        path = "/workspaces/{workspaceName}/styles/{styleName}",
        method = RequestMethod.DELETE)
    protected void deleteStyleWithWorkspace(
        @PathVariable String styleName,
        @PathVariable String workspaceName,
        @RequestParam(required = false, defaultValue = "false") boolean recurse,
        @RequestParam(required = false, defaultValue = "false") boolean purge)
        throws IOException {
        deleteStyleInternal(styleName, recurse, purge, workspaceName);
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.DELETE)
    protected void deleteStyle(
        @PathVariable String styleName,
        @RequestParam(required = false, defaultValue = "false") boolean recurse,
        @RequestParam(required = false, defaultValue = "false") boolean purge)
        throws IOException {
        deleteStyleInternal(styleName, recurse, purge, null);
    }

    protected void deleteStyleInternal(String styleName, boolean recurse, boolean purge,
        String workspace)
        throws IOException {

        StyleInfo style = workspace != null ? catalog.getStyleByName(workspace, styleName) :
            catalog.getStyleByName(styleName);

        if (recurse) {
            new CascadeDeleteVisitor(catalog).visit(style);
        } else {
            // ensure that no layers reference the style
            List<LayerInfo> layers = catalog.getLayers(style);
            if (!layers.isEmpty()) {
                //todo, a real exception
                throw new ForbiddenException("Can't delete style referenced by existing layers.");
            }
            catalog.remove(style);
        }

        catalog.getResourcePool().deleteStyle(style, purge);

        LOGGER.info("DELETE style " + styleName);
    }

}
