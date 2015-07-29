package org.geoserver.wps.executor;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.util.NoExternalEntityResolver;
import org.geoserver.wps.MonkeyProcess;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.executor.DefaultProcessManager;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class ThreadPoolTest extends WPSTestSupport {
    private static final Logger LOGGER = Logging.getLogger(ThreadPoolTest.class);
    
    @Before
    public void setUpInternal() throws Exception {
        // make extra sure we don't have anything else going
        MonkeyProcess.clearCommands();
        
        //Force small thread pool
        DefaultProcessManager processManager = applicationContext.getBean(DefaultProcessManager.class);
        processManager.asynchService = new ThreadPoolExecutor(1, 5, 10L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        processManager.synchService = new ThreadPoolExecutor(1, 5, 10L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        WPSExecutionManager  executionManager = applicationContext.getBean(WPSExecutionManager.class);
        executionManager.executors = new ThreadPoolExecutor(1, 5, 10L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        //Field field = WPSExecutionManager.class.getDeclaredField("executors");
        //field.setAccessible(true);
        //field.set(executionManager, Executors.newFixedThreadPool(5));
    }
    
    
    @Test
    public void testManyRequests() throws Exception {
    
        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                + "<ows:Identifier>geo:buffer</ows:Identifier>"
                + "<wps:DataInputs>"
                + "<wps:Input>"
                + "<ows:Identifier>distance</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:LiteralData>1</wps:LiteralData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "<wps:Input>"
                + "<ows:Identifier>geom</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:ComplexData mimeType=\"application/wkt\">"
                + "<![CDATA[POINT(1 2)]]>"
                + "</wps:ComplexData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "</wps:DataInputs>"
                + "<wps:ResponseForm>"
                + "<wps:ResponseDocument storeExecuteResponse='false'>"
                + "<wps:Output>"
                + "<ows:Identifier>result</ows:Identifier>"
                + "</wps:Output>"
                + "</wps:ResponseDocument>" + "</wps:ResponseForm>" + "</wps:Execute>";
    
        WPSResourceManager  resourceManager = applicationContext.getBean(WPSResourceManager.class);
        Field field = WPSResourceManager.class.getDeclaredField("executionId");
        field.setAccessible(true);
        ThreadLocal<String> executionId = (ThreadLocal<String>) field.get(resourceManager);
        Document dom;
        
        
        for (int i = 0; i < 1000; i++) {
            
            dom = postAsDOM("wps", xml);
            //print(dom);
            assertXpathExists("//gml:Polygon", dom);
            LOGGER.log(Level.INFO, "ExecutionId: "+executionId.get());
        }
    }
}
