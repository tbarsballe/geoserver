/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.xml.sax.SAXParseException;

/**
 * Base page for creating/editing styles
 */
@SuppressWarnings("serial")
public abstract class AbstractStylePage extends GeoServerSecuredPage {

    protected Form<StyleInfo> styleForm;

    protected AjaxTabbedPanel<ITab> tabbedPanel;

    protected CodeMirrorEditor editor;

    protected CompoundPropertyModel<StyleInfo> styleModel;

    String rawStyle;

    public AbstractStylePage() {
    }

    public AbstractStylePage(StyleInfo style) {
        initUI(style);
    }

    protected void initUI(StyleInfo style) {
        
        /* init model */
        if (style == null) {
            styleModel = new CompoundPropertyModel<StyleInfo>(getCatalog().getFactory().createStyle());
        } else {
            styleModel = new CompoundPropertyModel<StyleInfo>(new StyleDetachableModel(style));
        }
        
        /* init main form */
        styleForm = new Form<StyleInfo>("styleForm", styleModel) {
            @Override
            protected void onSubmit() {
                super.onSubmit();
                onStyleFormSubmit();
                tabbedPanel.visitChildren(StyleEditTabPanel.class, (component, visit) -> {
                    if (component instanceof StyleEditTabPanel) {
                        ((StyleEditTabPanel) component).onStyleFormSubmit();
                    }
                });
            }
        };
        add(styleForm);
        
        /* init tabs */
        List<ITab> tabs = new ArrayList<ITab>();
        
        //Well known tabs
        tabs.add(new PanelCachingTab(new AbstractTab(new Model<String>("Generated SLD")) {
            private static final long serialVersionUID = 8555701231692660833L;

            public Panel getPanel(String id) {
                StyleAdminPanel panel = new StyleAdminPanel(id, styleModel, AbstractStylePage.this);
                return panel;
            }
        }));
        
        //Dynamic tabs
        List<StyleEditTabPanelInfo> tabPanels = getGeoServerApplication().getBeansOfType(StyleEditTabPanelInfo.class);
        
        // sort the tabs based on order
        Collections.sort(tabPanels, new Comparator<StyleEditTabPanelInfo>() {
            public int compare(StyleEditTabPanelInfo o1, StyleEditTabPanelInfo o2) {
                Integer order1 = o1.getOrder() >= 0 ? o1.getOrder() : Integer.MAX_VALUE;
                Integer order2 = o2.getOrder() >= 0 ? o2.getOrder() : Integer.MAX_VALUE;

                return order1.compareTo(order2);
            }
        });
        // instantiate tab panels and add to tabs list
        for (StyleEditTabPanelInfo tabPanelInfo : tabPanels) {
            String titleKey = tabPanelInfo.getTitleKey();
            IModel<String> titleModel = null;
            if (titleKey != null) {
                titleModel = new org.apache.wicket.model.ResourceModel(titleKey);
            } else {
                titleModel = new Model<String>(tabPanelInfo.getComponentClass().getSimpleName());
            }
            
            final Class<StyleEditTabPanel> panelClass = tabPanelInfo.getComponentClass();
            
            tabs.add(new AbstractTab(titleModel) {
                private static final long serialVersionUID = -6637277497986497791L;
                @Override
                public Panel getPanel(String panelId) {
                    StyleEditTabPanel tabPanel;
                    try {
                        tabPanel = panelClass.getConstructor(String.class, IModel.class)
                                .newInstance(panelId, styleModel);
                    } catch (Exception e) {
                        throw new WicketRuntimeException(e);
                    }
                    return tabPanel;
                }
            });
        }
        
        tabbedPanel = new AjaxTabbedPanel<ITab>("context", tabs);
        
        styleForm.add(tabbedPanel);
        
        /* init editor */
        styleForm.add(editor = new CodeMirrorEditor("styleEditor", styleHandler()
                .getCodeMirrorEditMode(), new PropertyModel<String>(this, "rawStyle")));
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        editor.setRequired(true);
        styleForm.add(editor);
        
        add(validateLink());
        Link<StylePage> cancelLink = new Link<StylePage>("cancel") {
            @Override
            public void onClick() {
                doReturn(StylePage.class);
            }
        };
        add(cancelLink);
    }
    
    StyleHandler styleHandler() {
        String format = styleModel.getObject().getFormat();
        return Styles.handler(format);
    }
    
    Component validateLink() {
        return new GeoServerAjaxFormLink("validate", styleForm) {
            
            @Override
            protected void onClick(AjaxRequestTarget target, Form<?> form) {
                editor.processInput();

                List<Exception> errors = validateSLD();
                
                if ( errors.isEmpty() ) {
                    form.info( "No validation errors.");
                } else {
                    for( Exception e : errors ) {
                        form.error( sldErrorWithLineNo(e) );
                    }    
                }        
            }
            
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getAjaxCallListeners().add(editor.getSaveDecorator());
            }
        };
    }
    
    

    private String sldErrorWithLineNo(Exception e) {
        if (e instanceof SAXParseException) {
            SAXParseException se = (SAXParseException) e;
            return "line " + se.getLineNumber() + ": " + e.getLocalizedMessage();
        }
        String message = e.getLocalizedMessage();
        if(message != null) {
            return message;
        } else {
            return new ParamResourceModel("genericError", this).getString();
        }
    }
    
    List<Exception> validateSLD() {
        try {
            final String sld = editor.getInput();
            ByteArrayInputStream input = new ByteArrayInputStream(sld.getBytes());
            List<Exception> validationErrors = styleHandler().validate(input, null, null);
            return validationErrors;
        } catch( Exception e ) {
            return Arrays.asList( e );
        }
    }

    Reader readFile(StyleInfo style) throws IOException {
        ResourcePool pool = getCatalog().getResourcePool();
        return pool.readStyle(style);
    }
    
    public void setRawStyle(Reader in) throws IOException {
        BufferedReader bin = null;
        if ( in instanceof BufferedReader ) {
            bin = (BufferedReader) in;
        }
        else {
            bin = new BufferedReader( in );
        }
        
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = bin.readLine()) != null ) {
            builder.append(line).append("\n");
        }

        this.rawStyle = builder.toString();
        editor.setModelObject(rawStyle);
        in.close();
    }

    /**
     * Subclasses must implement to define the submit behavior
     */
    protected abstract void onStyleFormSubmit();

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
    
    //Make sure child tabs can see this
    @Override
    protected boolean isAuthenticatedAsAdmin() {
        return super.isAuthenticatedAsAdmin();
    }
    @Override
    protected Catalog getCatalog() {
        return super.getCatalog();
    }
    
    @Override
    protected GeoServerApplication getGeoServerApplication() {
        return super.getGeoServerApplication();
    }
    
 
}
