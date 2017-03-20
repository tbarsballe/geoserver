package org.geoserver.restng.catalog;

import org.geoserver.catalog.*;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestletException;
import org.geoserver.restng.ForbiddenException;
import org.geoserver.restng.ResourceNotFoundException;
import org.geoserver.restng.catalog.wrapper.CatalogFreemarkerContextWrapper;
import org.geoserver.restng.catalog.wrapper.Styles;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.restlet.data.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Example style resource controller
 */
@RestController @RequestMapping(path = "/restng", produces = {
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE,
    MediaType.TEXT_HTML_VALUE})
public class StyleController {

    private final Catalog catalog;

    private static final Logger LOGGER = Logging.getLogger(StyleController.class);

    @Autowired
    public StyleController(Catalog catalog) {
        this.catalog = catalog;
    }

    @RequestMapping(value = "/styles", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Styles test(@RequestParam(required = false) String workspace,
                       @RequestParam(required = false) String layer) {

        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return new Styles(styles);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE})
    public CatalogFreemarkerContextWrapper testFreemarker(@RequestParam(required = false) String workspace,
                                         @RequestParam(required = false) String layer) {

        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        return new CatalogFreemarkerContextWrapper(styles, StyleInfo.class);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST, consumes = { "text/xml", "application/xml" })
    @ResponseStatus(HttpStatus.CREATED)
    public String postStyle(@RequestBody StyleInfo style) {
        return postStyleInfoInternal(style, null, null);
    }

    @RequestMapping(value = "/styles", method = RequestMethod.POST,
        consumes = {SLDHandler.MIMETYPE_11, SLDHandler.MIMETYPE_10})
    public ResponseEntity<String> postStyle(@RequestBody Style style,
                                            @RequestHeader("Content-Type") String contentType, UriComponentsBuilder builder)
    {
        StyleHandler handler = org.geoserver.catalog.Styles.handler(contentType);
        return postStyleInternal(style, null, null, handler, contentType, builder);
    }

    public ResponseEntity<String> postStyleInternal(Object object, String name, String workspace,
        StyleHandler styleFormat, String mediaType, UriComponentsBuilder builder)
    {

        if (name == null) {
            name = findNameFromObject(object);
        }

        //ensure that the style does not already exist
        if (catalog.getStyleByName(workspace, name) != null) {
            throw new RestletException("Style " + name + " already exists.",
                Status.CLIENT_ERROR_FORBIDDEN);
        }

        StyleInfo sinfo = catalog.getFactory().createStyle();
        sinfo.setName(name);
        sinfo.setFilename(name + "." + styleFormat.getFileExtension());
        sinfo.setFormat(styleFormat.getFormat());
        sinfo.setFormatVersion(styleFormat.versionForMimeType(mediaType));

        if (workspace != null) {
            sinfo.setWorkspace(catalog.getWorkspaceByName(workspace));
        }

        // ensure that a existing resource does not already exist, because we may not want to overwrite it
        GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        if (dataDir.style(sinfo).getType() != Resource.Type.UNDEFINED) {
            String msg = "Style resource " + sinfo.getFilename() + " already exists.";
            throw new RestletException(msg, Status.CLIENT_ERROR_FORBIDDEN);
        }


        ResourcePool resourcePool = catalog.getResourcePool();
        try {
            if (object instanceof Style) {
                resourcePool.writeStyle(sinfo, (Style) object);
            } else {
                resourcePool.writeStyle(sinfo, (InputStream) object);
            }
        } catch (IOException e) {
            throw new RestletException("Error writing style", Status.SERVER_ERROR_INTERNAL, e);
        }

        catalog.add(sinfo);
        LOGGER.info("POST Style " + name);

        UriComponents uriComponents = builder.path("/styles/{id}").buildAndExpand(name);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<String>(name, headers, HttpStatus.CREATED);
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

    @RequestMapping(path = "/workspaces/{workspaceName}/styles/{styleName}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    protected StyleInfo getStyleFromWorkspace(
        @PathVariable String styleName,
        @PathVariable String workspaceName) {
        return getStyleInternal(styleName, workspaceName);
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    protected StyleInfo getStyle(
        @PathVariable String styleName) {
        return getStyleInternal(styleName, null);
    }

    @RequestMapping(path = "/workspaces/{workspaceName}/styles/{styleName}", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE})
    protected CatalogFreemarkerContextWrapper getStyleFromWorkspaceFreemarker(
            @PathVariable String styleName,
            @PathVariable String workspaceName) {
        return new CatalogFreemarkerContextWrapper(getStyleInternal(styleName, workspaceName));
    }

    @RequestMapping(path = "/styles/{styleName}", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE})
    protected CatalogFreemarkerContextWrapper getStyleFreemarker(
            @PathVariable String styleName) {
        return new CatalogFreemarkerContextWrapper(getStyleInternal(styleName, null));
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

    String findNameFromObject(Object object) {
        String name = null;
        if (object instanceof Style) {
            name = ((Style)object).getName();
        }

        if (name == null) {
            // generate a random one
            for (int i = 0; name == null && i < 100; i++) {
                String candidate = "style-"+ UUID.randomUUID().toString().substring(0, 7);
                if (catalog.getStyleByName(candidate) == null) {
                    name = candidate;
                }
            }
        }

        if (name == null) {
            throw new RestletException("Unable to generate style name, specify one with 'name' parameter",
                Status.SERVER_ERROR_INTERNAL);
        }

        return name;
    }

}
