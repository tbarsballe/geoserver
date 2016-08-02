package org.geoserver.wms.web.data;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class LayerAttributePanel extends StyleEditTabPanel {

    public LayerAttributePanel(String id, AbstractStylePage parent) {
        super(id, parent);
        
        //Change layer link
        PropertyModel<String> layerNameModel = new PropertyModel<String>(parent.getLayerModel(),"prefixedName");
        add(new SimpleAjaxLink<String>("change.layer", layerNameModel) {
            private static final long serialVersionUID = 7341058018479354596L;

            public void onClick(AjaxRequestTarget target) {
                ModalWindow popup = parent.getPopup();
                
                popup.setInitialHeight(400);
                popup.setInitialWidth(600);
                popup.setTitle(new Model<String>("Choose layer to edit"));
                popup.setContent(new LayerChooser(popup.getContentId(), parent));
                popup.show(target);
            }
        });
        
        
    }

}
