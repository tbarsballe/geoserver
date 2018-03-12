/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.gwc.wmts.Tuple;
import org.geoserver.wms.WMS;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Base class for vector based dimension
 */
public abstract class VectorDimension extends Dimension {

    private final Comparator comparator;

    public VectorDimension(WMS wms, String dimensionName, LayerInfo layerInfo, DimensionInfo dimensionInfo,
                           Comparator comparator) {
        super(wms, dimensionName, layerInfo, dimensionInfo);
        this.comparator = comparator;
    }

    /**
     * Helper method used to get domain values from a vector type in the form of a feature collection.
     */
    protected FeatureCollection getDomain(Filter filter) {
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) getResourceInfo();
        FeatureSource source;
        try {
            source = typeInfo.getFeatureSource(null, GeoTools.getDefaultHints());
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error getting feature source of vector '%s'.", resourceInfo.getName()), exception);
        }
        Query query = new Query(source.getSchema().getName().getLocalPart(), filter == null ? Filter.INCLUDE : filter);
        try {
            return source.getFeatures(query);
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error reading feature from layer '%s' for dimension '%s'.",
                    resourceInfo.getName(), getDimensionName()), exception);
        }
    }

    @Override
    public List<Object> getDomainValues(Filter filter, boolean noDuplicates) {
        FeatureCollection featureCollection = getDomain(filter);
        if (noDuplicates) {
            // no duplicate values should be included
            Set<Object> values = DimensionsUtils.
                    getValuesWithoutDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator);
            return new ArrayList<>(values);
        }
        // we need the duplicate values (this is useful for some operations like get histogram operation)
        return DimensionsUtils.getValuesWithDuplicates(dimensionInfo.getAttribute(), featureCollection, comparator);
    }

    protected DomainSummary getDomainSummary(Filter filter, boolean includeCount) {
        FeatureCollection features = getDomain(filter);
        String attribute = dimensionInfo.getAttribute();

        return getDomainSummary(features, attribute, includeCount);
    }

}
