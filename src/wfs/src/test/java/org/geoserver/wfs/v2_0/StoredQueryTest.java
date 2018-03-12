/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.StoredQueryProvider;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.xml.Parser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class StoredQueryTest extends WFS20TestSupport {

    @Before
    public void clearQueries() {
        new StoredQueryProvider(getCatalog()).removeAll();
    }

    @Test
    public void testListStoredQueries() throws Exception {
        MockHttpServletResponse response = getAsServletResponse
                ("wfs?request=ListStoredQueries&service=wfs&version=2.0.0");
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        XMLAssert.assertXpathExists("//wfs:StoredQuery[@id = '" + StoredQuery.DEFAULT.getName() + "']", dom);

        // schema validate the response
        FeatureTypeSchemaBuilder sb =
                new FeatureTypeSchemaBuilder.GML3(getGeoServer());
        WFSConfiguration configuration = new WFSConfiguration(getGeoServer(), sb, new org.geoserver.wfs.xml
                .v1_1_0.WFS(sb));
        Parser parser = new Parser(configuration);
        parser.parse(new ByteArrayInputStream(response.getContentAsByteArray()));

        assertEquals(0, parser.getValidationErrors().size());
    }

    @Test
    public void testListStoredQueries2() throws Exception {
        testCreateStoredQuery();

        Document dom = getAsDOM("wfs?request=ListStoredQueries&service=wfs&version=2.0.0");
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:StoredQuery)", dom);
        XMLAssert.assertXpathExists("//wfs:StoredQuery[@id = '" + StoredQuery.DEFAULT.getName() + "']", dom);
        XMLAssert.assertXpathExists("//wfs:StoredQuery[@id = 'myStoredQuery']", dom);
    }

    @Test
    public void testCreateUnknownLanguage() throws Exception {
        String xml = "<CreateStoredQuery xmlns=\"http://www.opengis.net/wfs/2.0\" service=\"WFS\" " +
                "version=\"2.0.0\">\n" +
                "  <StoredQueryDefinition xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
                "                          id=\"urn:example:wfs2-query:InvalidLang\">\n" +
                "      <Title>GetFeatureByTypeName</Title>\n" +
                "      <Abstract>Returns feature representations by type name.</Abstract>\n" +
                "      <Parameter name=\"typeName\" type=\"xsd:QName\">\n" +
                "         <Abstract>Qualified name of feature type (required).</Abstract>\n" +
                "      </Parameter>\n" +
                "      <QueryExpressionText isPrivate=\"false\" language=\"http://qry.example.org\" " +
                "returnFeatureTypes=\"\">\n" +
                "         <Query typeNames=\"${typeName}\"/>\n" +
                "      </QueryExpressionText>\n" +
                "  </StoredQueryDefinition>\n" +
                "</CreateStoredQuery>";
        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals(400, response.getStatus());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        checkOws11Exception(dom, "2.0.0", ServiceException.INVALID_PARAMETER_VALUE, "language");
    }

    @Test
    public void testCreateStoredQuery() throws Exception {
        String xml =
                "<wfs:ListStoredQueries service='WFS' version='2.0.0' " +
                        " xmlns:wfs='" + WFS.NAMESPACE + "'/>";
        Document dom = postAsDOM("wfs", xml);
        // print(dom);
        assertEquals("wfs:ListStoredQueriesResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:StoredQuery)", dom);

        xml = getCreatePrimitiveWithinQuery();

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:CreateStoredQueryResponse", dom.getDocumentElement().getNodeName());
        assertEquals("OK", dom.getDocumentElement().getAttribute("status"));

        dom = getAsDOM("wfs?request=ListStoredQueries");
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:StoredQuery)", dom);
        XMLAssert.assertXpathExists("//wfs:StoredQuery[@id = 'myStoredQuery']", dom);
        XMLAssert.assertXpathExists("//wfs:ReturnFeatureType[text() = 'sf:PrimitiveGeoFeature']", dom);
    }

    @Test
    public void testDuplicateStoredQuery() throws Exception {
        String xml = getCreatePrimitiveWithinQuery();
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:CreateStoredQueryResponse", dom.getDocumentElement().getNodeName());
        assertEquals("OK", dom.getDocumentElement().getAttribute("status"));

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals(400, response.getStatus());
        dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));

        checkOws11Exception(dom, "2.0.0", WFSException.DUPLICATE_STORED_QUERY_ID_VALUE, "myStoredQuery");
    }

    public String getCreatePrimitiveWithinQuery() {
        return "<wfs:CreateStoredQuery service='WFS' version='2.0.0' " +
                "   xmlns:wfs='http://www.opengis.net/wfs/2.0' " +
                "   xmlns:fes='http://www.opengis.org/fes/2.0' " +
                "   xmlns:gml='http://www.opengis.net/gml/3.2' " +
                "   xmlns:myns='http://www.someserver.com/myns' " +
                "   xmlns:sf='" + MockData.SF_URI + "'>" +
                "   <wfs:StoredQueryDefinition id='myStoredQuery'> " +
                "      <wfs:Parameter name='AreaOfInterest' type='gml:Polygon'/> " +
                "      <wfs:QueryExpressionText " +
                "           returnFeatureTypes='sf:PrimitiveGeoFeature' " +
                "           language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression' " +
                "           isPrivate='false'> " +
                "         <wfs:Query typeNames='sf:PrimitiveGeoFeature'> " +
                "            <fes:Filter> " +
                "               <fes:Within> " +
                "                  <fes:ValueReference>pointProperty</fes:ValueReference> " +
                "                  ${AreaOfInterest} " +
                "               </fes:Within> " +
                "            </fes:Filter> " +
                "         </wfs:Query> " +
                "      </wfs:QueryExpressionText> " +
                "   </wfs:StoredQueryDefinition> " +
                "</wfs:CreateStoredQuery>";
    }

    @Test
    public void testCreateStoredQueryMismatchingTypes() throws Exception {
        String xml =
                "<wfs:ListStoredQueries service='WFS' version='2.0.0' " +
                        " xmlns:wfs='" + WFS.NAMESPACE + "'/>";
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:ListStoredQueriesResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:StoredQuery)", dom);

        xml =
                "<wfs:CreateStoredQuery service='WFS' version='2.0.0' " +
                        "   xmlns:wfs='http://www.opengis.net/wfs/2.0' " +
                        "   xmlns:fes='http://www.opengis.org/fes/2.0' " +
                        "   xmlns:gml='http://www.opengis.net/gml/3.2' " +
                        "   xmlns:sf='" + MockData.SF_URI + "'>" +
                        "   <wfs:StoredQueryDefinition id='myStoredQuery'> " +
                        "      <wfs:Parameter name='AreaOfInterest' type='gml:Polygon'/> " +
                        "      <wfs:QueryExpressionText " +
                        "           returnFeatureTypes='sf:PrimitiveGeoFeature' " +
                        "           language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression' " +
                        "           isPrivate='false'> " +
                        "         <wfs:Query typeNames='sf:AggregateGeoFeature'> " +
                        "            <fes:Filter> " +
                        "               <fes:Within> " +
                        "                  <fes:ValueReference>pointProperty</fes:ValueReference> " +
                        "                  ${AreaOfInterest} " +
                        "               </fes:Within> " +
                        "            </fes:Filter> " +
                        "         </wfs:Query> " +
                        "      </wfs:QueryExpressionText> " +
                        "   </wfs:StoredQueryDefinition> " +
                        "</wfs:CreateStoredQuery>";

        dom = postAsDOM("wfs", xml);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        dom = getAsDOM("wfs?request=ListStoredQueries");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:StoredQuery)", dom);
    }

    @Test
    public void testDescribeStoredQueries() throws Exception {
        Document dom = getAsDOM("wfs?request=DescribeStoredQueries&storedQueryId=myStoredQuery", 400);
        checkOws11Exception(dom, "2.0.0", ServiceException.INVALID_PARAMETER_VALUE, "STOREDQUERY_ID");

        testCreateStoredQuery();

        String xml =
                "<wfs:DescribeStoredQueries xmlns:wfs='" + WFS.NAMESPACE + "' service='WFS'>" +
                        "<wfs:StoredQueryId>myStoredQuery</wfs:StoredQueryId>" +
                        "</wfs:DescribeStoredQueries>";

        dom = postAsDOM("wfs", xml);
        assertEquals("wfs:DescribeStoredQueriesResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//wfs:StoredQueryDescription[@id='myStoredQuery']", dom);
    }

    @Test
    public void testDescribeStoredQueries2() throws Exception {
        Document dom = getAsDOM("wfs?request=DescribeStoredQueries&storedQuery_Id=myStoredQuery");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        testCreateStoredQuery();

        dom = getAsDOM("wfs?request=DescribeStoredQueries&storedQuery_Id=myStoredQuery");
        assertEquals("wfs:DescribeStoredQueriesResponse", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//wfs:StoredQueryDescription[@id='myStoredQuery']", dom);
    }

    @Test
    public void testDescribeDefaultStoredQuery() throws Exception {
        Document dom = getAsDOM("wfs?request=DescribeStoredQueries&storedQueryId=" + StoredQuery.DEFAULT.getName());
        assertEquals("wfs:DescribeStoredQueriesResponse", dom.getDocumentElement().getNodeName());

        XMLAssert.assertXpathExists("//wfs:StoredQueryDescription[@id = '"
                + StoredQuery.DEFAULT.getName() + "']", dom);
        XMLAssert.assertXpathExists("//wfs:Parameter[@name = 'ID']", dom);
        XMLAssert.assertXpathExists("//wfs:QueryExpressionText[@isPrivate = 'true']", dom);
        XMLAssert.assertXpathNotExists("//wfs:QueryExpressionText/*", dom);
    }

    @Test
    public void testDropStoredQuery() throws Exception {
        Document dom = getAsDOM("wfs?request=DropStoredQuery&id=myStoredQuery");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        testCreateStoredQuery();

        String xml =
                "<wfs:DropStoredQuery xmlns:wfs='" + WFS.NAMESPACE + "' service='WFS' id='myStoredQuery'/>";
        dom = postAsDOM("wfs", xml);

        assertEquals("wfs:DropStoredQueryResponse", dom.getDocumentElement().getNodeName());
        assertEquals("OK", dom.getDocumentElement().getAttribute("status"));

        dom = getAsDOM("wfs?request=DropStoredQuery&id=myStoredQuery");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testDropStoredQuery2() throws Exception {
        Document dom = getAsDOM("wfs?request=DropStoredQuery&storedQuery_id=myStoredQuery");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        testCreateStoredQuery();
        dom = getAsDOM("wfs?request=DropStoredQuery&storedQuery_id=myStoredQuery");
        assertEquals("wfs:DropStoredQueryResponse", dom.getDocumentElement().getNodeName());
        assertEquals("OK", dom.getDocumentElement().getAttribute("status"));

        dom = getAsDOM("wfs?request=DropStoredQuery&storedQuery_id=myStoredQuery");
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testCreateStoredQuerySOAP() throws Exception {
        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " +
                        " <soap:Header/> " +
                        " <soap:Body>" +
                        "<wfs:CreateStoredQuery service='WFS' version='2.0.0' " +
                        "   xmlns:wfs='http://www.opengis.net/wfs/2.0' " +
                        "   xmlns:fes='http://www.opengis.org/fes/2.0' " +
                        "   xmlns:gml='http://www.opengis.net/gml/3.2' " +
                        "   xmlns:myns='http://www.someserver.com/myns' " +
                        "   xmlns:sf='" + MockData.SF_URI + "'>" +
                        "   <wfs:StoredQueryDefinition id='myStoredQuery'> " +
                        "      <wfs:Parameter name='AreaOfInterest' type='gml:Polygon'/> " +
                        "      <wfs:QueryExpressionText " +
                        "           returnFeatureTypes='sf:PrimitiveGeoFeature' " +
                        "           language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression' " +
                        "           isPrivate='false'> " +
                        "         <wfs:Query typeNames='sf:PrimitiveGeoFeature'> " +
                        "            <fes:Filter> " +
                        "               <fes:Within> " +
                        "                  <fes:ValueReference>pointProperty</fes:ValueReference> " +
                        "                  ${AreaOfInterest} " +
                        "               </fes:Within> " +
                        "            </fes:Filter> " +
                        "         </wfs:Query> " +
                        "      </wfs:QueryExpressionText> " +
                        "   </wfs:StoredQueryDefinition> " +
                        "</wfs:CreateStoredQuery>" +
                        " </soap:Body> " +
                        "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:CreateStoredQueryResponse").getLength());
    }

    @Test
    public void testDescribeStoredQueriesSOAP() throws Exception {
        testCreateStoredQuery();

        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " +
                        " <soap:Header/> " +
                        " <soap:Body>" +
                        "<wfs:DescribeStoredQueries xmlns:wfs='" + WFS.NAMESPACE + "' service='WFS'>" +
                        "<wfs:StoredQueryId>myStoredQuery</wfs:StoredQueryId>" +
                        "</wfs:DescribeStoredQueries>" +
                        " </soap:Body> " +
                        "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:DescribeStoredQueriesResponse").getLength());
    }

    @Test
    public void testListStoredQueriesSOAP() throws Exception {
        testCreateStoredQuery();

        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " +
                        " <soap:Header/> " +
                        " <soap:Body>" +
                        "<wfs:ListStoredQueries service='WFS' version='2.0.0' " +
                        " xmlns:wfs='" + WFS.NAMESPACE + "'/>" +
                        " </soap:Body> " +
                        "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:ListStoredQueriesResponse").getLength());
    }

    @Test
    public void testDropStoredQuerySOAP() throws Exception {
        testCreateStoredQuery();

        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " +
                        " <soap:Header/> " +
                        " <soap:Body>" +
                        "<wfs:DropStoredQuery service='WFS' version='2.0.0' " +
                        " xmlns:wfs='" + WFS.NAMESPACE + "' id='myStoredQuery'/>" +
                        " </soap:Body> " +
                        "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:DropStoredQueryResponse").getLength());
    }

    @Test
    public void testDropUnknownStoredQuery() throws Exception {
        Document dom = getAsDOM("wfs?request=DropStoredQuery&storedQuery_Id=myStoredQuery", 400);
        checkOws11Exception(dom, "2.0.0", ServiceException.INVALID_PARAMETER_VALUE, "id");
    }

    @Test
    public void testCreateParametrizedOnTypename() throws Exception {
        // this evil thing comes from the CITE tests
        String xml =
                "<CreateStoredQuery xmlns=\"http://www.opengis.net/wfs/2.0\" service=\"WFS\" version=\"2.0.0\">\n" +
                        "  <StoredQueryDefinition xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
                        "                          id=\"urn:example:wfs2-query:GetFeatureByTypeName\">\n" +
                        "      <Title>GetFeatureByTypeName</Title>\n" +
                        "      <Abstract>Returns feature representations by type name.</Abstract>\n" +
                        "      <Parameter name=\"typeName\" type=\"xsd:QName\">\n" +
                        "         <Abstract>Qualified name of feature type (required).</Abstract>\n" +
                        "      </Parameter>\n" +
                        "      <QueryExpressionText isPrivate=\"false\"\n" +
                        "                           " +
                        "language=\"urn:ogc:def:queryLanguage:OGC-WFS::WFSQueryExpression\"\n" +
                        "                           returnFeatureTypes=\"\">\n" +
                        "         <Query typeNames=\"${typeName}\"/>\n" +
                        "      </QueryExpressionText>\n" +
                        "</StoredQueryDefinition>\n" +
                        "</CreateStoredQuery>";

        // create
        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:CreateStoredQueryResponse", dom.getDocumentElement().getNodeName());
        assertEquals("OK", dom.getDocumentElement().getAttribute("status"));

        // verify exists
        dom = getAsDOM("wfs?request=ListStoredQueries");
        XMLAssert.assertXpathEvaluatesTo("2", "count(//wfs:StoredQuery)", dom);
        XMLAssert.assertXpathExists("//wfs:StoredQuery[@id = 'urn:example:wfs2-query:GetFeatureByTypeName']", dom);

        // run it (the cite tests use a random prefix
        dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetFeature&storedQuery_id=urn:example:wfs2-query" +
                ":GetFeatureByTypeName&typename=tns:Fifteen", 200);
        // print(dom);
        XMLAssert.assertXpathExists("/wfs:FeatureCollection", dom);
        XMLAssert.assertXpathEvaluatesTo("15", "count(//cdf:Fifteen)", dom);
    }

}
