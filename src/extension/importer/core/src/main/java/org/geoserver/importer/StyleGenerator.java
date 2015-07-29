/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Generates pseudo random styles using a specified color ramp
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class StyleGenerator {

    static enum StyleType {
        POINT, LINE, POLYGON, RASTER, GENERIC
    };

    static final Map<StyleType, String> TEMPLATES = new HashMap<StyleType, String>();
    static {
        try {
            TEMPLATES.put(StyleType.POINT, IOUtils.toString(StyleGenerator.class
                    .getResourceAsStream("template_point.sld")));
            TEMPLATES.put(StyleType.POLYGON, IOUtils.toString(StyleGenerator.class
                    .getResourceAsStream("template_polygon.sld")));
            TEMPLATES.put(StyleType.LINE, IOUtils.toString(StyleGenerator.class
                    .getResourceAsStream("template_line.sld")));
            TEMPLATES.put(StyleType.RASTER, IOUtils.toString(StyleGenerator.class
                    .getResourceAsStream("template_raster.sld")));
            TEMPLATES.put(StyleType.GENERIC, IOUtils.toString(StyleGenerator.class
                    .getResourceAsStream("template_generic.sld")));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the style templates", e);
        }
    }

    private ColorRamp ramp;

    private Catalog catalog;

    /**
     * Workspace to create styles relative to
     */
    private WorkspaceInfo workspace;

    /**
     * Builds a style generator with the default color ramp
     * @param catalog
     */
    public StyleGenerator(Catalog catalog) {
        this.catalog = catalog;
        ramp = new ColorRamp();
        ramp.add("red", Color.decode("0xFF0000"));
        ramp.add("orange", Color.decode("0xFFA500"));
        ramp.add("yellow", Color.decode("0xFFFF00"));
        ramp.add("chartreuse", Color.decode("0x7FFF00"));
        ramp.add("lime", Color.decode("0x00FF00"));
        ramp.add("springgreen", Color.decode("0x00FF7F"));
        ramp.add("cyan", Color.decode("0x00FFFF"));
        ramp.add("deepskyblue", Color.decode("0x00BFFF"));
        ramp.add("blue", Color.decode("0x0000FF"));
        ramp.add("darkviolet", Color.decode("0x9400D3"));
        ramp.add("magenta", Color.decode("0xFF00FF"));
        randomizeRamp();
    }

    protected void randomizeRamp() {
        ramp.initRandom();
    }

    public StyleGenerator(Catalog catalog, ColorRamp ramp) {
        if (ramp == null)
            throw new NullPointerException("The color ramp cannot be null");

        this.ramp = ramp;
        this.catalog = catalog;
    }

    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    public StyleInfo createStyle(FeatureTypeInfo featureType) throws IOException {
        return createStyle(featureType, featureType.getFeatureType());
    }
    
    public StyleInfo createStyle(FeatureTypeInfo featureType, FeatureType nativeFeatureType) 
        throws IOException {

        // geometryless, style it randomly
        GeometryDescriptor gd = nativeFeatureType.getGeometryDescriptor();
        if (gd == null)
            return catalog.getStyleByName(StyleInfo.DEFAULT_POINT);

        Class gtype = gd.getType().getBinding();
        StyleType st;
        if (LineString.class.isAssignableFrom(gtype)
                || MultiLineString.class.isAssignableFrom(gtype)) {
            st = StyleType.LINE;
        } else if (Polygon.class.isAssignableFrom(gtype)
                || MultiPolygon.class.isAssignableFrom(gtype)) {
            st = StyleType.POLYGON;
        } else if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
            st = StyleType.POINT;
        } else {
            st = StyleType.GENERIC;
        }

        return doCreateStyle(st, featureType);
    }

    public StyleInfo createStyle(CoverageInfo coverage) throws IOException {
        return doCreateStyle(StyleType.RASTER, coverage);
    }

    StyleInfo doCreateStyle(StyleType styleType, ResourceInfo resource) throws IOException {
        // find a new style name
        String styleName = workspace != null ?
            findUniqueStyleName(resource, workspace) : findUniqueStyleName(resource);

        // variable replacement
        String sld = loadSLDFromTemplate(styleType, ramp.next(), resource);

        // let's store it
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(styleName);
        style.setFilename(styleName + ".sld");
        if (workspace != null) {
            style.setWorkspace(workspace);
        }

        catalog.getResourcePool().writeStyle(style, new ByteArrayInputStream(sld.getBytes()));
        
        return style;
    }

    String findUniqueStyleName(ResourceInfo resource) {
        String styleName = resource.getStore().getWorkspace().getName() + "_" + resource.getName();
        StyleInfo style = catalog.getStyleByName(styleName);
        int i = 1;
        while(style != null) {
            styleName = resource.getStore().getWorkspace().getName() + "_" + resource.getName() + i;
            style = catalog.getStyleByName(styleName);
            i++;
        }
        return styleName;
    }

    String findUniqueStyleName(ResourceInfo resource, WorkspaceInfo workspace) {
        String styleName = resource.getName();
        StyleInfo style = catalog.getStyleByName(workspace, styleName);
        int i = 1;
        while(style != null) {
            styleName = resource.getName() + i;
            style = catalog.getStyleByName(workspace, styleName);
            i++;
        }
        return styleName;
    }

    String loadSLDFromTemplate(StyleType type, ColorRamp.Entry entry, ResourceInfo resource) {
        String colorCode = Integer.toHexString(entry.color.getRGB());
        colorCode = colorCode.substring(2, colorCode.length());
        return TEMPLATES.get(type).replace("${colorName}", entry.name).replace(
                "${colorCode}", "#" + colorCode).replace("${layerName}", resource.getName());

    }
    /**
     * A rolling color ramp with color names
     */
    static class ColorRamp {
        static class Entry {
            String name;
            Color color;

            Entry(String name, Color color) {
                this.name = name;
                this.color = color;
            }
        }

        List<Entry> entries = new ArrayList<Entry>();
        int position;
        
        /**
         * Builds an empty ramp. Mind, you need to call {@link #add(String, Color)} at least
         * once to make the ramp usable.
         */
        public ColorRamp() {
            
        }
        
        /**
         * Adds a name/color combination
         * @param name
         * @param color
         */
        public void add(String name, Color color) {
            entries.add(new Entry(name, color));
        }
        
        /**
         * Moves to the next color in the ramp
         */
        public Entry next() {
            position = (position+1) % entries.size();
            return entries.get(position);
        }

        /**
         * Sets the current ramp position to a random index
         */
        public void initRandom() {
            position = (int) (entries.size() * Math.random());
        }

    }
}
