package org.geoserver.restng.catalog.wrapper;

import org.geoserver.rest.format.ReflectiveHTMLFormat;

/**
 * Created by tbarsballe on 2017-03-21.
 */
public class DefaultFreemarkerContextWrapper implements FreemarkerContextWrapper {

    Object object;
    Class objectClass;
    Class classForTemplateLoading;
    String templatePrefix;

    public DefaultFreemarkerContextWrapper(Object object) {
        this(object, object.getClass());
    }

    public DefaultFreemarkerContextWrapper(Object object, Class objectClass) {
        this(object, objectClass, ReflectiveHTMLFormat.class);
    }

    public DefaultFreemarkerContextWrapper(Object object, Class objectClass, Class classForTemplateLoading) {
        this(object, objectClass, classForTemplateLoading, "");
    }

    public DefaultFreemarkerContextWrapper(Object object, Class objectClass, Class classForTemplateLoading, String templatePrefix) {
        this.object = object;
        this.objectClass = objectClass;
        this.classForTemplateLoading = classForTemplateLoading;
        this.templatePrefix = templatePrefix;
    }

    @Override
    public Class getClassForTemplateLoading() {
        return classForTemplateLoading;
    }

    public void setClassForTemplateLoading(Class classForTemplateLoading) {
        this.classForTemplateLoading = classForTemplateLoading;
    }

    @Override
    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    @Override
    public Class getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(Class objectClass) {
        this.objectClass = objectClass;
    }

    @Override
    public String getTemplatePrefix() {
        return templatePrefix;
    }

    public void setTemplatePrefix(String templatePrefix) {
        this.templatePrefix = templatePrefix;
    }
}
