package org.geoserver.wms.web.data;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.catalog.StyleInfo;

public class OpenLayersPreviewPanel extends StyleEditTabPanel {

    public OpenLayersPreviewPanel(String id, CompoundPropertyModel<? extends StyleInfo> model,
            AbstractStylePage parent) {
        super(id, model, parent);
        // TODO Auto-generated constructor stub
    }

}
