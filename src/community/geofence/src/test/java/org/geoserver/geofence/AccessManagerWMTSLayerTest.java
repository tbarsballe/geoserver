package org.geoserver.geofence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.*;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.rest.catalog.WMTSLayerTest;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class AccessManagerWMTSLayerTest extends GeoServerSystemTestSupport {
    private static Boolean IS_GEOFENCE_AVAILABLE;

    protected GeofenceAccessManager accessManager;

    protected GeoFenceConfigurationManager configManager;

    protected RuleReaderService geofenceService;

    private static final String LAYER_NAME = "AMSR2_Snow_Water_Equivalent";

    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // Copy setup from GeoFenceBaseTest
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // get the beans we use for testing
        accessManager =
                (GeofenceAccessManager) applicationContext.getBean("geofenceRuleAccessManager");
        geofenceService =
                (RuleReaderService)
                        applicationContext.getBean(
                                applicationContext
                                        .getBeanFactory()
                                        .resolveEmbeddedValue("${ruleReaderBackend}"));
        configManager =
                (GeoFenceConfigurationManager)
                        applicationContext.getBean("geofenceConfigurationManager");

        // GeoFenceBaseTest enables the secure catalog, so we need to login first
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        Catalog catalog = getCatalog();
        // add a wmts store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMTSStoreInfo wmts = cb.buildWMTSStore("demo");
        wmts.setCapabilitiesURL(
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        catalog.add(wmts);

        // and a wmts layer as well (cannot use the builder, would turn this test into an online one
        addWmtsLayer();

        // logout before running any tests
        logout();
        IS_GEOFENCE_AVAILABLE = isGeoFenceAvailable();
    }

    protected boolean isGeoFenceAvailable() {
        try {
            geofenceService.getMatchingRules(null, null, null, null, null, null, null, null);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error connecting to GeoFence", e);
            return false;
        }
    }

    public void addWmtsLayer() throws Exception {
        String capabilities =
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";

        Catalog catalog = getCatalog();
        WMTSLayerInfo wml = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        if (wml == null) {
            wml = catalog.getFactory().createWMTSLayer();
            wml.setName(LAYER_NAME);
            wml.setNativeName("topp:" + LAYER_NAME);
            wml.setStore(catalog.getStoreByName("demo", WMTSStoreInfo.class));
            wml.setCatalog(catalog);
            wml.setNamespace(catalog.getNamespaceByPrefix("sf"));
            wml.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            wml.setNativeCRS(wgs84);
            wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

            catalog.add(wml);

            LayerInfo layer = catalog.getFactory().createLayer();
            layer.setResource(wml);
            layer.setName(LAYER_NAME);
            layer.setEnabled(true);
            catalog.add(layer);
        }

        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities),
                new MockHttpResponse(
                        WMTSLayerTest.class.getResource("nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @After
    public void removeLayer() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", LAYER_NAME));
        if (l != null) {
            catalog.remove(l);
        }
    }

    @Test
    public void testGetWmtsLayer() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }
        Catalog catalog = getCatalog();

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        WorkspaceInfo ws = catalog.getWorkspaceByName("sf");
        assertNotNull(ws);

        WMTSStoreInfo store = catalog.getStoreByName("sf", "demo", WMTSStoreInfo.class);
        assertNotNull(store);

        LayerInfo layer = catalog.getLayerByName("sf:" + LAYER_NAME);
        assertNotNull(layer);
        assertNotNull(layer.getResource());
        assertTrue(layer.getResource() instanceof WMTSLayerInfo);

        logout();
    }
}
