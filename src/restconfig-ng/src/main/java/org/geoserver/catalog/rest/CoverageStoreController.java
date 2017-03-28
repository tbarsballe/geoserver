package org.geoserver.catalog.rest;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.geoserver.catalog.*;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.rest.wrapper.RestWrapperAdapter;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Coverage store controller
 */
@RestController
@ControllerAdvice
@RequestMapping(path = "/restng/workspaces/{workspace}/coveragestores")
public class CoverageStoreController extends CatalogController {

    private static final Logger LOGGER = Logging.getLogger(CoverageStoreController.class);

    @Autowired
    public CoverageStoreController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public RestWrapper<CoverageStoreInfo> getCoverageStores(
            @PathVariable(name = "workspace") String workspaceName) {
        List<CoverageStoreInfo> coverageStores = catalog
                .getCoverageStoresByWorkspace(workspaceName);
        return wrapList(coverageStores, CoverageStoreInfo.class);
    }

    @GetMapping(path = "{store}", produces = { MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public RestWrapper<CoverageStoreInfo> getCoverageStore(
            @PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName) {
        CoverageStoreInfo coverageStore = getExistingCoverageStore(workspaceName, storeName);
        return wrapCoverageStore(coverageStore);
    }
    
    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public ResponseEntity<String> postCoverageStoreInfo(@RequestBody CoverageStoreInfo coverageStore,
            @PathVariable(name = "workspace") String workspaceName,
            UriComponentsBuilder builder) {
        catalog.validate(coverageStore, true).throwIfInvalid();
        catalog.add(coverageStore);

        String storeName = coverageStore.getName();
        LOGGER.info("POST coverage store " + storeName);
        UriComponents uriComponents = builder.path("/workspaces/{workspaceName}/coveragestores/{storeName}")
            .buildAndExpand(workspaceName, storeName);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uriComponents.toUri());
        return new ResponseEntity<String>(storeName, headers, HttpStatus.CREATED);
    }

    
    @PutMapping(value = "{store}", consumes = { MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
            MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE })
    public void putCoverageStoreInfo(@RequestBody CoverageStoreInfo info,
            @PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName) {
        CoverageStoreInfo original = getExistingCoverageStore(workspaceName, storeName);
        
        new CatalogBuilder(catalog).updateCoverageStore(original, info);
        catalog.validate(original, false).throwIfInvalid();
        catalog.save(original);
        clear(original);

        LOGGER.info("PUT coverage store " + workspaceName + "," + storeName);
    }

    private CoverageStoreInfo getExistingCoverageStore(String workspaceName, String storeName) {
        CoverageStoreInfo original = catalog.getCoverageStoreByName(workspaceName, storeName);
        if(original == null) {
            throw new ResourceNotFoundException(
                    "No such coverage store: " + workspaceName + "," + storeName);
        }
        return original;
    }
    
    @DeleteMapping(value = "{store}")
    public void deleteCoverageStoreInfo(@PathVariable(name = "workspace") String workspaceName,
            @PathVariable(name = "store") String storeName,
            @RequestParam(name = "recurse", required = false, defaultValue = "false") boolean recurse,
            @RequestParam(name = "purge", required = false, defaultValue = "none") String deleteType) throws IOException {
        CoverageStoreInfo cs = getExistingCoverageStore(workspaceName, storeName);
        if (!recurse) {
            if (!catalog.getCoveragesByCoverageStore(cs).isEmpty()) {
                throw new RestException("coveragestore not empty", HttpStatus.UNAUTHORIZED);
            }
            catalog.remove(cs);
        } else {
            new CascadeDeleteVisitor(catalog).visit(cs);
        }
        delete(deleteType, cs);
        clear(cs);

        LOGGER.info("DELETE coverage store " + workspaceName + ":s" + workspaceName);
    }
    
    

    /**
     * Check the deleteType parameter in order to decide whether to delete some data too (all, or just metadata).
     * @param deleteType
     * @param cs
     * @throws IOException
     */
    private void delete(String deleteType, CoverageStoreInfo cs) throws IOException {
        if (deleteType.equalsIgnoreCase("none")) {
            return;
        } else if (deleteType.equalsIgnoreCase("all") || deleteType.equalsIgnoreCase("metadata")) {
            final boolean deleteData = deleteType.equalsIgnoreCase("all");
            GridCoverageReader reader = cs.getGridCoverageReader(null, null);
            if (reader instanceof StructuredGridCoverage2DReader) {
                ((StructuredGridCoverage2DReader) reader).delete(deleteData);
            }
        }
    }

    void clear(CoverageStoreInfo info) {
        catalog.getResourcePool().clear(info);
    }

    // @Override
    // protected void handleObjectPut(Object object) throws Exception {
    // String workspace = getAttribute("workspace");
    // String coveragestore = getAttribute("coveragestore");
    //
    // CoverageStoreInfo cs = (CoverageStoreInfo) object;
    // CoverageStoreInfo original = catalog.getCoverageStoreByName(workspace, coveragestore);
    // new CatalogBuilder( catalog ).updateCoverageStore( original, cs );
    //
    // catalog.validate(original, false).throwIfInvalid();
    // catalog.save( original );
    // clear(original);
    //
    // LOGGER.info( "PUT coverage store " + workspace + "," + coveragestore );
    // }

    RestWrapper<CoverageStoreInfo> wrapCoverageStore(CoverageStoreInfo store) {
        return new RestWrapperAdapter<CoverageStoreInfo>(store, CoverageStoreInfo.class,
                getTemplate(store, CoverageStoreInfo.class)) {
            @Override
            public void configurePersister(XStreamPersister persister,
                    XStreamMessageConverter converter) {
                persister.setCallback(new XStreamPersister.Callback() {
                    @Override
                    protected Class<CoverageStoreInfo> getObjectClass() {
                        return CoverageStoreInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        WorkspaceInfo ws = store.getWorkspace();
                        String name = store.getName();
                        
                        if(ws == null || name == null) {
                            return null;
                        }
                        
                        return catalog.getCoverageStoreByName(ws, name);
                    }

                    @Override
                    protected void postEncodeCoverageStore(CoverageStoreInfo cs,
                            HierarchicalStreamWriter writer, MarshallingContext context) {
                        // add a link to the coverages
                        writer.startNode("coverages");
                        converter.encodeCollectionLink("coverages", writer);
                        writer.endNode();
                    }

                    @Override
                    protected void postEncodeReference(Object obj, String ref, String prefix,
                            HierarchicalStreamWriter writer, MarshallingContext context) {
                        if (obj instanceof WorkspaceInfo) {
                            converter.encodeLink("/workspaces/" + converter.encode(ref), writer);
                        }
                    }
                });
            }
        };

    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return CoverageStoreInfo.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return new RestHttpInputWrapper(inputMessage) {
            @Override
            public void configurePersister(XStreamPersister persister, XStreamMessageConverter xStreamMessageConverter) {
                persister.setCallback(new XStreamPersister.Callback() {
                    @Override
                    protected Class<CoverageStoreInfo> getObjectClass() {
                        return CoverageStoreInfo.class;
                    }

                    @Override
                    protected CatalogInfo getCatalogObject() {
                        Map<String, String> uriTemplateVars = (Map<String, String>) RequestContextHolder.getRequestAttributes().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                        String workspace = uriTemplateVars.get("workspace");
                        String coveragestore = uriTemplateVars.get("store");

                        if (workspace == null || coveragestore == null) {
                            return null;
                        }
                        return catalog.getCoverageStoreByName(workspace, coveragestore);
                    }
                });
            }
        };
    }
}