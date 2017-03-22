package org.geoserver.restng.catalog.wrapper;

import java.util.Collection;

/**
 * A wrapper for all Collection type responses. In the previous rest API this wasn't needed because in
 * each individual rest request the Collections were aliased to
 */
public interface XStreamListWrapper<T> {
    /**
     * Get the class of the wrapped object (or class of the collection contents)
     *
     * @return
     */
    Class getObjectClass();

    Collection<T> getCollection();
}
