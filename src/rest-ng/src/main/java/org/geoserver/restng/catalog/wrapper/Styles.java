package org.geoserver.restng.catalog.wrapper;

import java.util.List;

import org.geoserver.catalog.StyleInfo;
import org.geoserver.restng.converters.CatalogListConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * A wrapper for the styles response. In the previous rest API this wasn't needed because in
 * each individual rest request the Collections were aliased to
 */
@XStreamAlias("styles")
@XStreamConverter(CatalogListConverter.class)
public class Styles implements XStreamListWrapper {

    @XStreamImplicit(itemFieldName = "style")
    List<StyleInfo> styles;

    public Styles(List<StyleInfo> styles) {
        this.styles = styles;
    }
}
