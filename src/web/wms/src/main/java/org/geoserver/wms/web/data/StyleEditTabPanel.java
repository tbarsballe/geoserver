/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.IOException;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.StyleInfo;

/**
 * Extension point for panels which appear in separate tabs on the style edit page.
 * <p>
 * Subclasses <b>must</b> override the {@link #StyleEditTabPanel(String, IModel)} constructor
 * and <b>not</b> change its signature.
 * </p>
 * <p>
 * Instances of this class are described in a spring context with a {@link StyleEditTabPanelInfo}
 * bean.
 * </p>
 */
public class StyleEditTabPanel extends Panel {

    private static final long serialVersionUID = 8044055895040826418L;
    
    protected AbstractStylePage stylePage;

    /**
     * @param id The id given to the panel.
     * @param model The model for the panel which wraps a {@link LayerInfo} instance.
     */
    public StyleEditTabPanel(String id, CompoundPropertyModel<? extends StyleInfo> model, AbstractStylePage parent) {
        super(id, model);
        
        this.stylePage = parent;
    }

    /**
     * @return the style currently being edited by the panel.
     */
    @SuppressWarnings("unchecked")
    public StyleInfo getStyleInfo() {
        return ((CompoundPropertyModel<? extends StyleInfo>) getDefaultModel()).getObject();
    }

    /**
     * Called by {@link AbstractStylePage} when the style form is submitted.
     * <p>
     */
    public void onStyleFormSubmit() {
        //do nothing by default
    }

    public StyleEditTabPanel setInputEnabled(final boolean enabled) {
        visitChildren((component, visit) -> {
            component.setEnabled(enabled);
        });
        return this;
    }
}
