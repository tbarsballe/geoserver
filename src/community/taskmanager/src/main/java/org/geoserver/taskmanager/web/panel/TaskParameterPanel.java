/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.web.model.ParametersModel;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class TaskParameterPanel extends Panel {

    public static final String CONFIGURATION_NAME = "configurationName";
    public static final String TASK_NAME = "taskName";

    private static final long serialVersionUID = 3902645494421966388L;

    private IModel<Task> taskModel;

    public TaskParameterPanel(String id, IModel<Task> taskModel) {
        super(id);
        this.taskModel = taskModel;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        //the parameters panel
        GeoServerTablePanel<Parameter> parametersPanel =
                new GeoServerTablePanel<Parameter>("parametersPanel",
                        new ParametersModel(taskModel), true) {

                    private static final long serialVersionUID = -8943273843044917552L;

                    @SuppressWarnings("unchecked")
                    @Override
                    protected Component getComponentForProperty(String id, IModel<Parameter> itemModel,
                                                                Property<Parameter> property) {
                        if (property.equals(ParametersModel.VALUE)) {
                            return new TextFieldPanel(id, (IModel<String>) property.getModel(itemModel));
                        }
                        return null;
                    }
                };
        parametersPanel.setFilterVisible(false);
        parametersPanel.setSelectable(false);
        parametersPanel.setPageable(false);
        parametersPanel.setSortable(false);
        parametersPanel.setOutputMarkupId(true);
        add(parametersPanel);
    }

}
